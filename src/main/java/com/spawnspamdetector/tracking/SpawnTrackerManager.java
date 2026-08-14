package com.spawnspamdetector.tracking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;

import com.spawnspamdetector.config.SpawnSpamDetectorConfig;


public final class SpawnTrackerManager {

    private static final int ALERT_DETAIL_LIMIT = 3;

    private static final Map<Long, TrackedMobSnapshot.RegionCounts> regionCounters = new HashMap<>();
    private static final Map<ResourceLocation, Integer> globalMobCounts = new HashMap<>();
    private static final Map<Integer, String> dimensionNames = new HashMap<>();
    private static final Map<String, Long> lastAlertTimes = new HashMap<>();

    private static int globalCount;

    private SpawnTrackerManager() {
    }

    public static void clearAll() {
        clearSnapshotState();
        lastAlertTimes.clear();
    }

    public static void applySnapshot(TrackedMobSnapshot snapshot) {
        if (!SpawnSpamDetectorConfig.isDetectionEnabled()) {
            clearAll();
            return;
        }

        clearSnapshotState();
        if (snapshot == null) return;

        globalCount = snapshot.getGlobalCount();
        globalMobCounts.putAll(snapshot.getGlobalMobCounts());
        regionCounters.putAll(snapshot.getRegionCounters());
        dimensionNames.putAll(snapshot.getDimensionNames());

        evaluateAllAlerts();
    }

    public static void applyDelta(TrackedMobDelta delta) {
        if (!SpawnSpamDetectorConfig.isDetectionEnabled()) {
            clearAll();
            return;
        }

        if (delta == null) return;

        globalCount = delta.getGlobalCount();

        for (ResourceLocation mobId : delta.getRemovedGlobalMobIds()) globalMobCounts.remove(mobId);

        globalMobCounts.putAll(delta.getGlobalMobCounts());

        for (Long regionKey : delta.getRemovedRegions()) {
            regionCounters.remove(regionKey);
        }

        regionCounters.putAll(delta.getRegionCounters());
        dimensionNames.putAll(delta.getDimensionNames());

        evaluateDeltaAlerts(delta);
    }

    /**
     * Displays the N most common mob types in the client-side tracking snapshot.
     * This only reads the most recently received server snapshot/delta, it does not
     * request data from the server or trigger a scan.
     *
     * @param player   The client-side player to send the top mob counts to.
     * @param topCount The number of entries to display.
     */
    public static void sendTopGlobalMobCounts(EntityPlayerSP player, int topCount) {
        if (player == null) return;

        int globalThreshold = SpawnSpamDetectorConfig.general.globalDetectionThreshold;
        int perTypeThreshold = SpawnSpamDetectorConfig.general.globalDetectionThresholdPerType;
        List<Map.Entry<ResourceLocation, Integer>> topMobs = getTopMobCounts(globalMobCounts, topCount);

        player.sendMessage(styleSummary(new TextComponentTranslation(
            "spawnspamdetector.command.top.header",
            globalCount,
            globalThreshold
        )));

        if (topMobs.isEmpty()) {
            player.sendMessage(styleDetail(new TextComponentTranslation("spawnspamdetector.command.top.empty")));
            return;
        }

        player.sendMessage(styleDetail(new TextComponentTranslation("spawnspamdetector.command.top.list")));
        for (int index = 0; index < topMobs.size(); index++) {
            Map.Entry<ResourceLocation, Integer> entry = topMobs.get(index);
            player.sendMessage(styleDetail(new TextComponentTranslation(
                "spawnspamdetector.command.top.entry",
                index + 1,
                entry.getValue(),
                perTypeThreshold,
                createMobNameComponent(entry.getKey())
            )));
        }
    }

    private static void clearSnapshotState() {
        regionCounters.clear();
        globalMobCounts.clear();
        dimensionNames.clear();
        globalCount = 0;
    }

    private static void evaluateAllAlerts() {
        for (Map.Entry<Long, TrackedMobSnapshot.RegionCounts> regionEntry : regionCounters.entrySet()) {
            checkRegionTotalAlert(regionEntry.getKey(), regionEntry.getValue());

            for (Map.Entry<ResourceLocation, Integer> mobEntry : regionEntry.getValue().getMobCounts().entrySet()) {
                checkRegionTypeAlert(regionEntry.getKey(), mobEntry.getKey(), mobEntry.getValue());
            }
        }

        checkGlobalTotalAlert();

        for (ResourceLocation mobId : globalMobCounts.keySet()) checkGlobalTypeAlert(mobId);
    }

