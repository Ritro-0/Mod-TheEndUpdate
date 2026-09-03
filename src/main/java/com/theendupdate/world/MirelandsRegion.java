package com.theendupdate.world;

/**
 * Compatibility wrapper. Mirelands placement now lives in {@link OuterEndLayout}.
 */
public final class MirelandsRegion {
    private MirelandsRegion() {}

    public static void setSeed(long seed) {
        OuterEndLayout.setSeed(seed);
    }

    public static boolean isInRegion(int chunkX, int chunkZ) {
        int bx = (chunkX << 4) + 8;
        int bz = (chunkZ << 4) + 8;
        return isInRegionBlocks(bx, bz);
    }

    public static boolean isInRegionBlocks(int blockX, int blockZ) {
        return OuterEndLayout.isMirelands(blockX, blockZ);
    }
}
