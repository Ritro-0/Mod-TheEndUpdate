package com.theendupdate.world;

/**
 * Seeded region mask for Mirelands mega-biomes.
 * Uses disjoint hashed cell activation to create large, rare blobs, explicitly
 * biased to avoid overlap with ShadowlandsRegion.
 */
public final class MirelandsRegion {
    private MirelandsRegion() {}

    private static volatile long WORLD_SEED = 0L;

    public static void setSeed(long seed) {
        WORLD_SEED = seed ^ 0xC3A5C85C97CB3127L;
    }

    private static long mix64(long x) {
        x ^= (x >>> 33);
        x *= 0xff51afd7ed558ccdL;
        x ^= (x >>> 33);
        x *= 0xc4ceb9fe1a85ec53L;
        x ^= (x >>> 33);
        return x;
    }

    private static long hash2D(long seed, int x, int z) {
        long h = seed ^ WORLD_SEED;
        h = mix64(h ^ (long)x * 0x9E3779B97F4A7C15L);
        h = mix64(h ^ (long)z * 0xC2B2AE3D27D4EB4FL);
        return h;
    }

    private static final int CELL_SIZE_CHUNKS = 56; // slightly different scale from Shadowlands
    private static final int MIN_RADIUS_CHUNKS = 40; // ~640 blocks radius
    private static final int EXTRA_RADIUS_CHUNKS = 16; // up to +256 blocks
    private static final double ACTIVATION_THRESHOLD = 0.9965; // rare but a bit more common than Shadowlands
    private static final int EXCLUSION_RADIUS_BLOCKS = 1024; // keep away from main island
    private static final long EXCLUSION_RADIUS_BLOCKS_SQ = (long)EXCLUSION_RADIUS_BLOCKS * (long)EXCLUSION_RADIUS_BLOCKS;

    public static boolean isInRegion(int chunkX, int chunkZ) {
        int bx = (chunkX << 4) + 8;
        int bz = (chunkZ << 4) + 8;
        return isInRegionBlocks(bx, bz);
    }

    public static boolean isInRegionBlocks(int blockX, int blockZ) {
        long bx = (long) blockX;
        long bz = (long) blockZ;
        if (bx * bx + bz * bz < EXCLUSION_RADIUS_BLOCKS_SQ) return false;

        int chunkX = Math.floorDiv(blockX, 16);
        int chunkZ = Math.floorDiv(blockZ, 16);
        int cellX = Math.floorDiv(chunkX, CELL_SIZE_CHUNKS);
        int cellZ = Math.floorDiv(chunkZ, CELL_SIZE_CHUNKS);

        long h = hash2D(0x13579BDF2468ACE0L, cellX, cellZ);
        // De-conflict with Shadowlands: if Shadowlands active here, bias against Mirelands
        // by lowering random value when both try to activate.
        double a = (Double.longBitsToDouble((h >>> 12) | 0x3FF0000000000000L) - 1.0);
        if (ShadowlandsRegion.isInRegionBlocks(blockX, blockZ)) {
            a *= 0.9; // reduce activation so overlap is unlikely
        }
        if (a < ACTIVATION_THRESHOLD) return false;

        long h2 = mix64(h ^ 0x9E3779B97F4A7C15L);
        long h3 = mix64(h ^ 0xC2B2AE3D27D4EB4FL);
        int jitterX = (int)((((h2 >>> 32) & 0xffff) / 65535.0) * 16.0) - 8;
        int jitterZ = (int)((((h3 >>> 32) & 0xffff) / 65535.0) * 16.0) - 8;
        int centerChunkX = cellX * CELL_SIZE_CHUNKS + CELL_SIZE_CHUNKS / 2 + jitterX;
        int centerChunkZ = cellZ * CELL_SIZE_CHUNKS + CELL_SIZE_CHUNKS / 2 + jitterZ;

        long h4 = mix64(h ^ 0xD6E8FEB86659FD93L);
        int extra = (int)((((h4 >>> 40) & 0xff) / 255.0) * EXTRA_RADIUS_CHUNKS);
        int radiusChunks = MIN_RADIUS_CHUNKS + extra;

        int centerBlockX = (centerChunkX << 4) + 8;
        int centerBlockZ = (centerChunkZ << 4) + 8;
        int radiusBlocks = radiusChunks << 4;

        long dx = (long) blockX - (long) centerBlockX;
        long dz = (long) blockZ - (long) centerBlockZ;
        return dx * dx + dz * dz <= (long) radiusBlocks * (long) radiusBlocks;
    }
}