    private static void evaluateDeltaAlerts(TrackedMobDelta delta) {
        checkGlobalTotalAlert();

        for (ResourceLocation mobId : delta.getGlobalMobCounts().keySet()) checkGlobalTypeAlert(mobId);

        for (Map.Entry<Long, TrackedMobSnapshot.RegionCounts> regionEntry : delta.getRegionCounters().entrySet()) {
            checkRegionTotalAlert(regionEntry.getKey(), regionEntry.getValue());

            for (Map.Entry<ResourceLocation, Integer> mobEntry : regionEntry.getValue().getMobCounts().entrySet()) {
                checkRegionTypeAlert(regionEntry.getKey(), mobEntry.getKey(), mobEntry.getValue());
            }
        }
    }

    private static void checkRegionTotalAlert(long regionKey, TrackedMobSnapshot.RegionCounts counters) {
        if (counters.getTotalCount() < SpawnSpamDetectorConfig.general.regionDetectionThreshold) return;
        if (!shouldSendAlert("region-total:" + regionKey)) return;

        sendRegionTotalAlert(regionKey, counters);
    }

    private static void checkRegionTypeAlert(long regionKey, ResourceLocation mobId, Integer mobCount) {
        if (mobCount == null) return;
        if (mobCount < SpawnSpamDetectorConfig.general.regionDetectionThresholdPerType) return;
        if (!shouldSendAlert("region-type:" + regionKey + ":" + mobId)) return;

        sendRegionTypeAlert(regionKey, mobId, mobCount);
    }

    private static void checkGlobalTotalAlert() {
        if (globalCount < SpawnSpamDetectorConfig.general.globalDetectionThreshold) return;
        if (!shouldSendAlert("global-total")) return;

        sendGlobalTotalAlert();
    }

    private static void checkGlobalTypeAlert(ResourceLocation mobId) {
        Integer mobCount = globalMobCounts.get(mobId);
        if (mobCount == null) return;
        if (mobCount < SpawnSpamDetectorConfig.general.globalDetectionThresholdPerType) return;
        if (!shouldSendAlert("global-type:" + mobId)) return;

        sendGlobalTypeAlert(mobId, mobCount);
    }

    private static boolean shouldSendAlert(String alertKey) {
        long now = System.currentTimeMillis();
        long cooldown = SpawnSpamDetectorConfig.getDetectionCooldownMillis();
        Long lastSent = lastAlertTimes.get(alertKey);

        if (lastSent != null && now - lastSent < cooldown) return false;

        lastAlertTimes.put(alertKey, now);
        return true;
    }

    private static void sendRegionTotalAlert(long regionKey, TrackedMobSnapshot.RegionCounts counters) {
        EntityPlayerSP player = getClientPlayer();
        if (player == null) return;

        List<ITextComponent> lines = new ArrayList<>();
        lines.add(styleSummary(new TextComponentTranslation(
            "spawnspamdetector.alert.region.total",
            counters.getTotalCount(),
            createRegionComponent(regionKey)
        )));
        lines.addAll(createTopMobLines(counters.getMobCounts()));

        sendToPlayer(player, lines);
    }

    private static void sendRegionTypeAlert(long regionKey, ResourceLocation mobId, int mobCount) {
        EntityPlayerSP player = getClientPlayer();
        if (player == null) return;

        String killCommand = createKillCommand(regionKey, mobId);
        List<ITextComponent> lines = new ArrayList<>();
        lines.add(styleSummary(new TextComponentTranslation(
            "spawnspamdetector.alert.region.type",
            mobCount,
            createMobNameComponent(mobId),
            createRegionComponent(regionKey)
        )));
        lines.add(createCommandComponent(killCommand));

        if (LocationKey.getDimension(regionKey) != player.dimension) {
            lines.add(styleDetail(new TextComponentTranslation(
                "spawnspamdetector.alert.commandDimensionHint",
                createDimensionComponent(LocationKey.getDimension(regionKey))
            )));
        }

        if (!player.canUseCommand(2, "kill")) {
            lines.add(styleDetail(new TextComponentTranslation("spawnspamdetector.alert.commandPermissionHint")));
        }

        sendToPlayer(player, lines);
    }

