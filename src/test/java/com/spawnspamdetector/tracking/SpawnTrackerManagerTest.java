package com.spawnspamdetector.tracking;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import net.minecraft.util.ResourceLocation;


class SpawnTrackerManagerTest {

    @Test
    void topMobCountsLimitsResultsAndUsesStableTieOrdering() {
        Map<ResourceLocation, Integer> counts = new LinkedHashMap<>();
        for (int index = 0; index < 11; index++) {
            counts.put(new ResourceLocation("test", String.format("mob%02d", index)), index);
        }
        counts.put(new ResourceLocation("test", "alpha"), 10);

        List<Map.Entry<ResourceLocation, Integer>> topMobs = SpawnTrackerManager.getTopMobCounts(counts, 10);

        Assertions.assertEquals(10, topMobs.size());
        Assertions.assertEquals(new ResourceLocation("test", "alpha"), topMobs.get(0).getKey());
        Assertions.assertEquals(10, topMobs.get(0).getValue());
        Assertions.assertEquals(new ResourceLocation("test", "mob10"), topMobs.get(1).getKey());
        Assertions.assertEquals(10, topMobs.get(1).getValue());
        Assertions.assertEquals(2, topMobs.get(9).getValue());
    }
}
