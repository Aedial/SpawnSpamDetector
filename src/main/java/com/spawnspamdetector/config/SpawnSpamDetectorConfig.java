package com.spawnspamdetector.config;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;

import com.spawnspamdetector.SpawnSpamDetector;
import com.spawnspamdetector.Tags;
import com.spawnspamdetector.tracking.TrackedMobFilter;
import com.spawnspamdetector.util.MobIdUtils;


@Config(modid = Tags.MODID, name = Tags.MODID, category = "")
@Config.LangKey(Tags.MODID + ".config.title")
public final class SpawnSpamDetectorConfig {

    private static final String CATEGORY_GENERAL = "general";
    private static final String CATEGORY_FILTERS = "filters";

    @Config.Name(CATEGORY_GENERAL)
    @Config.LangKey(Tags.MODID + ".config.category.general")
    @Config.Comment("General detection thresholds and cooldown settings.")
    public static GeneralCategory general = new GeneralCategory();

    @Config.Name(CATEGORY_FILTERS)
    @Config.LangKey(Tags.MODID + ".config.category.filters")
    @Config.Comment("Mob whitelist and blacklist filters.")
    public static FiltersCategory filters = new FiltersCategory();

    private static Set<ResourceLocation> whitelistedMobIds = Collections.emptySet();
    private static Set<ResourceLocation> blacklistedMobIds = Collections.emptySet();

    private SpawnSpamDetectorConfig() {
    }

    public static void init(File configFile) {
        syncConfig();
    }

    public static boolean isDetectionEnabled() {
        return general.enableDetection;
    }

    public static boolean isMobTracked(ResourceLocation mobId) {
        if (!isDetectionEnabled()) return false;
        if (mobId == null) return false;

        if (!whitelistedMobIds.isEmpty()) return whitelistedMobIds.contains(mobId);

        return !blacklistedMobIds.contains(mobId);
    }

    public static long getDetectionCooldownMillis() {
        return Math.max(1L, general.detectionCooldown) * 1_000L;
    }

    public static TrackedMobFilter.FilterKey getTrackingFilterKey() {
        return TrackedMobFilter.createKey(whitelistedMobIds, blacklistedMobIds);
    }

    public static Set<ResourceLocation> getWhitelistedMobIds() {
        return whitelistedMobIds;
    }

    public static Set<ResourceLocation> getBlacklistedMobIds() {
        return blacklistedMobIds;
    }

    public static void syncConfig() {
        ConfigManager.sync(Tags.MODID, Config.Type.INSTANCE);
        whitelistedMobIds = Collections.unmodifiableSet(parseMobIds(filters.whitelistedMobs, "whitelist"));
        blacklistedMobIds = Collections.unmodifiableSet(parseMobIds(filters.blacklistedMobs, "blacklist"));
    }

    private static Set<ResourceLocation> parseMobIds(String[] rawEntries, String listName) {
        LinkedHashSet<ResourceLocation> mobIds = new LinkedHashSet<>();

        if (rawEntries == null) return mobIds;

        for (String rawEntry : rawEntries) {
            ResourceLocation mobId = MobIdUtils.tryParseMobId(rawEntry);
            if (mobId == null) {
                if (rawEntry == null || rawEntry.trim().isEmpty()) continue;

                SpawnSpamDetector.LOGGER.warn("Ignoring invalid {} mob id '{}'", listName, rawEntry);
                continue;
            }

            mobIds.add(mobId);
        }

        return mobIds;
    }

    public static class GeneralCategory {

        @Config.Name("enableDetection")
        @Config.LangKey(Tags.MODID + ".config.enableDetection")
        @Config.Comment("Enable or disable spawn spam detection and alerts.")
        public boolean enableDetection = true;

        @Config.Name("regionDetectionThreshold")
        @Config.LangKey(Tags.MODID + ".config.regionDetectionThreshold")
        @Config.Comment("Warn when a region reaches this many tracked mobs in total.")
        @Config.RangeInt(min = 1)
        public int regionDetectionThreshold = 1000;

        @Config.Name("regionDetectionThresholdPerType")
        @Config.LangKey(Tags.MODID + ".config.regionDetectionThresholdPerType")
        @Config.Comment("Warn when a single mob type reaches this count inside one region.")
        @Config.RangeInt(min = 1)
        public int regionDetectionThresholdPerType = 100;

        @Config.Name("globalDetectionThreshold")
        @Config.LangKey(Tags.MODID + ".config.globalDetectionThreshold")
        @Config.Comment("Warn when the total tracked mob count across loaded regions reaches this value.")
        @Config.RangeInt(min = 1)
        public int globalDetectionThreshold = 1000;

        @Config.Name("globalDetectionThresholdPerType")
        @Config.LangKey(Tags.MODID + ".config.globalDetectionThresholdPerType")
        @Config.Comment("Warn when one mob type reaches this count across loaded regions.")
        @Config.RangeInt(min = 1)
        public int globalDetectionThresholdPerType = 100;

        @Config.Name("detectionCooldown")
        @Config.LangKey(Tags.MODID + ".config.detectionCooldown")
        @Config.Comment("Minimum time between repeated alerts for the same threshold, in seconds.")
        @Config.RangeInt(min = 1)
        public int detectionCooldown = 5 * 60;
    }

    public static class FiltersCategory {

        @Config.Name("whitelistedMobs")
        @Config.LangKey(Tags.MODID + ".config.whitelistedMobs")
        @Config.Comment("Server-side only. When this list is not empty, only these mob ids will be tracked and blacklist entries are ignored. Example: minecraft:bat")
        public String[] whitelistedMobs = new String[0];

        @Config.Name("blacklistedMobs")
        @Config.LangKey(Tags.MODID + ".config.blacklistedMobs")
        @Config.Comment("Server-side only. These mob ids will never be tracked unless they are explicitly whitelisted. Example: minecraft:bat")
        public String[] blacklistedMobs = new String[0];
    }
}