    private static void sendGlobalTotalAlert() {
        EntityPlayerSP player = getClientPlayer();
        if (player == null) return;

        List<ITextComponent> lines = new ArrayList<>();
        lines.add(styleSummary(new TextComponentTranslation(
            "spawnspamdetector.alert.global.total",
            globalCount
        )));
        lines.addAll(createTopRegionLines(getTopRegions(ALERT_DETAIL_LIMIT), null));

        sendToPlayer(player, lines);
    }

    private static void sendGlobalTypeAlert(ResourceLocation mobId, int mobCount) {
        EntityPlayerSP player = getClientPlayer();
        if (player == null) return;

        List<ITextComponent> lines = new ArrayList<>();
        lines.add(styleSummary(new TextComponentTranslation(
            "spawnspamdetector.alert.global.type",
            mobCount,
            createMobNameComponent(mobId)
        )));
        lines.addAll(createTopRegionLines(getTopRegionsForMob(mobId, ALERT_DETAIL_LIMIT), mobId));

        sendToPlayer(player, lines);
    }

    private static void sendToPlayer(EntityPlayerSP player, List<ITextComponent> lines) {
        for (ITextComponent line : lines) player.sendMessage(line.createCopy());
    }

    private static List<ITextComponent> createTopMobLines(Map<ResourceLocation, Integer> mobCounts) {
        List<Map.Entry<ResourceLocation, Integer>> topMobs = getTopMobCounts(mobCounts, ALERT_DETAIL_LIMIT);
        if (topMobs.isEmpty()) return Collections.emptyList();

        List<ITextComponent> lines = new ArrayList<>();
        lines.add(styleDetail(new TextComponentTranslation("spawnspamdetector.alert.topMobs")));

        for (int index = 0; index < topMobs.size(); index++) {
            Map.Entry<ResourceLocation, Integer> entry = topMobs.get(index);
            lines.add(styleDetail(new TextComponentTranslation(
                "spawnspamdetector.alert.entry.mobCount",
                entry.getValue(),
                createMobNameComponent(entry.getKey())
            )));
        }

        return lines;
    }

    private static List<ITextComponent> createTopRegionLines(List<RegionCount> topRegions, ResourceLocation mobId) {
        if (topRegions.isEmpty()) return Collections.emptyList();

        List<ITextComponent> lines = new ArrayList<>();
        lines.add(styleDetail(new TextComponentTranslation("spawnspamdetector.alert.topRegions")));

        for (RegionCount regionCount : topRegions) {
            if (mobId == null) {
                lines.add(styleDetail(new TextComponentTranslation(
                    "spawnspamdetector.alert.entry.regionTotal",
                    regionCount.count,
                    createRegionComponent(regionCount.regionKey)
                )));
                continue;
            }

            lines.add(styleDetail(new TextComponentTranslation(
                "spawnspamdetector.alert.entry.regionType",
                regionCount.count,
                createMobNameComponent(mobId),
                createRegionComponent(regionCount.regionKey)
            )));
        }

        return lines;
    }

    static List<Map.Entry<ResourceLocation, Integer>> getTopMobCounts(
            Map<ResourceLocation, Integer> mobCounts,
            int limit) {
        if (mobCounts.isEmpty() || limit <= 0) return Collections.emptyList();

        List<Map.Entry<ResourceLocation, Integer>> topMobs = new ArrayList<>(Math.min(limit, mobCounts.size()));

        for (Map.Entry<ResourceLocation, Integer> entry : mobCounts.entrySet()) {
            int index = 0;
            while (index < topMobs.size() && !appearsBefore(entry, topMobs.get(index))) index++;
            if (index >= limit) continue;

            topMobs.add(index, entry);
            if (topMobs.size() > limit) topMobs.remove(topMobs.size() - 1);
        }

        return topMobs;
    }

    private static boolean appearsBefore(
            Map.Entry<ResourceLocation, Integer> candidate,
            Map.Entry<ResourceLocation, Integer> existing) {
        int countComparison = Integer.compare(candidate.getValue(), existing.getValue());
        if (countComparison != 0) return countComparison > 0;

        return candidate.getKey().toString().compareTo(existing.getKey().toString()) < 0;
    }

    private static List<RegionCount> getTopRegions(int limit) {
        List<RegionCount> topRegions = new ArrayList<>(Math.min(limit, regionCounters.size()));

        for (Map.Entry<Long, TrackedMobSnapshot.RegionCounts> entry : regionCounters.entrySet()) {
            addTopRegion(topRegions, entry.getKey(), entry.getValue().getTotalCount(), limit);
        }

        return topRegions;
    }

