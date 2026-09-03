// pre-existing libraries for precedural biome generation didn't seem to provide the spacing & scale I wanted here.
// This is my (probably way too complicated) way of spacing out and enlarging (dramatically) the Shadowlands biomes, along with adding in some visual features. Mirelands cells are handled here as well, they were far simpler.
package com.theendupdate.world;

/**
 * Shadowlands continents are generated here. Mirelands stay on vanilla End
 * island terrain; we paint whole 384-block cells of those islands as Mirelands.
 */
public final class OuterEndLayout {
    public enum Family {
        CENTER,
        VANILLA,
        MIRELANDS,
        SHADOWLANDS
    }

    private OuterEndLayout() {}

    private static volatile long SEED = 0L;

    private static final int CENTER_EXCLUSION_BLOCKS = 1400;
    private static final long CENTER_EXCLUSION_BLOCKS_SQ =
        (long) CENTER_EXCLUSION_BLOCKS * (long) CENTER_EXCLUSION_BLOCKS;

    private static final int SHADOW_CELL_CHUNKS = 144; // 2304 blocks
    private static final double SHADOW_CHANCE = 0.10;
    private static final int SHADOW_RADIUS_MIN = 1100;
    private static final int SHADOW_RADIUS_EXTRA = 600; // up to 1700
    private static final int SHADOW_CENTER_JITTER = 160;
    private static final int SHADOW_MIN_ORIGIN_DISTANCE = 1800;

    private static final int MIRE_CELL_CHUNKS = 24; // 384-block squares
    private static final double MIRE_CHANCE = 0.72;
    private static final long MIRE_MAIN_ISLAND_SQ = 1024L * 1024L;

    public static void setSeed(long seed) {
        SEED = seed;
    }

    public static Family familyAt(int blockX, int blockZ) {
        if (isShadowlands(blockX, blockZ)) {
            return Family.SHADOWLANDS;
        }
        if (isMirelands(blockX, blockZ)) {
            return Family.MIRELANDS;
        }
        if (isCenter(blockX, blockZ)) {
            return Family.CENTER;
        }
        return Family.VANILLA;
    }

    public static boolean isShadowlands(int blockX, int blockZ) {
        if (isCenter(blockX, blockZ)) {
            return false;
        }
        return findShadowIsland(blockX, blockZ) != null || shadowCellActive(blockX, blockZ);
    }

    public static boolean isMirelands(int blockX, int blockZ) {
        long dx = blockX;
        long dz = blockZ;
        if (dx * dx + dz * dz < MIRE_MAIN_ISLAND_SQ) {
            return false;
        }
        if (isShadowlands(blockX, blockZ)) {
            return false;
        }
        int chunkX = Math.floorDiv(blockX, 16);
        int chunkZ = Math.floorDiv(blockZ, 16);
        int cellX = Math.floorDiv(chunkX, MIRE_CELL_CHUNKS);
        int cellZ = Math.floorDiv(chunkZ, MIRE_CELL_CHUNKS);
        return hash01(0x4D4952454C414E44L, cellX, cellZ) < MIRE_CHANCE;
    }

    /**
     * Replaces vanilla End island density only inside Shadowlands continents.
     * Mirelands keep normal outer End islands.
     */
    public static double density(int blockX, int blockZ, double vanilla) {
        if (isCenter(blockX, blockZ)) {
            return vanilla;
        }

        IslandShape shadow = findShadowIsland(blockX, blockZ);
        if (shadow != null) {
            double value = shadow.density(blockX, blockZ);
            return value > -0.84 ? value : -0.84375;
        }
        if (shadowCellActive(blockX, blockZ)) {
            return -0.84375;
        }
        return vanilla;
    }

    /** Shadowlands continent containing this column, or null. */
    public static Continent shadowContinentAt(int blockX, int blockZ) {
        IslandShape shape = findShadowIsland(blockX, blockZ);
        if (shape == null) {
            return null;
        }
        return new Continent(shape.centerX, shape.centerZ, shape.radius);
    }

