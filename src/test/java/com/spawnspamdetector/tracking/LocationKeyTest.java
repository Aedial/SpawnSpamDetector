package com.spawnspamdetector.tracking;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class LocationKeyTest {

    @Test
    void roundTripsDimensionsAndRegionsAtSupportedBounds() {
        int[] dimensions = {-134_217_728, -1, 0, 1, 134_217_727};
        int[] regions = {-131_072, -1, 0, 1, 131_071};

        for (int dimension : dimensions) {
            for (int regionX : regions) {
                for (int regionZ : regions) {
                    long key = LocationKey.fromRegion(dimension, regionX, regionZ);

                    Assertions.assertEquals(dimension, LocationKey.getDimension(key));
                    Assertions.assertEquals(regionX, LocationKey.getRegionX(key));
                    Assertions.assertEquals(regionZ, LocationKey.getRegionZ(key));
                }
            }
        }
    }

    @Test
    void blockCoordinatesUseFloorDivisionAtRegionBoundaries() {
        Assertions.assertEquals(-1, LocationKey.getRegionX(LocationKey.fromBlockPosition(0, -0.01D, 0D)));
        Assertions.assertEquals(-1, LocationKey.getRegionX(LocationKey.fromBlockPosition(0, -512D, 0D)));
        Assertions.assertEquals(-2, LocationKey.getRegionX(LocationKey.fromBlockPosition(0, -512.01D, 0D)));
        Assertions.assertEquals(0, LocationKey.getRegionX(LocationKey.fromBlockPosition(0, 511.99D, 0D)));
        Assertions.assertEquals(1, LocationKey.getRegionX(LocationKey.fromBlockPosition(0, 512D, 0D)));
    }
}
