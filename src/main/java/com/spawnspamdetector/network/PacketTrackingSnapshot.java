package com.spawnspamdetector.network;

import java.util.HashMap;
import java.util.Map;

import io.netty.buffer.ByteBuf;

import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import com.spawnspamdetector.tracking.SpawnTrackerManager;
import com.spawnspamdetector.tracking.TrackedMobSnapshot;
import com.spawnspamdetector.util.MobIdUtils;


public class PacketTrackingSnapshot implements IMessage {

    private int globalCount;
    private Map<ResourceLocation, Integer> globalMobCounts = new HashMap<>();
    private Map<Long, TrackedMobSnapshot.RegionCounts> regionCounters = new HashMap<>();
    private Map<Integer, String> dimensionNames = new HashMap<>();

    public PacketTrackingSnapshot() {
    }

    public PacketTrackingSnapshot(TrackedMobSnapshot snapshot) {
        globalCount = snapshot.getGlobalCount();

        // Snapshots expose immutable collections, so retaining them avoids a
        // second full copy immediately before network serialization.
        globalMobCounts = snapshot.getGlobalMobCounts();
        regionCounters = snapshot.getRegionCounters();
        dimensionNames = snapshot.getDimensionNames();
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        PacketBuffer buffer = new PacketBuffer(buf);
        globalCount = buffer.readInt();

        dimensionNames = new HashMap<>();
        int dimensionCount = buffer.readInt();
        for (int index = 0; index < dimensionCount; index++) {
            dimensionNames.put(buffer.readInt(), buffer.readString(32767));
        }

        globalMobCounts = readMobCounts(buffer);
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

        buffer.writeInt(dimensionNames.size());
        for (Map.Entry<Integer, String> entry : dimensionNames.entrySet()) {
            buffer.writeInt(entry.getKey());
            buffer.writeString(entry.getValue());
        }

        writeMobCounts(buffer, globalMobCounts);
        buffer.writeInt(regionCounters.size());

        for (Map.Entry<Long, TrackedMobSnapshot.RegionCounts> entry : regionCounters.entrySet()) {
            buffer.writeLong(entry.getKey());
            buffer.writeInt(entry.getValue().getTotalCount());
            writeMobCounts(buffer, entry.getValue().getMobCounts());
        }
    }

    public TrackedMobSnapshot toSnapshot() {
        return new TrackedMobSnapshot(globalCount, globalMobCounts, regionCounters, dimensionNames);
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

    public static class ClientHandler implements IMessageHandler<PacketTrackingSnapshot, IMessage> {

        @Override
        public IMessage onMessage(PacketTrackingSnapshot message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> SpawnTrackerManager.applySnapshot(message.toSnapshot()));
            return null;
        }
    }
}
