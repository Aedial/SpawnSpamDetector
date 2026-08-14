package com.spawnspamdetector.tracking;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.WorldServer;

import com.spawnspamdetector.SpawnSpamDetector;
import com.spawnspamdetector.config.SpawnSpamDetectorConfig;
import com.spawnspamdetector.network.PacketTrackingDelta;
import com.spawnspamdetector.network.PacketTrackingSnapshot;
import com.spawnspamdetector.network.SpawnSpamDetectorNetwork;


/**
 * Server-authoritative, scan-driven mob counter.
 *
 * <p>
 * The manager has two layers of state:
 * <ol>
 *   <li>{@code aggregateStates} stores the most recent scanned counts for one server-side filter.</li>
 *   <li>{@code clientStates} records which aggregate each opted-in player receives.</li>
 * </ol>
 *
 * <p>
 * A client only sends its opt-in state. Filters are server-owned, so clients sharing the
 * same filter share one aggregate and one set of counts. While at least one client is
 * subscribed, the manager scans every loaded world once per minute, groups living mobs by
 * their current region, and forwards only the changed regions and mob totals to clients.
 *
 * <p>
 * All methods run on the server thread: packet settings are scheduled onto it by
 * {@code PacketClientTrackingSettings}, and lifecycle methods are invoked by
 * {@link ServerTrackingEventHandler}.
 */
public final class ServerTrackingManager {

    /** Ticks between full tracking scans while at least one client is subscribed. */
    private static final int TRACKING_SCAN_INTERVAL_TICKS = 20 * 60;

    /** Subscription and packet state per opted-in player UUID. */
    private static final Map<UUID, ClientTrackingState> clientStates = new HashMap<>();
    /** Shared count state per server filter, potentially with several subscribers. */
    private static final Map<TrackedMobFilter.FilterKey, AggregateTrackingState> aggregateStates = new LinkedHashMap<>();
    private static MinecraftServer activeServer;
    private static long serverTickCounter;
    private static long nextScanTick;

    private ServerTrackingManager() {
    }

    /**
     * Applies the opt-in packet received from one player.
     *
     * <p>
     * Enabling attaches the player to the current server filter. Tracking data is refreshed by
     * periodic server scans, and any newly subscribed client receives a full snapshot after the
     * next completed scan of its aggregate.
     */
    public static void updateClientSettings(EntityPlayerMP player, boolean detectionEnabled) {
        if (player == null) return;

        UUID playerId = player.getUniqueID();
        MinecraftServer server = player.getServer();
        SpawnSpamDetector.LOGGER.info(
            "Received tracking settings from {}. Detection enabled: {}",
            describePlayer(player),
            detectionEnabled
        );

        synchronizeActiveServer(server);
        synchronizeServerFilter(server);

        if (!detectionEnabled) {
            removeClient(server, playerId);
            return;
        }

        TrackedMobFilter.FilterKey filterKey = SpawnSpamDetectorConfig.getTrackingFilterKey();
        ClientTrackingState trackingState = clientStates.computeIfAbsent(playerId, ClientTrackingState::new);

        if (trackingState.hasFilter(filterKey)) {
            SpawnSpamDetector.LOGGER.info(
                "Tracking subscription already active for {} with {}; scheduling full snapshot",
                describePlayer(player),
                describeFilter(filterKey)
            );

            if (trackingState.getAggregateState() != null && !trackingState.getAggregateState().isInitialized()) {
                scheduleImmediateScan();
            }

            trackingState.scheduleFullSync();
            return;
        }

        boolean aggregateSetChanged = subscribeClient(playerId, trackingState, filterKey);
        if (aggregateSetChanged) {
            scheduleImmediateScan();
            scheduleFullSyncForAllClients();
        }

        trackingState.scheduleFullSync();
    }

    /**
     * Removes a player subscription on logout or opt-out.
     *
     * <p>
     * Counts remain live while another subscriber uses the same aggregate. Once the final
     * subscription disappears, all tracking state is discarded so the next first subscriber
     * starts from a fresh scan.
     */
    public static void removeClient(MinecraftServer server, UUID playerId) {
        if (playerId == null) return;

        synchronizeActiveServer(server);

        ClientTrackingState trackingState = clientStates.remove(playerId);
        if (trackingState == null) {
            SpawnSpamDetector.LOGGER.info("Tracking unsubscribe ignored for {} because no client state exists", playerId);
            return;
        }

        unsubscribeClient(playerId, trackingState);
        clearTrackingStateIfUnused();
    }

