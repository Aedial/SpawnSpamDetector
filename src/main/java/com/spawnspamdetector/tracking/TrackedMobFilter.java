package com.spawnspamdetector.tracking;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import net.minecraft.util.ResourceLocation;


public final class TrackedMobFilter {

    private final Set<ResourceLocation> whitelistedMobIds;
    private final Set<ResourceLocation> blacklistedMobIds;

    public TrackedMobFilter(Set<ResourceLocation> whitelistedMobIds, Set<ResourceLocation> blacklistedMobIds) {
        this.whitelistedMobIds = copyMobIds(whitelistedMobIds);
        this.blacklistedMobIds = copyMobIds(blacklistedMobIds);
    }

    public boolean matches(ResourceLocation mobId) {
        if (mobId == null) return false;
        if (!whitelistedMobIds.isEmpty()) return whitelistedMobIds.contains(mobId);

        return !blacklistedMobIds.contains(mobId);
    }

    public Set<ResourceLocation> getWhitelistedMobIds() {
        return whitelistedMobIds;
    }

    public Set<ResourceLocation> getBlacklistedMobIds() {
        return blacklistedMobIds;
    }

    public static FilterKey createKey(Set<ResourceLocation> whitelistedMobIds, Set<ResourceLocation> blacklistedMobIds) {
        return new FilterKey(whitelistedMobIds, blacklistedMobIds);
    }

    private static Set<ResourceLocation> copyMobIds(Set<ResourceLocation> mobIds) {
        if (mobIds == null || mobIds.isEmpty()) return Collections.emptySet();

        return Collections.unmodifiableSet(new LinkedHashSet<>(mobIds));
    }

    public static final class FilterKey {

        private final Set<ResourceLocation> whitelistedMobIds;
        private final Set<ResourceLocation> blacklistedMobIds;

        private FilterKey(Set<ResourceLocation> whitelistedMobIds, Set<ResourceLocation> blacklistedMobIds) {
            this.whitelistedMobIds = copyMobIds(whitelistedMobIds);
            this.blacklistedMobIds = copyMobIds(blacklistedMobIds);
        }

        public TrackedMobFilter createFilter() {
            return new TrackedMobFilter(whitelistedMobIds, blacklistedMobIds);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof FilterKey)) return false;

            FilterKey filterKey = (FilterKey) other;
            if (!whitelistedMobIds.equals(filterKey.whitelistedMobIds)) return false;

            return blacklistedMobIds.equals(filterKey.blacklistedMobIds);
        }

        @Override
        public int hashCode() {
            return 31 * whitelistedMobIds.hashCode() + blacklistedMobIds.hashCode();
        }
    }
}