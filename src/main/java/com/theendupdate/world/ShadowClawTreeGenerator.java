package com.theendupdate.world;

import com.theendupdate.block.EtherealSporocarpBlock;
import com.theendupdate.registry.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.WorldAccess;

/**
 * Generates a massive Shadow tree from a Shadow Claw sapling.
 * Trunk is a 3x3 column, rising high, then five thick "fingers" sprout from the crown
 * (N, S, E, W and upward).
 */
public final class ShadowClawTreeGenerator {
    private ShadowClawTreeGenerator() {}

    public static void generate(WorldAccess world, BlockPos startPos, Random random) {
        // Basic parameters — sized beyond a typical 2x2 jungle tree volume
        // Wider circular trunk (radius 3-4 → 7-9 blocks width)
        int trunkRadius = 3 + random.nextInt(2);
        int trunkHeight = 28 + random.nextInt(11); // 28-38 blocks tall
        int fingerLength = 12 + random.nextInt(9); // 12-20 blocks outwards
        int upFingerHeight = 14 + random.nextInt(9); // 14-22 blocks upwards

        // Ensure trunk space from ground level; fingers will adapt per-block
        if (!hasTrunkSpace(world, startPos, trunkRadius, trunkHeight)) {
            return; // not enough room for trunk — keep sapling
        }

        // Clear the sapling spot first
        world.setBlockState(startPos, net.minecraft.block.Blocks.AIR.getDefaultState(), 3);

        // Build trunk (filled rugged discs) from ground level, thicker and more varied near the base
        BlockPos trunkBase = startPos;
        for (int y = 0; y < trunkHeight; y++) {
            int ringRadius = computeRingRadius(trunkRadius, y, trunkHeight, random);
            placeRuggedDisc(world, trunkBase.up(y), ringRadius, random);
            // Add subtle buttress flares near the ground
            if (y <= 2) {
                placeButtressFlares(world, trunkBase.up(y), ringRadius, random);
            }
        }

        BlockPos crown = trunkBase.up(trunkHeight - 1);

        // Crown thickening as a larger rugged disc (radius = trunkRadius + 1), 2 layers
        for (int dy = 0; dy <= 1; dy++) {
            placeRuggedDisc(world, crown.up(dy), trunkRadius + 1, random);
        }

        // Five fingers: N, S, E, W, and Up
        buildUpwardBiasedFinger(world, crown, Direction.NORTH, fingerLength, random);
        buildUpwardBiasedFinger(world, crown, Direction.SOUTH, fingerLength, random);
        buildUpwardBiasedFinger(world, crown, Direction.EAST, fingerLength, random);
        buildUpwardBiasedFinger(world, crown, Direction.WEST, fingerLength, random);
        buildUpwardFinger(world, crown.up(1), upFingerHeight, random);
    }