    /**
     * Performs the low-frequency work: disconnected-client cleanup, optional filter migration,
     * tracking scans, queued full snapshots, and delta delivery.
     */
    public static void onServerTick(MinecraftServer server) {
        if (server == null) return;

        synchronizeActiveServer(server);
        serverTickCounter++;
        if (clientStates.isEmpty()) return;

        cleanupDisconnectedClients(server);
        if (clientStates.isEmpty()) return;

        synchronizeServerFilter(server);

        if (shouldRunTrackingScan()) scanTrackedEntities(server);

        syncPendingSnapshots(server);
        syncDirtyAggregates(server);
    }

    /**
     * Releases state for a stopped server. This is particularly important for
     * integrated servers, where static state otherwise outlives a world.
     */
    public static void resetServerState() {
        clientStates.clear();
        aggregateStates.clear();
        activeServer = null;
        serverTickCounter = 0L;
        nextScanTick = 0L;
    }

    /**
     * Rebuilds each aggregate from the entities currently loaded on the server, then computes
     * the delta against the previous scan.
     */
    private static void scanTrackedEntities(MinecraftServer server) {
        if (server == null || aggregateStates.isEmpty()) return;

        Map<AggregateTrackingState, AggregateScanState> scanStates = new LinkedHashMap<>();
        for (AggregateTrackingState aggregateState : aggregateStates.values()) {
            scanStates.put(aggregateState, new AggregateScanState());
        }

        for (WorldServer world : server.worlds) {
            if (world == null) continue;

            for (Entity entity : world.loadedEntityList) {
                ResourceLocation mobId = resolveTrackedMobId(entity);
                if (mobId == null) continue;

                long regionKey = LocationKey.fromEntity(entity);
                for (Map.Entry<AggregateTrackingState, AggregateScanState> entry : scanStates.entrySet()) {
                    if (!entry.getKey().matches(mobId)) continue;

                    entry.getValue().incrementCounts(mobId, regionKey);
                }
            }
        }

        for (Map.Entry<AggregateTrackingState, AggregateScanState> entry : scanStates.entrySet()) {
            entry.getKey().replaceCounts(entry.getValue());
        }

        nextScanTick = serverTickCounter + TRACKING_SCAN_INTERVAL_TICKS;
    }

    /**
     * Frees entity and aggregate state after the final client has unsubscribed.
     */
    private static void clearTrackingStateIfUnused() {
        if (!clientStates.isEmpty()) return;

        aggregateStates.clear();
        nextScanTick = serverTickCounter;
    }

    /**
     * Clears static state if an integrated server is replaced before its stop event is observed.
     */
    private static void synchronizeActiveServer(MinecraftServer server) {
        if (server == null || server == activeServer) return;

        resetServerState();
        activeServer = server;
    }

    /**
     * Backstop for logout handling. This catches clients that disappear without a usable logout
     * event, while the normal {@link #removeClient(MinecraftServer, UUID)} path is immediate.
     */
    private static void cleanupDisconnectedClients(MinecraftServer server) {
        Iterator<Map.Entry<UUID, ClientTrackingState>> iterator = clientStates.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, ClientTrackingState> entry = iterator.next();
            EntityPlayerMP player = server.getPlayerList().getPlayerByUUID(entry.getKey());
            if (player != null) continue;

            SpawnSpamDetector.LOGGER.info("Cleaning up tracking state for disconnected player {}", entry.getKey());

            unsubscribeClient(entry.getKey(), entry.getValue());

            iterator.remove();
        }

