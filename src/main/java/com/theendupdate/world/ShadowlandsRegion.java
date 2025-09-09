package com.theendupdate.world;

/**
 * Deterministic, seed-agnostic region mask for Shadowlands mega-biomes.
 * Generates large contiguous blobs (~1000+ blocks across) using low-frequency value noise
 * over chunk coordinates, avoiding any cross-chunk world lookups during generation.
 */
public final class ShadowlandsRegion {
    private ShadowlandsRegion() {}

    // Per-world seed; set on server world load
    private static volatile long WORLD_SEED = 0xA1B2C3D4E5F60718L;

    public static void setSeed(long seed) {
        WORLD_SEED = seed ^ 0x9E3779B97F4A7C15L; // mix for diffusion
    }

    // Coarse grid where each cell can host at most one mega-region disc
    private static final int CELL_SIZE_CHUNKS = 48; // slightly tighter grid to increase chances
    // Minimum radius in chunks to guarantee >= 1000 blocks radius (>= 2000 diameter)
    private static final int MIN_RADIUS_CHUNKS = 48; // 48*16 = 768 blocks radius (1536 diameter)
    private static final int EXTRA_RADIUS_CHUNKS = 24; // up to +384 blocks for variability
    // Activation rarity per cell (higher -> rarer)
    private static final double ACTIVATION_THRESHOLD = 0.995; // slightly more common, still rare
    // Exclude the main island (dragon fight) area completely
    private static final int EXCLUSION_RADIUS_BLOCKS = 1600; // do not generate inside this radius
    private static final long EXCLUSION_RADIUS_BLOCKS_SQ = (long) EXCLUSION_RADIUS_BLOCKS * (long) EXCLUSION_RADIUS_BLOCKS;

    // Hashing utilities (64-bit mix, adapted from SplitMix64-like mixers)
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
        h = mix64(h ^ x * 0x9E3779B97F4A7C15L);
        h = mix64(h ^ z * 0xC2B2AE3D27D4EB4FL);
        return h;
    }

    public static boolean isInRegion(int chunkX, int chunkZ) {
        int bx = (chunkX << 4) + 8;
        int bz = (chunkZ << 4) + 8;
        return isInRegionBlocks(bx, bz);
    }

    public static boolean isInRegionBlocks(int blockX, int blockZ) {
        long bx = (long) blockX;
        long bz = (long) blockZ;
        long dist2 = bx * bx + bz * bz;
        if (dist2 < EXCLUSION_RADIUS_BLOCKS_SQ) return false;

        int chunkX = Math.floorDiv(blockX, 16);
        int chunkZ = Math.floorDiv(blockZ, 16);

        int cellX = Math.floorDiv(chunkX, CELL_SIZE_CHUNKS);
        int cellZ = Math.floorDiv(chunkZ, CELL_SIZE_CHUNKS);

        long h = hash2D(0xA1B2C3D4E5F60718L, cellX, cellZ);
        double a = (Double.longBitsToDouble((h >>> 12) | 0x3FF0000000000000L) - 1.0);
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


