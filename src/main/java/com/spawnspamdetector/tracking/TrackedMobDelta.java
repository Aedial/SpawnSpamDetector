package com.spawnspamdetector.tracking;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.util.ResourceLocation;


public final class TrackedMobDelta {

    private final int globalCount;
    private final Map<ResourceLocation, Integer> globalMobCounts;
    private final Set<ResourceLocation> removedGlobalMobIds;
    private final Map<Long, TrackedMobSnapshot.RegionCounts> regionCounters;
    private final Set<Long> removedRegions;
    private final Map<Integer, String> dimensionNames;

    public TrackedMobDelta(
        int globalCount,
        Map<ResourceLocation, Integer> globalMobCounts,
        Set<ResourceLocation> removedGlobalMobIds,
        Map<Long, TrackedMobSnapshot.RegionCounts> regionCounters,
        Set<Long> removedRegions,
        Map<Integer, String> dimensionNames) {
        this.globalCount = globalCount;
        this.globalMobCounts = copyMobCounts(globalMobCounts);
        this.removedGlobalMobIds = copyMobIds(removedGlobalMobIds);
        this.regionCounters = copyRegionCounters(regionCounters);
        this.removedRegions = copyRegionKeys(removedRegions);
        this.dimensionNames = copyDimensionNames(dimensionNames);
    }

    public int getGlobalCount() {
        return globalCount;
    }

    public Map<ResourceLocation, Integer> getGlobalMobCounts() {
        return globalMobCounts;
    }

    public Set<ResourceLocation> getRemovedGlobalMobIds() {
        return removedGlobalMobIds;
    }

    public Map<Long, TrackedMobSnapshot.RegionCounts> getRegionCounters() {
        return regionCounters;
    }

    public Set<Long> getRemovedRegions() {
        return removedRegions;
    }

    public Map<Integer, String> getDimensionNames() {
        return dimensionNames;
    }

    private static Map<ResourceLocation, Integer> copyMobCounts(Map<ResourceLocation, Integer> mobCounts) {
        if (mobCounts == null || mobCounts.isEmpty()) return Collections.emptyMap();

        return Collections.unmodifiableMap(new LinkedHashMap<>(mobCounts));
    }

    private static Set<ResourceLocation> copyMobIds(Set<ResourceLocation> mobIds) {
        if (mobIds == null || mobIds.isEmpty()) return Collections.emptySet();

        return Collections.unmodifiableSet(new LinkedHashSet<>(mobIds));
    }

    private static Map<Long, TrackedMobSnapshot.RegionCounts> copyRegionCounters(Map<Long, TrackedMobSnapshot.RegionCounts> regionCounters) {
        if (regionCounters == null || regionCounters.isEmpty()) return Collections.emptyMap();

        LinkedHashMap<Long, TrackedMobSnapshot.RegionCounts> copy = new LinkedHashMap<>();
        for (Map.Entry<Long, TrackedMobSnapshot.RegionCounts> entry : regionCounters.entrySet()) {
            copy.put(entry.getKey(), new TrackedMobSnapshot.RegionCounts(entry.getValue().getTotalCount(), entry.getValue().getMobCounts()));
        }

        return Collections.unmodifiableMap(copy);
    }

    private static Set<Long> copyRegionKeys(Set<Long> regionKeys) {
        if (regionKeys == null || regionKeys.isEmpty()) return Collections.emptySet();

        return Collections.unmodifiableSet(new LinkedHashSet<>(regionKeys));
    }

    private static Map<Integer, String> copyDimensionNames(Map<Integer, String> dimensionNames) {
        if (dimensionNames == null || dimensionNames.isEmpty()) return Collections.emptyMap();

        return Collections.unmodifiableMap(new LinkedHashMap<>(dimensionNames));
    }
}