    /**
     * 0–1 patch field used for Shadow Claw scatter: low = sparse, high = dense.
     */
    public static double shadowClawChance(int blockX, int blockZ) {
        double wide = smoothNoise(blockX, blockZ, 64, 0x434C415700000001L);
        double local = smoothNoise(blockX, blockZ, 28, 0x434C415700000002L);
        double patch = wide * 0.72 + local * 0.28;
        double shaped = patch * patch * (3.0 - 2.0 * patch);
        return 0.07 + 0.83 * shaped;
    }

    public record Continent(int centerX, int centerZ, int radius) {
        public double[] coastPoint(double angle) {
            double dx = Math.cos(angle);
            double dz = Math.sin(angle);
            double lo = radius * 0.35;
            double hi = radius * 1.18;
            for (int i = 0; i < 20; i++) {
                double mid = (lo + hi) * 0.5;
                int x = centerX + (int) Math.round(dx * mid);
                int z = centerZ + (int) Math.round(dz * mid);
                IslandShape shape = findShadowIsland(x, z);
                boolean land = shape != null && shape.density(x, z) > -0.52;
                if (land) {
                    lo = mid;
                } else {
                    hi = mid;
                }
            }
            double r = lo;
            return new double[] { centerX + dx * r, centerZ + dz * r };
        }
    }

    private static boolean isCenter(int blockX, int blockZ) {
        long dx = blockX;
        long dz = blockZ;
        return dx * dx + dz * dz < CENTER_EXCLUSION_BLOCKS_SQ;
    }

    private static boolean shadowCellActive(int blockX, int blockZ) {
        int chunkX = Math.floorDiv(blockX, 16);
        int chunkZ = Math.floorDiv(blockZ, 16);
        int cellX = Math.floorDiv(chunkX, SHADOW_CELL_CHUNKS);
        int cellZ = Math.floorDiv(chunkZ, SHADOW_CELL_CHUNKS);
        return islandInShadowCell(cellX, cellZ) != null;
    }

