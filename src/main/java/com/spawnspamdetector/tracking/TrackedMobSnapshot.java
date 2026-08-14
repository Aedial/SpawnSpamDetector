package com.spawnspamdetector.tracking;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.util.ResourceLocation;


public final class TrackedMobSnapshot {

    private final int globalCount;
    private final Map<ResourceLocation, Integer> globalMobCounts;
    private final Map<Long, RegionCounts> regionCounters;
    private final Map<Integer, String> dimensionNames;

    public TrackedMobSnapshot(
        int globalCount,
        Map<ResourceLocation, Integer> globalMobCounts,
        Map<Long, RegionCounts> regionCounters,
        Map<Integer, String> dimensionNames) {
        this.globalCount = globalCount;
        this.globalMobCounts = copyGlobalMobCounts(globalMobCounts);
        this.regionCounters = copyRegionCounters(regionCounters);
        this.dimensionNames = copyDimensionNames(dimensionNames);
    }

    public int getGlobalCount() {
        return globalCount;
    }

    public Map<ResourceLocation, Integer> getGlobalMobCounts() {
        return globalMobCounts;
    }

    public Map<Long, RegionCounts> getRegionCounters() {
        return regionCounters;
    }

    public Map<Integer, String> getDimensionNames() {
        return dimensionNames;
    }

    private static Map<ResourceLocation, Integer> copyGlobalMobCounts(Map<ResourceLocation, Integer> globalMobCounts) {
        if (globalMobCounts == null || globalMobCounts.isEmpty()) return Collections.emptyMap();

        return Collections.unmodifiableMap(new LinkedHashMap<>(globalMobCounts));
    }

    private static Map<Long, RegionCounts> copyRegionCounters(Map<Long, RegionCounts> regionCounters) {
        if (regionCounters == null || regionCounters.isEmpty()) return Collections.emptyMap();

        LinkedHashMap<Long, RegionCounts> copy = new LinkedHashMap<>();
        for (Map.Entry<Long, RegionCounts> entry : regionCounters.entrySet()) {
            copy.put(entry.getKey(), new RegionCounts(entry.getValue().getTotalCount(), entry.getValue().getMobCounts()));
        }

        return Collections.unmodifiableMap(copy);
    }

    private static Map<Integer, String> copyDimensionNames(Map<Integer, String> dimensionNames) {
        if (dimensionNames == null || dimensionNames.isEmpty()) return Collections.emptyMap();

        return Collections.unmodifiableMap(new LinkedHashMap<>(dimensionNames));
    }

    public static final class RegionCounts {

        private final int totalCount;
        private final Map<ResourceLocation, Integer> mobCounts;

        public RegionCounts(int totalCount, Map<ResourceLocation, Integer> mobCounts) {
            this.totalCount = totalCount;
            this.mobCounts = copyGlobalMobCounts(mobCounts);
        }

        public int getTotalCount() {
            return totalCount;
        }

        public Map<ResourceLocation, Integer> getMobCounts() {
            return mobCounts;
        }
    }
}