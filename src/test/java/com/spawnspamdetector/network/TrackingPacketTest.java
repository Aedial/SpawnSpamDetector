package com.spawnspamdetector.network;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import net.minecraft.util.ResourceLocation;

import com.spawnspamdetector.tracking.LocationKey;
import com.spawnspamdetector.tracking.TrackedMobDelta;
import com.spawnspamdetector.tracking.TrackedMobSnapshot;


class TrackingPacketTest {

    private static final ResourceLocation ZOMBIE = new ResourceLocation("minecraft", "zombie");
    private static final ResourceLocation SKELETON = new ResourceLocation("minecraft", "skeleton");

    @Test
    void snapshotRoundTripsAllTrackingData() {
        long overworldRegion = LocationKey.fromRegion(0, -1, 2);
        long netherRegion = LocationKey.fromRegion(-1, 3, -4);
        TrackedMobSnapshot snapshot = new TrackedMobSnapshot(
            21,
            mobCounts(15, 6),
            regionCounts(overworldRegion, 15, netherRegion, 6),
            dimensionNames()
        );

        PacketTrackingSnapshot received = roundTrip(new PacketTrackingSnapshot(snapshot), new PacketTrackingSnapshot());
        TrackedMobSnapshot decoded = received.toSnapshot();

        Assertions.assertEquals(snapshot.getGlobalCount(), decoded.getGlobalCount());
        Assertions.assertEquals(snapshot.getGlobalMobCounts(), decoded.getGlobalMobCounts());
        assertRegionCountsEqual(snapshot.getRegionCounters(), decoded.getRegionCounters());
        Assertions.assertEquals(snapshot.getDimensionNames(), decoded.getDimensionNames());
    }

    @Test
    void deltaRoundTripsChangedAndRemovedData() {
        long changedRegion = LocationKey.fromRegion(0, 1, 1);
        long removedRegion = LocationKey.fromRegion(7, -2, 4);
        Set<ResourceLocation> removedMobIds = new LinkedHashSet<>(Collections.singletonList(SKELETON));
        Set<Long> removedRegions = new LinkedHashSet<>(Collections.singletonList(removedRegion));
        TrackedMobDelta delta = new TrackedMobDelta(
            9,
            Collections.singletonMap(ZOMBIE, 9),
            removedMobIds,
            Collections.singletonMap(changedRegion, new TrackedMobSnapshot.RegionCounts(9, Collections.singletonMap(ZOMBIE, 9))),
            removedRegions,
            Collections.singletonMap(0, "Overworld")
        );

        PacketTrackingDelta received = roundTrip(new PacketTrackingDelta(delta), new PacketTrackingDelta());
        TrackedMobDelta decoded = received.toDelta();

        Assertions.assertEquals(delta.getGlobalCount(), decoded.getGlobalCount());
        Assertions.assertEquals(delta.getGlobalMobCounts(), decoded.getGlobalMobCounts());
        Assertions.assertEquals(delta.getRemovedGlobalMobIds(), decoded.getRemovedGlobalMobIds());
        assertRegionCountsEqual(delta.getRegionCounters(), decoded.getRegionCounters());
        Assertions.assertEquals(delta.getRemovedRegions(), decoded.getRemovedRegions());
        Assertions.assertEquals(delta.getDimensionNames(), decoded.getDimensionNames());
    }

    private static PacketTrackingSnapshot roundTrip(PacketTrackingSnapshot sent, PacketTrackingSnapshot received) {
        ByteBuf buffer = Unpooled.buffer();
        sent.toBytes(buffer);
        received.fromBytes(buffer);
        return received;
    }

    private static PacketTrackingDelta roundTrip(PacketTrackingDelta sent, PacketTrackingDelta received) {
        ByteBuf buffer = Unpooled.buffer();
        sent.toBytes(buffer);
        received.fromBytes(buffer);
        return received;
    }

    private static void assertRegionCountsEqual(
        Map<Long, TrackedMobSnapshot.RegionCounts> expected,
        Map<Long, TrackedMobSnapshot.RegionCounts> actual) {
        Assertions.assertEquals(expected.keySet(), actual.keySet());

        for (Long regionKey : expected.keySet()) {
            TrackedMobSnapshot.RegionCounts expectedCounts = expected.get(regionKey);
            TrackedMobSnapshot.RegionCounts actualCounts = actual.get(regionKey);
            Assertions.assertEquals(expectedCounts.getTotalCount(), actualCounts.getTotalCount());
            Assertions.assertEquals(expectedCounts.getMobCounts(), actualCounts.getMobCounts());
        }
    }

    private static Map<ResourceLocation, Integer> mobCounts(int zombieCount, int skeletonCount) {
        Map<ResourceLocation, Integer> mobCounts = new LinkedHashMap<>();
        mobCounts.put(ZOMBIE, zombieCount);
        mobCounts.put(SKELETON, skeletonCount);
        return mobCounts;
    }

    private static Map<Long, TrackedMobSnapshot.RegionCounts> regionCounts(
        long firstRegion,
        int firstCount,
        long secondRegion,
        int secondCount) {
        Map<Long, TrackedMobSnapshot.RegionCounts> regionCounts = new LinkedHashMap<>();
        regionCounts.put(firstRegion, new TrackedMobSnapshot.RegionCounts(firstCount, Collections.singletonMap(ZOMBIE, firstCount)));
        regionCounts.put(secondRegion, new TrackedMobSnapshot.RegionCounts(secondCount, Collections.singletonMap(SKELETON, secondCount)));
        return regionCounts;
    }

    private static Map<Integer, String> dimensionNames() {
        Map<Integer, String> dimensionNames = new LinkedHashMap<>();
        dimensionNames.put(-1, "Nether");
        dimensionNames.put(0, "Overworld");
        return dimensionNames;
    }
}
