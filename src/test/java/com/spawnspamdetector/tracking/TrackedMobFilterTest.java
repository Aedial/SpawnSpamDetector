package com.spawnspamdetector.tracking;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import net.minecraft.util.ResourceLocation;


class TrackedMobFilterTest {

    private static final ResourceLocation ZOMBIE = new ResourceLocation("minecraft", "zombie");
    private static final ResourceLocation SKELETON = new ResourceLocation("minecraft", "skeleton");

    @Test
    void emptyListsTrackEveryValidMob() {
        TrackedMobFilter filter = new TrackedMobFilter(Collections.emptySet(), Collections.emptySet());

        Assertions.assertTrue(filter.matches(ZOMBIE));
        Assertions.assertFalse(filter.matches(null));
    }

    @Test
    void blacklistExcludesOnlyListedMobs() {
        TrackedMobFilter filter = new TrackedMobFilter(Collections.emptySet(), singleton(ZOMBIE));

        Assertions.assertFalse(filter.matches(ZOMBIE));
        Assertions.assertTrue(filter.matches(SKELETON));
    }

    @Test
    void whitelistTakesPrecedenceOverBlacklist() {
        TrackedMobFilter filter = new TrackedMobFilter(singleton(ZOMBIE), singleton(ZOMBIE));

        Assertions.assertTrue(filter.matches(ZOMBIE));
        Assertions.assertFalse(filter.matches(SKELETON));
    }

    private static Set<ResourceLocation> singleton(ResourceLocation mobId) {
        Set<ResourceLocation> mobIds = new LinkedHashSet<>();
        mobIds.add(mobId);
        return mobIds;
    }
}