        clearTrackingStateIfUnused();
    }

    /**
     * Moves all subscribers to the current server filter after a server-side configuration
     * change. A filter replacement invalidates existing counts, so it triggers a rescan and
     * resnapshot for every subscriber.
     */
    private static void synchronizeServerFilter(MinecraftServer server) {
        if (server == null) return;
        if (clientStates.isEmpty() || aggregateStates.isEmpty()) return;

        TrackedMobFilter.FilterKey filterKey = SpawnSpamDetectorConfig.getTrackingFilterKey();
        if (aggregateStates.size() == 1 && aggregateStates.containsKey(filterKey)) return;

        SpawnSpamDetector.LOGGER.info(
            "Server tracking filter changed; rebuilding {} subscribed client(s) to {}",
            clientStates.size(),
            describeFilter(filterKey)
        );

        AggregateTrackingState aggregateState = new AggregateTrackingState(filterKey);
        aggregateStates.clear();
        aggregateStates.put(filterKey, aggregateState);

        for (Map.Entry<UUID, ClientTrackingState> entry : clientStates.entrySet()) {
            aggregateState.addSubscriber(entry.getKey());
            entry.getValue().setAggregate(filterKey, aggregateState);
        }

        scheduleImmediateScan();
        scheduleFullSyncForAllClients();
    }

    /**
     * Sends complete state to clients that have just subscribed or need resynchronization.
     * One packet instance is reused for every client sharing the same aggregate in this tick.
     */
    private static void syncPendingSnapshots(MinecraftServer server) {
        Map<AggregateTrackingState, PacketTrackingSnapshot> snapshotPackets = new HashMap<>();

        for (Map.Entry<UUID, ClientTrackingState> entry : clientStates.entrySet()) {
            ClientTrackingState trackingState = entry.getValue();
            if (!trackingState.isFullSyncPending()) continue;

            EntityPlayerMP player = server.getPlayerList().getPlayerByUUID(entry.getKey());
            if (player == null) continue;

            AggregateTrackingState aggregateState = trackingState.getAggregateState();
            if (aggregateState == null) {
                trackingState.markSynced(serverTickCounter);
                continue;
            }

            if (!aggregateState.isInitialized()) continue;

            PacketTrackingSnapshot packet = snapshotPackets.computeIfAbsent(
                aggregateState,
                ignored -> new PacketTrackingSnapshot(aggregateState.createSnapshot(server))
            );

            SpawnSpamDetectorNetwork.INSTANCE.sendTo(packet, player);
            trackingState.markSynced(serverTickCounter);
        }
    }

    /**
     * Sends changed regions/types after the latest scan. Clients that already received a full
     * snapshot this tick are skipped because that snapshot is newer than the delta.
     */
    private static void syncDirtyAggregates(MinecraftServer server) {
        for (AggregateTrackingState aggregateState : aggregateStates.values()) {
            if (!aggregateState.isDirty()) continue;

            PacketTrackingDelta packet = new PacketTrackingDelta(aggregateState.createDelta(server));

            for (UUID playerId : aggregateState.getSubscriberIds()) {
                ClientTrackingState trackingState = clientStates.get(playerId);
                if (trackingState == null || trackingState.wasSynced(serverTickCounter)) continue;

                EntityPlayerMP player = server.getPlayerList().getPlayerByUUID(playerId);
                if (player == null) continue;

                SpawnSpamDetectorNetwork.INSTANCE.sendTo(packet, player);
                trackingState.markSynced(serverTickCounter);
            }

            aggregateState.markSynced();
        }
    }

    /**
     * Ensures the next server tick performs a full entity scan before sending snapshots.
     */
    private static void scheduleImmediateScan() {
        nextScanTick = serverTickCounter;
    }

    private static boolean shouldRunTrackingScan() {
        return !aggregateStates.isEmpty() && serverTickCounter >= nextScanTick;
    }

    private static ResourceLocation resolveTrackedMobId(Entity entity) {
        if (!(entity instanceof EntityLiving)) return null;

        return EntityList.getKey(entity);
    }

    /**
     * Attaches a client to a shared aggregate.
     *
     * @return {@code true} when creating/removing an aggregate changed the set that must be
     * rebuilt; adding another subscriber to an existing aggregate returns {@code false}.
     */
    private static boolean subscribeClient(UUID playerId, ClientTrackingState trackingState, TrackedMobFilter.FilterKey filterKey) {
        AggregateTrackingState previousAggregate = trackingState.getAggregateState();
        if (previousAggregate != null && filterKey.equals(trackingState.getFilterKey())) return false;

        AggregateTrackingState nextAggregate = aggregateStates.get(filterKey);
        boolean createdAggregate = false;
        if (nextAggregate == null) {
            nextAggregate = new AggregateTrackingState(filterKey);
            aggregateStates.put(filterKey, nextAggregate);
            createdAggregate = true;
        }

        boolean removedAggregate = unsubscribeClient(playerId, trackingState);
        nextAggregate.addSubscriber(playerId);
        trackingState.setAggregate(filterKey, nextAggregate);
        return createdAggregate || removedAggregate;
    }

    /**
     * Detaches a client from its aggregate.
     *
     * @return {@code true} only when this was the final subscriber and the aggregate was removed.
     */
    private static boolean unsubscribeClient(UUID playerId, ClientTrackingState trackingState) {
        AggregateTrackingState aggregateState = trackingState.getAggregateState();
        TrackedMobFilter.FilterKey filterKey = trackingState.getFilterKey();
        trackingState.clearAggregate();

        if (aggregateState == null || filterKey == null) return false;

        aggregateState.removeSubscriber(playerId);

        SpawnSpamDetector.LOGGER.info(
            "Unsubscribed player {} from {}. Remaining subscribers: {}",
            playerId,
            describeFilter(filterKey),
            aggregateState.subscriberIds.size()
        );

        if (!aggregateState.isUnused()) return false;

        aggregateStates.remove(filterKey);
        SpawnSpamDetector.LOGGER.info("Removed unused tracking for {}", describeFilter(filterKey));
        return true;
    }

    private static void scheduleFullSyncForAllClients() {
        if (clientStates.isEmpty()) return;

        SpawnSpamDetector.LOGGER.info("Scheduling full tracking snapshots for {} subscribed client(s)", clientStates.size());

        for (ClientTrackingState trackingState : clientStates.values()) {
            trackingState.scheduleFullSync();
        }
    }

    private static String describePlayer(EntityPlayerMP player) {
        if (player == null) return "unknown-player";

        return player.getName();
    }

    private static String describeFilter(TrackedMobFilter.FilterKey filterKey) {
        if (filterKey == null) return "no-filter";

        return describeFilter(filterKey.createFilter());
    }

    private static String describeFilter(TrackedMobFilter filter) {
        if (filter == null) return "no-filter";

        Set<ResourceLocation> whitelistedMobIds = filter.getWhitelistedMobIds();
        if (!whitelistedMobIds.isEmpty()) {
            return "whitelist(" + whitelistedMobIds.size() + ")=" + whitelistedMobIds;
        }

        Set<ResourceLocation> blacklistedMobIds = filter.getBlacklistedMobIds();
        if (!blacklistedMobIds.isEmpty()) {
            return "blacklist(" + blacklistedMobIds.size() + ")=" + blacklistedMobIds;
        }

        return "all-mobs";
    }

    /**
     * Per-player subscription and same-tick packet suppression state.
     */
    private static final class ClientTrackingState {

        private TrackedMobFilter.FilterKey filterKey;
        private AggregateTrackingState aggregateState;
        private boolean fullSyncPending;
        private long lastSyncTick = -1L;

        private ClientTrackingState(UUID playerId) {
        }

        private boolean hasFilter(TrackedMobFilter.FilterKey filterKey) {
            return filterKey != null && filterKey.equals(this.filterKey);
        }

        private TrackedMobFilter.FilterKey getFilterKey() {
            return filterKey;
        }

        private AggregateTrackingState getAggregateState() {
            return aggregateState;
        }

        private void setAggregate(TrackedMobFilter.FilterKey filterKey, AggregateTrackingState aggregateState) {
            this.filterKey = filterKey;
            this.aggregateState = aggregateState;
        }

        private void clearAggregate() {
            filterKey = null;
            aggregateState = null;
            fullSyncPending = false;
        }

        private void scheduleFullSync() {
            fullSyncPending = true;
        }

        private boolean isFullSyncPending() {
            return fullSyncPending;
        }

        private void markSynced(long currentTick) {
            fullSyncPending = false;
            lastSyncTick = currentTick;
        }

        private boolean wasSynced(long currentTick) {
            return lastSyncTick == currentTick;
        }
    }

    /**
     * Shared counters for one {@link TrackedMobFilter}. Periodic scans rebuild this state from
     * scratch, then its dirty sets describe the smallest delta that clients need to receive.
     */
    private static final class AggregateTrackingState {

        private final Map<Long, MutableRegionCounts> regionCounters = new HashMap<>();
        private final Map<ResourceLocation, Integer> globalMobCounts = new HashMap<>();
        private final Set<UUID> subscriberIds = new LinkedHashSet<>();
        /** Changed entries carry their current count; removed entries are encoded separately. */
        private final Set<Long> dirtyRegionKeys = new LinkedHashSet<>();
        private final Set<Long> removedRegionKeys = new LinkedHashSet<>();
        private final Set<ResourceLocation> dirtyGlobalMobIds = new LinkedHashSet<>();
        private final Set<ResourceLocation> removedGlobalMobIds = new LinkedHashSet<>();

        private final TrackedMobFilter filter;
        private int globalCount;
        private boolean dirty;
        private boolean initialized;

        private AggregateTrackingState(TrackedMobFilter.FilterKey filterKey) {
            filter = filterKey.createFilter();
        }

        private void addSubscriber(UUID playerId) {
            subscriberIds.add(playerId);
        }

        private void removeSubscriber(UUID playerId) {
            subscriberIds.remove(playerId);
        }

        private boolean isUnused() {
            return subscriberIds.isEmpty();
        }

        private Set<UUID> getSubscriberIds() {
            return subscriberIds;
        }

        private boolean matches(ResourceLocation mobId) {
            return filter.matches(mobId);
        }

        /**
         * Replaces the aggregate with a fresh scan result and derives the smallest packet delta
         * needed to move clients from the previous scan to the new one.
         */
        private void replaceCounts(AggregateScanState scanState) {
            dirtyRegionKeys.clear();
            removedRegionKeys.clear();
            dirtyGlobalMobIds.clear();
            removedGlobalMobIds.clear();

            if (!initialized) {
                copyCountsFrom(scanState);
                initialized = true;
                dirty = false;
                return;
            }

            dirty = false;

            for (Map.Entry<Long, MutableRegionCounts> entry : regionCounters.entrySet()) {
                MutableRegionCounts nextRegionCounts = scanState.regionCounters.get(entry.getKey());
                if (nextRegionCounts == null) {
                    markRegionRemoved(entry.getKey());
                    dirty = true;
                    continue;
                }

                if (!entry.getValue().sameCounts(nextRegionCounts)) {
                    markRegionChanged(entry.getKey());
                    dirty = true;
                }
            }

            for (Long regionKey : scanState.regionCounters.keySet()) {
                if (regionCounters.containsKey(regionKey)) continue;

                markRegionChanged(regionKey);
                dirty = true;
            }

            for (Map.Entry<ResourceLocation, Integer> entry : globalMobCounts.entrySet()) {
                Integer nextCount = scanState.globalMobCounts.get(entry.getKey());
                if (nextCount == null) {
                    markGlobalMobRemoved(entry.getKey());
                    dirty = true;
                    continue;
                }

                if (!entry.getValue().equals(nextCount)) {
                    markGlobalMobChanged(entry.getKey());
                    dirty = true;
                }
            }

            for (ResourceLocation mobId : scanState.globalMobCounts.keySet()) {
                if (globalMobCounts.containsKey(mobId)) continue;

                markGlobalMobChanged(mobId);
                dirty = true;
            }

            copyCountsFrom(scanState);
        }

        private boolean isInitialized() {
            return initialized;
        }

        private boolean isDirty() {
            return dirty;
        }

        /** Clears the pending delta after all subscribers have received it. */
        private void markSynced() {
            dirty = false;
            dirtyRegionKeys.clear();
            removedRegionKeys.clear();
            dirtyGlobalMobIds.clear();
            removedGlobalMobIds.clear();
        }

        private void copyCountsFrom(AggregateScanState scanState) {
            regionCounters.clear();
            for (Map.Entry<Long, MutableRegionCounts> entry : scanState.regionCounters.entrySet()) {
                regionCounters.put(entry.getKey(), entry.getValue().copy());
            }

            globalMobCounts.clear();
            globalMobCounts.putAll(scanState.globalMobCounts);
            globalCount = scanState.globalCount;
        }

        /**
         * Creates a complete immutable baseline for a newly synchronized client.
         */
        private TrackedMobSnapshot createSnapshot(MinecraftServer server) {
            Map<ResourceLocation, Integer> snapshotGlobalMobCounts = new HashMap<>(globalMobCounts);
            Map<Long, TrackedMobSnapshot.RegionCounts> snapshotRegionCounters = new HashMap<>();

            for (Map.Entry<Long, MutableRegionCounts> entry : regionCounters.entrySet()) {
                snapshotRegionCounters.put(
                    entry.getKey(),
                    new TrackedMobSnapshot.RegionCounts(entry.getValue().totalCount, entry.getValue().mobCounts)
                );
            }

            Map<Integer, String> dimensionNames = new HashMap<>();
            for (Long regionKey : regionCounters.keySet()) {
                int dimensionId = LocationKey.getDimension(regionKey);
                dimensionNames.putIfAbsent(dimensionId, resolveDimensionName(server, dimensionId));
            }

            return new TrackedMobSnapshot(globalCount, snapshotGlobalMobCounts, snapshotRegionCounters, dimensionNames);
        }

        /**
         * Creates the compact changed/removed subset accumulated since the last sync.
         */
        private TrackedMobDelta createDelta(MinecraftServer server) {
            Map<ResourceLocation, Integer> deltaGlobalMobCounts = new HashMap<>();
            for (ResourceLocation mobId : dirtyGlobalMobIds) {
                Integer count = globalMobCounts.get(mobId);
                if (count == null) continue;

                deltaGlobalMobCounts.put(mobId, count);
            }

            Map<Long, TrackedMobSnapshot.RegionCounts> deltaRegionCounters = new HashMap<>();
            Map<Integer, String> dimensionNames = new HashMap<>();

            for (Long regionKey : dirtyRegionKeys) {
                MutableRegionCounts regionCounts = regionCounters.get(regionKey);
                if (regionCounts == null) continue;

                deltaRegionCounters.put(regionKey, new TrackedMobSnapshot.RegionCounts(regionCounts.totalCount, regionCounts.mobCounts));

                int dimensionId = LocationKey.getDimension(regionKey);
                dimensionNames.putIfAbsent(dimensionId, resolveDimensionName(server, dimensionId));
            }

            return new TrackedMobDelta(
                globalCount,
                deltaGlobalMobCounts,
                removedGlobalMobIds,
                deltaRegionCounters,
                removedRegionKeys,
                dimensionNames
            );
        }

        private void markRegionChanged(long regionKey) {
            dirtyRegionKeys.add(regionKey);
            removedRegionKeys.remove(regionKey);
        }

        private void markRegionRemoved(long regionKey) {
            removedRegionKeys.add(regionKey);
            dirtyRegionKeys.remove(regionKey);
        }

        private void markGlobalMobChanged(ResourceLocation mobId) {
            dirtyGlobalMobIds.add(mobId);
            removedGlobalMobIds.remove(mobId);
        }

        private void markGlobalMobRemoved(ResourceLocation mobId) {
            removedGlobalMobIds.add(mobId);
            dirtyGlobalMobIds.remove(mobId);
        }

        private String resolveDimensionName(MinecraftServer server, int dimensionId) {
            WorldServer world = server.getWorld(dimensionId);
            if (world != null) return world.provider.getDimensionType().getName();

            return "";
        }
    }

    private static final class AggregateScanState {

        private final Map<Long, MutableRegionCounts> regionCounters = new HashMap<>();
        private final Map<ResourceLocation, Integer> globalMobCounts = new HashMap<>();
        private int globalCount;

        private void incrementCounts(ResourceLocation mobId, long regionKey) {
            MutableRegionCounts regionCount = regionCounters.computeIfAbsent(regionKey, ignored -> new MutableRegionCounts());
            regionCount.totalCount++;
            regionCount.mobCounts.merge(mobId, 1, Integer::sum);

            globalCount++;
            globalMobCounts.merge(mobId, 1, Integer::sum);
        }
    }

    private static final class MutableRegionCounts {

        private int totalCount;
        private final Map<ResourceLocation, Integer> mobCounts = new HashMap<>();

        private MutableRegionCounts copy() {
            MutableRegionCounts copy = new MutableRegionCounts();
            copy.totalCount = totalCount;
            copy.mobCounts.putAll(mobCounts);
            return copy;
        }

        private boolean sameCounts(MutableRegionCounts other) {
            if (other == null) return false;
            if (totalCount != other.totalCount) return false;

            return mobCounts.equals(other.mobCounts);
        }
    }
}
