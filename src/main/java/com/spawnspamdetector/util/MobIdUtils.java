package com.spawnspamdetector.util;

import java.util.regex.Pattern;

import net.minecraft.util.ResourceLocation;


public final class MobIdUtils {

    private static final Pattern RESOURCE_NAMESPACE = Pattern.compile("[a-z0-9_.-]+");
    private static final Pattern RESOURCE_PATH = Pattern.compile("[a-z0-9_./-]+");

    private MobIdUtils() {
    }

    public static ResourceLocation tryParseMobId(String rawEntry) {
        if (rawEntry == null) return null;

        String trimmedEntry = rawEntry.trim();
        if (trimmedEntry.isEmpty()) return null;

        int separatorIndex = trimmedEntry.indexOf(':');
        if (separatorIndex <= 0 || separatorIndex == trimmedEntry.length() - 1) return null;

        String namespace = trimmedEntry.substring(0, separatorIndex);
        String path = trimmedEntry.substring(separatorIndex + 1);

        if (!RESOURCE_NAMESPACE.matcher(namespace).matches()) return null;
        if (!RESOURCE_PATH.matcher(path).matches()) return null;

        return new ResourceLocation(namespace, path);
    }
}