    private static IslandShape findShadowIsland(int blockX, int blockZ) {
        int chunkX = Math.floorDiv(blockX, 16);
        int chunkZ = Math.floorDiv(blockZ, 16);
        int cellX = Math.floorDiv(chunkX, SHADOW_CELL_CHUNKS);
        int cellZ = Math.floorDiv(chunkZ, SHADOW_CELL_CHUNKS);
        IslandShape best = null;
        long bestDist = Long.MAX_VALUE;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                IslandShape island = islandInShadowCell(cellX + dx, cellZ + dz);
                if (island == null || !island.couldAffect(blockX, blockZ)) {
                    continue;
                }
                long dist = island.distSq(blockX, blockZ);
                if (dist < bestDist) {
                    best = island;
                    bestDist = dist;
                }
            }
        }
        return best;
    }

    private static IslandShape islandInShadowCell(int cellX, int cellZ) {
        if (hash01(0x534841444F574C44L, cellX, cellZ) >= SHADOW_CHANCE) {
            return null;
        }

        double jx = hash01(0x4A58000000000000L, cellX, cellZ);
        double jz = hash01(0x4A5A000000000000L, cellX, cellZ);
        int cellBlocks = SHADOW_CELL_CHUNKS << 4;
        int centerX = cellX * cellBlocks + cellBlocks / 2
            + (int) ((jx * 2.0 - 1.0) * SHADOW_CENTER_JITTER);
        int centerZ = cellZ * cellBlocks + cellBlocks / 2
            + (int) ((jz * 2.0 - 1.0) * SHADOW_CENTER_JITTER);

        long originDistSq = (long) centerX * (long) centerX + (long) centerZ * (long) centerZ;
        long minOriginSq = (long) SHADOW_MIN_ORIGIN_DISTANCE * (long) SHADOW_MIN_ORIGIN_DISTANCE;
        if (originDistSq < minOriginSq) {
            return null;
        }

        double sizeRoll = hash01(0x5241440000000000L, cellX, cellZ);
        int radius = SHADOW_RADIUS_MIN + (int) (sizeRoll * (SHADOW_RADIUS_EXTRA + 1));
        return new IslandShape(centerX, centerZ, radius);
    }

    private static double hash01(long salt, int x, int z) {
        long h = mix64(salt ^ SEED);
        h = mix64(h ^ (long) x * 0x9E3779B97F4A7C15L);
        h = mix64(h ^ (long) z * 0xC2B2AE3D27D4EB4FL);
        return Double.longBitsToDouble((h >>> 12) | 0x3FF0000000000000L) - 1.0;
    }

    private static long mix64(long x) {
        x ^= (x >>> 33);
        x *= 0xff51afd7ed558ccdL;
        x ^= (x >>> 33);
        x *= 0xc4ceb9fe1a85ec53L;
        x ^= (x >>> 33);
        return x;
    }

    private static double smoothNoise(int blockX, int blockZ, int grid) {
        return smoothNoise(blockX, blockZ, grid, 0x434F415354000000L);
    }

    private static double smoothNoise(int blockX, int blockZ, int grid, long salt) {
        int x0 = Math.floorDiv(blockX, grid);
        int z0 = Math.floorDiv(blockZ, grid);
        int x1 = x0 + 1;
        int z1 = z0 + 1;
        double tx = (blockX - (long) x0 * grid) / (double) grid;
        double tz = (blockZ - (long) z0 * grid) / (double) grid;
        double sx = tx * tx * (3.0 - 2.0 * tx);
        double sz = tz * tz * (3.0 - 2.0 * tz);
        double n00 = hash01(salt, x0, z0);
        double n10 = hash01(salt, x1, z0);
        double n01 = hash01(salt, x0, z1);
        double n11 = hash01(salt, x1, z1);
        double nx0 = n00 + (n10 - n00) * sx;
        double nx1 = n01 + (n11 - n01) * sx;
        return nx0 + (nx1 - nx0) * sz;
    }

    private record IslandShape(int centerX, int centerZ, int radius) {
        private static final double AFFECT_SCALE = 1.2;

        long distSq(int blockX, int blockZ) {
            long dx = (long) blockX - (long) centerX;
            long dz = (long) blockZ - (long) centerZ;
            return dx * dx + dz * dz;
        }

        boolean couldAffect(int blockX, int blockZ) {
            long max = (long) (radius * AFFECT_SCALE);
            return distSq(blockX, blockZ) <= max * max;
        }

        double density(int blockX, int blockZ) {
            double noisyRadius = radius * (0.88 + 0.18 * smoothNoise(blockX, blockZ, Math.max(32, radius / 8)));
            double dist = Math.sqrt(distSq(blockX, blockZ));
            double t = dist / Math.max(1.0, noisyRadius);
            if (t >= 1.1) {
                return Double.NEGATIVE_INFINITY;
            }

            double height;
            if (t <= 1.0) {
                double interior = 1.0 - t * t;
                height = -18.0 + 92.0 * interior;
            } else {
                double edge = (1.1 - t) / 0.1;
                height = -18.0 - 50.0 * (1.0 - edge);
            }

            double hills = (smoothNoise(blockX, blockZ, 112, 0x48494C4C53000001L) - 0.42) * 34.0;
            double ridges = (smoothNoise(blockX, blockZ, 48, 0x48494C4C53000002L) - 0.5) * 12.0;
            double bowls = Math.max(0.0, smoothNoise(blockX, blockZ, 80, 0x424F574C53000003L) - 0.74) * -46.0;
            double detail = (smoothNoise(blockX, blockZ, 22, 0x4445544149000004L) - 0.5) * 5.0;
            height += hills + ridges + bowls + detail;

            if (t < 0.88) {
                height = Math.max(-14.0, height);
            }
            height = Math.max(-100.0, Math.min(108.0, height));
            return (height - 8.0) / 128.0;
        }
    }
}
