package com.spawnspamdetector.tracking;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;


public final class LocationKey {

    public static final int REGION_SIZE = 512;

    private static final int REGION_BITS = 18;
    private static final int DIMENSION_BITS = 28;

    private static final int REGION_MASK = (1 << REGION_BITS) - 1;
    private static final int DIMENSION_MASK = (1 << DIMENSION_BITS) - 1;
    private static final int REGION_SHIFT = REGION_BITS;
    private static final int DIMENSION_SHIFT = REGION_BITS * 2;

    private static final int CHUNKS_PER_REGION = REGION_SIZE / 16;

    private LocationKey() {
    }

    // Pack dimension, region X, and region Z into one long to keep the hot path lean.
    public static long fromEntity(Entity entity) {
        return fromBlockPosition(entity.dimension, entity.posX, entity.posZ);
    }

    public static long fromBlockPosition(int dimensionId, double posX, double posZ) {
        int regionX = Math.floorDiv(MathHelper.floor(posX), REGION_SIZE);
        int regionZ = Math.floorDiv(MathHelper.floor(posZ), REGION_SIZE);
        return fromRegion(dimensionId, regionX, regionZ);
    }

    public static long fromChunk(int dimensionId, int chunkX, int chunkZ) {
        int regionX = Math.floorDiv(chunkX, CHUNKS_PER_REGION);
        int regionZ = Math.floorDiv(chunkZ, CHUNKS_PER_REGION);
        return fromRegion(dimensionId, regionX, regionZ);
    }

    public static long fromRegion(int dimensionId, int regionX, int regionZ) {
        long packedDimension = (long) (dimensionId & DIMENSION_MASK) << DIMENSION_SHIFT;
        long packedRegionX = (long) (regionX & REGION_MASK) << REGION_SHIFT;
        long packedRegionZ = regionZ & REGION_MASK;
        return packedDimension | packedRegionX | packedRegionZ;
    }

    public static int getDimension(long key) {
        return signExtend((int) ((key >> DIMENSION_SHIFT) & DIMENSION_MASK), DIMENSION_BITS);
    }

    public static int getRegionX(long key) {
        return signExtend((int) ((key >> REGION_SHIFT) & REGION_MASK), REGION_BITS);
    }

    public static int getRegionZ(long key) {
        return signExtend((int) (key & REGION_MASK), REGION_BITS);
    }

    public static int getRegionMinX(long key) {
        return getRegionX(key) * REGION_SIZE;
    }

    public static int getRegionMaxX(long key) {
        return getRegionMinX(key) + REGION_SIZE - 1;
    }

    public static int getRegionMinZ(long key) {
        return getRegionZ(key) * REGION_SIZE;
    }

    public static int getRegionMaxZ(long key) {
        return getRegionMinZ(key) + REGION_SIZE - 1;
    }

    private static int signExtend(int value, int bits) {
        int signBit = 1 << (bits - 1);
        return (value ^ signBit) - signBit;
    }
}