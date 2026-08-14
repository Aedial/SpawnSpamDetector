package com.spawnspamdetector.network;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import io.netty.buffer.ByteBuf;

import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import com.spawnspamdetector.tracking.SpawnTrackerManager;
import com.spawnspamdetector.tracking.TrackedMobDelta;
import com.spawnspamdetector.tracking.TrackedMobSnapshot;
import com.spawnspamdetector.util.MobIdUtils;


public class PacketTrackingDelta implements IMessage {

    private int globalCount;
    private Map<ResourceLocation, Integer> globalMobCounts = new HashMap<>();
    private Set<ResourceLocation> removedGlobalMobIds = new LinkedHashSet<>();
    private Map<Long, TrackedMobSnapshot.RegionCounts> regionCounters = new HashMap<>();
    private Set<Long> removedRegions = new LinkedHashSet<>();
    private Map<Integer, String> dimensionNames = new HashMap<>();

    public PacketTrackingDelta() {
    }

    public PacketTrackingDelta(TrackedMobDelta delta) {
        globalCount = delta.getGlobalCount();

        // Deltas expose immutable collections, so retaining them avoids a
        // second full copy immediately before network serialization.
        globalMobCounts = delta.getGlobalMobCounts();
        removedGlobalMobIds = delta.getRemovedGlobalMobIds();
        regionCounters = delta.getRegionCounters();
        removedRegions = delta.getRemovedRegions();
        dimensionNames = delta.getDimensionNames();
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        PacketBuffer buffer = new PacketBuffer(buf);
        globalCount = buffer.readInt();

        removedGlobalMobIds = readMobIds(buffer);
        globalMobCounts = readMobCounts(buffer);
        removedRegions = readRegionKeys(buffer);

        dimensionNames = new HashMap<>();
        int dimensionCount = buffer.readInt();
        for (int index = 0; index < dimensionCount; index++) {
            dimensionNames.put(buffer.readInt(), buffer.readString(32767));
        }

        regionCounters = new HashMap<>();
        int regionCount = buffer.readInt();
        for (int index = 0; index < regionCount; index++) {
            long regionKey = buffer.readLong();
            int totalCount = buffer.readInt();
            regionCounters.put(regionKey, new TrackedMobSnapshot.RegionCounts(totalCount, readMobCounts(buffer)));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        PacketBuffer buffer = new PacketBuffer(buf);
        buffer.writeInt(globalCount);

        writeMobIds(buffer, removedGlobalMobIds);
        writeMobCounts(buffer, globalMobCounts);
        writeRegionKeys(buffer, removedRegions);

        buffer.writeInt(dimensionNames.size());
        for (Map.Entry<Integer, String> entry : dimensionNames.entrySet()) {
            buffer.writeInt(entry.getKey());
            buffer.writeString(entry.getValue());
        }

        buffer.writeInt(regionCounters.size());
        for (Map.Entry<Long, TrackedMobSnapshot.RegionCounts> entry : regionCounters.entrySet()) {
            buffer.writeLong(entry.getKey());
            buffer.writeInt(entry.getValue().getTotalCount());
            writeMobCounts(buffer, entry.getValue().getMobCounts());
        }
    }

    public TrackedMobDelta toDelta() {
        return new TrackedMobDelta(globalCount, globalMobCounts, removedGlobalMobIds, regionCounters, removedRegions, dimensionNames);
    }

    private static Map<ResourceLocation, Integer> readMobCounts(PacketBuffer buffer) {
        HashMap<ResourceLocation, Integer> mobCounts = new HashMap<>();
        int mobCount = buffer.readInt();

        for (int index = 0; index < mobCount; index++) {
            ResourceLocation mobId = MobIdUtils.tryParseMobId(buffer.readString(32767));
            int count = buffer.readInt();
            if (mobId == null) continue;

            mobCounts.put(mobId, count);
        }

        return mobCounts;
    }

    private static void writeMobCounts(PacketBuffer buffer, Map<ResourceLocation, Integer> mobCounts) {
        buffer.writeInt(mobCounts.size());
        for (Map.Entry<ResourceLocation, Integer> entry : mobCounts.entrySet()) {
            buffer.writeString(entry.getKey().toString());
            buffer.writeInt(entry.getValue());
        }
    }

    private static Set<ResourceLocation> readMobIds(PacketBuffer buffer) {
        LinkedHashSet<ResourceLocation> mobIds = new LinkedHashSet<>();
        int mobCount = buffer.readInt();

        for (int index = 0; index < mobCount; index++) {
            ResourceLocation mobId = MobIdUtils.tryParseMobId(buffer.readString(32767));
            if (mobId == null) continue;

            mobIds.add(mobId);
        }

        return mobIds;
    }

    private static void writeMobIds(PacketBuffer buffer, Set<ResourceLocation> mobIds) {
        buffer.writeInt(mobIds.size());
        for (ResourceLocation mobId : mobIds) buffer.writeString(mobId.toString());
    }

    private static Set<Long> readRegionKeys(PacketBuffer buffer) {
        LinkedHashSet<Long> regionKeys = new LinkedHashSet<>();
        int regionCount = buffer.readInt();

        for (int index = 0; index < regionCount; index++) regionKeys.add(buffer.readLong());

        return regionKeys;
    }

    private static void writeRegionKeys(PacketBuffer buffer, Set<Long> regionKeys) {
        buffer.writeInt(regionKeys.size());
        for (Long regionKey : regionKeys) buffer.writeLong(regionKey);
    }

    public static class ClientHandler implements IMessageHandler<PacketTrackingDelta, IMessage> {

        @Override
        public IMessage onMessage(PacketTrackingDelta message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> SpawnTrackerManager.applyDelta(message.toDelta()));
            return null;
        }
    }
}