    private static List<RegionCount> getTopRegionsForMob(ResourceLocation mobId, int limit) {
        List<RegionCount> topRegions = new ArrayList<>(Math.min(limit, regionCounters.size()));

        for (Map.Entry<Long, TrackedMobSnapshot.RegionCounts> entry : regionCounters.entrySet()) {
            Integer mobCount = entry.getValue().getMobCounts().get(mobId);
            if (mobCount == null || mobCount <= 0) continue;

            addTopRegion(topRegions, entry.getKey(), mobCount, limit);
        }

        return topRegions;
    }

    private static void addTopRegion(List<RegionCount> topRegions, long regionKey, int count, int limit) {
        int index = 0;
        while (index < topRegions.size() && topRegions.get(index).count >= count) index++;
        if (index >= limit) return;

        topRegions.add(index, new RegionCount(regionKey, count));
        if (topRegions.size() > limit) topRegions.remove(topRegions.size() - 1);
    }

    private static ITextComponent createMobNameComponent(ResourceLocation mobId) {
        ITextComponent mobName = createResolvedMobNameComponent(mobId);
        if (mobId.toString().equals(mobName.getUnformattedText())) return mobName;

        return new TextComponentTranslation("spawnspamdetector.entity.nameWithId", mobName, mobId.toString());
    }

    private static ITextComponent createResolvedMobNameComponent(ResourceLocation mobId) {
        EntityPlayerSP player = getClientPlayer();
        if (player != null && player.world != null) {
            // Resolve the same client-visible entity name players already see elsewhere in-game.
            try {
                Entity entity = EntityList.createEntityByIDFromName(mobId, player.world);
                if (entity != null) return new TextComponentString(entity.getDisplayName().getUnformattedText());
            } catch (Exception e) {
                // Ignore exceptions from entity creation, fallback to translation key below.
            }
        }

        String translationKey = EntityList.getTranslationName(mobId);
        if (translationKey == null || translationKey.isEmpty()) {
            return new TextComponentString(mobId.toString());
        }

        return new TextComponentTranslation(translationKey);
    }

    private static ITextComponent createRegionComponent(long regionKey) {
        return new TextComponentTranslation(
            "spawnspamdetector.alert.region.label",
            createDimensionComponent(LocationKey.getDimension(regionKey)),
            LocationKey.getRegionX(regionKey),
            LocationKey.getRegionZ(regionKey),
            LocationKey.getRegionMinX(regionKey),
            LocationKey.getRegionMaxX(regionKey),
            LocationKey.getRegionMinZ(regionKey),
            LocationKey.getRegionMaxZ(regionKey)
        );
    }

    private static ITextComponent createDimensionComponent(int dimensionId) {
        String dimensionName = dimensionNames.get(dimensionId);
        if (dimensionName == null || dimensionName.isEmpty()) {
            return new TextComponentTranslation("spawnspamdetector.alert.dimension.idOnly", dimensionId);
        }

        return new TextComponentTranslation("spawnspamdetector.alert.dimension.label", dimensionName, dimensionId);
    }

    private static ITextComponent createCommandComponent(String command) {
        TextComponentTranslation component = new TextComponentTranslation("spawnspamdetector.alert.command");
        Style style = component.getStyle();
        style.setColor(TextFormatting.RED);
        style.setBold(true);
        style.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command));
        style.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString(command)));
        return component;
    }

    private static ITextComponent styleSummary(ITextComponent component) {
        component.getStyle().setColor(TextFormatting.GOLD);
        return component;
    }

    private static ITextComponent styleDetail(ITextComponent component) {
        component.getStyle().setColor(TextFormatting.GRAY);
        return component;
    }

    private static String createKillCommand(long regionKey, ResourceLocation mobId) {
        return String.format(
            "/kill @e[type=%s,x=%d,y=0,z=%d,dx=%d,dy=255,dz=%d]",
            mobId,
            LocationKey.getRegionMinX(regionKey),
            LocationKey.getRegionMinZ(regionKey),
            LocationKey.REGION_SIZE - 1,
            LocationKey.REGION_SIZE - 1
        );
    }

    private static EntityPlayerSP getClientPlayer() {
        return Minecraft.getMinecraft().player;
    }

    private static final class RegionCount {

        private final long regionKey;
        private final int count;

        private RegionCount(long regionKey, int count) {
            this.regionKey = regionKey;
            this.count = count;
        }
    }
}