    private static boolean hasTrunkSpace(WorldAccess world, BlockPos base, int radius, int height) {
        // Dark-oak-like permissiveness: ignore ground layer entirely and allow a small fraction
        // of obstructions above ground. This lets the tree grow around minor bumps/blocks.
        int checked = 0;
        int blocked = 0;
        for (int y = 1; y < height; y++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos p = base.add(dx, y, dz);
                    var state = world.getBlockState(p);
                    checked++;
                    if (!state.isAir() && !state.isReplaceable()) {
                        blocked++;
                    }
                }
            }
        }
        if (checked == 0) return true;
        // Allow up to ~12% blocked, but at least 8 blocks tolerance for tall trees
        int tolerance = Math.max(8, (int)Math.ceil(checked * 0.12));
        return blocked <= tolerance;
    }

    private static void buildUpwardBiasedFinger(WorldAccess world, BlockPos origin, Direction dir, int length, Random random) {
        Direction.Axis axis = dir.getAxis();
        // 3x3 thickness finger, biased upward over distance
        int yBias = 0;
        // Upward slope but never more than +1 per step to avoid detached blocks
        float slope = 0.85f + random.nextFloat() * 0.15f; // 0.85 - 1.0
        float accum = 0f;
        int blockedSteps = 0;
        for (int i = 1; i <= length; i++) {
            accum += slope;
            if (accum >= 1f) {
                yBias++;
                accum -= 1f;
            }
            BlockPos core = origin.offset(dir, i).up(yBias);
            boolean anyPlaced = false;
            if (axis == Direction.Axis.X) {
                anyPlaced |= placeFingerSlice(world, core, Direction.Axis.X);
            } else if (axis == Direction.Axis.Z) {
                anyPlaced |= placeFingerSlice(world, core, Direction.Axis.Z);
            }
            if (!anyPlaced) {
                blockedSteps++;
                if (blockedSteps >= 2) break; // stop if repeatedly blocked
            } else {
                blockedSteps = 0;
            }
            if (i % 4 == 0) {
                thickenKnuckle(world, core, axis);
            }
        }
    }

    private static boolean placeFingerSlice(WorldAccess world, BlockPos center, Direction.Axis along) {
        boolean placedAny = false;
        // Create a 3x3 cross-section perpendicular to the travel axis
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = 0; dy <= 2; dy++) {
                BlockPos p;
                if (along == Direction.Axis.X) {
                    p = center.add(0, dy, dx); // vary Z (dx) and Y; X is along-axis
                } else {
                    p = center.add(dx, dy, 0); // vary X (dx) and Y; Z is along-axis
                }
                placedAny |= placeLogIfReplaceable(world, p, along);
            }
        }
        return placedAny;
    }

    private static void buildUpwardFinger(WorldAccess world, BlockPos origin, int height, Random random) {
        int blocked = 0;
        for (int y = 0; y < height; y++) {
            BlockPos core = origin.up(y);
            boolean anyPlaced = false;
            // 3x3 vertical column slice
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    anyPlaced |= placeLogIfReplaceable(world, core.add(dx, 0, dz), Direction.Axis.Y);
                }
            }
            if (!anyPlaced) {
                blocked++;
                if (blocked >= 2) break;
            } else {
                blocked = 0;
            }
            if (y % 4 == 0) {
                thickenKnuckle(world, core, Direction.Axis.Y);
            }
        }
    }

    private static void thickenKnuckle(WorldAccess world, BlockPos center, Direction.Axis along) {
        // Add a small 3x3x3 bulge centered around the step; elongated along the axis
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos p = center.add(dx, dy, dz);
                    if (along == Direction.Axis.X && Math.abs(dz) <= 1 ||
                        along == Direction.Axis.Z && Math.abs(dx) <= 1 ||
                        along == Direction.Axis.Y && (Math.abs(dx) + Math.abs(dz) <= 2)) {
                        placeLogIfAttach(world, p, along);
                    }
                }
            }
        }
    }

    private static void placeRuggedDisc(WorldAccess world, BlockPos center, int radius, Random random) {
        int r2 = radius * radius;
        int manhattanLimit = radius * 2;
        for (int dx = -radius - 1; dx <= radius + 1; dx++) {
            for (int dz = -radius - 1; dz <= radius + 1; dz++) {
                int adx = Math.abs(dx);
                int adz = Math.abs(dz);
                boolean inCircle = dx * dx + dz * dz <= r2;
                boolean inDiamond = adx + adz <= manhattanLimit;
                boolean include = inCircle || inDiamond;
                if (include) {
                    // Occasionally push the silhouette outward one block to create a pronounced shape
                    if (!inCircle && (adx == radius + 1 || adz == radius + 1)) {
                        if (random.nextInt(3) != 0) continue; // keep some but not all outliers
                    }
                    placeLogIfReplaceable(world, center.add(dx, 0, dz), Direction.Axis.Y);
                }
            }
        }
    }

    private static int computeRingRadius(int trunkRadius, int y, int trunkHeight, Random random) {
        // Base taper: keep top slightly slimmer, bottom slightly fuller, but clamp within 3..4 (width 7..9)
        int base = trunkRadius;
        if (y < Math.min(6, trunkHeight / 6)) {
            base = Math.min(4, trunkRadius + 1); // flare near ground but stay within width 9
        } else if (y > trunkHeight * 0.7f) {
            base = Math.max(3, trunkRadius - 1); // slight taper near crown
        }
        // Occasional ring noise for organic variation (±0..1)
        if (random.nextInt(7) == 0) {
            base = Math.max(3, Math.min(4, base + (random.nextBoolean() ? 1 : -1)));
        }
        return base;
    }

    private static void placeButtressFlares(WorldAccess world, BlockPos center, int radius, Random random) {
        // Add small outward nubs around the base in cardinal directions (one block beyond the circle)
        int out = radius + 1;
        // North/South
        placeLogIfAttach(world, center.add(0, 0, -out), Direction.Axis.Y);
        placeLogIfAttach(world, center.add(0, 0, out), Direction.Axis.Y);
        // East/West
        placeLogIfAttach(world, center.add(out, 0, 0), Direction.Axis.Y);
        placeLogIfAttach(world, center.add(-out, 0, 0), Direction.Axis.Y);
        // Occasional diagonals for extra flare
        if (random.nextBoolean()) {
            placeLogIfAttach(world, center.add(out, 0, out), Direction.Axis.Y);
        }
        if (random.nextBoolean()) {
            placeLogIfAttach(world, center.add(-out, 0, out), Direction.Axis.Y);
        }
        if (random.nextBoolean()) {
            placeLogIfAttach(world, center.add(out, 0, -out), Direction.Axis.Y);
        }
        if (random.nextBoolean()) {
            placeLogIfAttach(world, center.add(-out, 0, -out), Direction.Axis.Y);
        }
    }

    private static boolean placeLogIfAttach(WorldAccess world, BlockPos pos, Direction.Axis axis) {
        BlockState state = world.getBlockState(pos);
        if (!(state.isAir() || state.isReplaceable())) return false;
        // Require adjacency to existing shadow log to avoid detached pieces
        for (Direction d : Direction.values()) {
            BlockPos n = pos.offset(d);
            if (world.getBlockState(n).isOf(ModBlocks.SHADOW_CRYPTOMYCOTA)) {
                world.setBlockState(pos, ModBlocks.SHADOW_CRYPTOMYCOTA.getDefaultState().with(EtherealSporocarpBlock.AXIS, axis), 3);
                return true;
            }
        }
        return false;
    }

    private static boolean placeLogIfReplaceable(WorldAccess world, BlockPos pos, Direction.Axis axis) {
        BlockState state = world.getBlockState(pos);
        if (state.isAir() || state.isReplaceable()) {
            world.setBlockState(pos, ModBlocks.SHADOW_CRYPTOMYCOTA.getDefaultState().with(EtherealSporocarpBlock.AXIS, axis), 3);
            return true;
        }
        return false;
    }
}


