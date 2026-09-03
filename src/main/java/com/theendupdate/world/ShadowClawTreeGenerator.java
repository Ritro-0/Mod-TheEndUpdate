package com.theendupdate.world;

import com.theendupdate.block.EtherealSporocarpBlock;
import com.theendupdate.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Generates Shadow trees from Shadow Claw saplings / worldgen.
 * Most trees are giant "hands" (flat or upright palms, 4 fingers + thumb) facing
 * a cardinal or diagonal. The original starfish crown remains as one size/shape variant.
 */
public final class ShadowClawTreeGenerator {
    private ShadowClawTreeGenerator() {}

    private static final Facing[] FACINGS = {
        new Facing(0, -1, 1, 0),   // N
        new Facing(1, -1, 1, 1),   // NE
        new Facing(1, 0, 0, 1),    // E
        new Facing(1, 1, -1, 1),   // SE
        new Facing(0, 1, -1, 0),   // S
        new Facing(-1, 1, -1, -1), // SW
        new Facing(-1, 0, 0, -1),  // W
        new Facing(-1, -1, 1, -1)  // NW
    };

    public static void generate(LevelAccessor world, BlockPos startPos, RandomSource random) {
        tryGenerate(world, startPos, random, false, startPos);
    }

    public static boolean generateForcedHollow(LevelAccessor world, BlockPos startPos, RandomSource random) {
        return generateForcedHollowWithAltarAt(world, startPos, random, startPos);
    }

    public static boolean generateForcedHollowWithAltarAt(LevelAccessor world, BlockPos startPos, RandomSource random, BlockPos altarPos) {
        return tryGenerate(world, startPos, random, true, altarPos);
    }

    private static boolean tryGenerate(LevelAccessor world, BlockPos startPos, RandomSource random, boolean forceHollow, BlockPos altarPos) {
        boolean classic = forceHollow || random.nextInt(128) == 0;
        boolean uprightPalm = !classic && random.nextInt(100) < 90;
        Facing facing = FACINGS[random.nextInt(FACINGS.length)];
        boolean thumbOnRight = random.nextBoolean();

        float scale = forceHollow ? 1.0f : pickScale(random);
        if (classic && !forceHollow) {
            scale = 0.88f + random.nextFloat() * 0.12f;
        }

        int trunkRadius = classic
            ? 3 + random.nextInt(2)
            : Math.max(2, Math.round(2.2f + 1.8f * scale) + (random.nextBoolean() ? 0 : -1));
        trunkRadius = Math.min(4, Math.max(2, trunkRadius));

        int trunkHeight = classic
            ? 28 + random.nextInt(11)
            : Math.max(10, Math.round(10f + 28f * scale) + random.nextInt(4) - 1);

        int fingerLength = classic
            ? 12 + random.nextInt(9)
            : Math.max(5, Math.round(5f + 15f * scale) + random.nextInt(3) - 1);
        int upFingerHeight = 14 + random.nextInt(9);

        if (!hasTrunkSpace(world, startPos, trunkRadius, trunkHeight)) {
            return false;
        }

        world.setBlock(startPos, Blocks.AIR.defaultBlockState(), 3);
        prepareGround(world, startPos, trunkRadius);

        boolean hollow = forceHollow || (trunkRadius >= 3 && random.nextInt(256) == 0);
        BlockPos crown = placeTrunk(world, startPos, trunkRadius, trunkHeight, hollow, classic ? null : facing, random);

        if (classic) {
            for (int dy = 0; dy <= 1; dy++) {
                placeRuggedDisc(world, crown.above(dy), trunkRadius + 1, random);
            }
            buildUpwardBiasedFinger(world, crown, Direction.NORTH, fingerLength, random);
            buildUpwardBiasedFinger(world, crown, Direction.SOUTH, fingerLength, random);
            buildUpwardBiasedFinger(world, crown, Direction.EAST, fingerLength, random);
            buildUpwardBiasedFinger(world, crown, Direction.WEST, fingerLength, random);
            buildUpwardFinger(world, crown.above(1), upFingerHeight, random);
        } else {
            buildHandCrown(world, crown, facing, thumbOnRight, uprightPalm, fingerLength, trunkRadius, scale, random);
        }

        if (hollow) {
            placeAltarChamber(world, altarPos);
            com.theendupdate.world.feature.ShadowClawScatterFeature.placeThicket(world, startPos, 20, 0.94f, random);
        }
        return true;
    }

    private static float pickScale(RandomSource random) {
        int roll = random.nextInt(100);
        if (roll < 28) {
            return 0.38f + random.nextFloat() * 0.14f;
        }
        if (roll < 62) {
            return 0.52f + random.nextFloat() * 0.18f;
        }
        if (roll < 88) {
            return 0.70f + random.nextFloat() * 0.16f;
        }
        return 0.88f + random.nextFloat() * 0.12f;
    }

    private static BlockPos placeTrunk(
        LevelAccessor world,
        BlockPos trunkBase,
        int trunkRadius,
        int trunkHeight,
        boolean hollow,
        Facing leanToward,
        RandomSource random
    ) {
        int lean = 0;
        float leanAccum = 0f;
        BlockPos lastCenter = trunkBase;
        for (int y = 0; y < trunkHeight; y++) {
            if (leanToward != null && y > trunkHeight * 0.42f) {
                leanAccum += 0.18f + random.nextFloat() * 0.16f;
                if (leanAccum >= 1f) {
                    lean++;
                    leanAccum -= 1f;
                }
            }
            BlockPos center = leanToward == null
                ? trunkBase.above(y)
                : trunkBase.offset(leanToward.fx * lean, y, leanToward.fz * lean);
            int ringRadius = computeRingRadius(trunkRadius, y, trunkHeight, random);
            if (hollow && y <= Math.max(8, trunkHeight / 3)) {
                placeHollowDisc(world, center, ringRadius, random);
            } else {
                placeRuggedDisc(world, center, ringRadius, random);
            }
            if (y <= 2) {
                placeButtressFlares(world, center, ringRadius, random);
            }
            lastCenter = center;
        }
        return lastCenter;
    }

    private static void buildHandCrown(
        LevelAccessor world,
        BlockPos wrist,
        Facing facing,
        boolean thumbOnRight,
        boolean upright,
        int fingerLength,
        int trunkRadius,
        float scale,
        RandomSource random
    ) {
        int side = thumbOnRight ? 1 : -1;
        int palmLength = Math.max(3, Math.round(3.2f + 3.6f * scale) + (upright ? random.nextInt(3) - 1 : 0));
        int palmWidth = Math.max(2, Math.round(2.4f + 2.8f * scale) + (random.nextBoolean() ? 0 : 1));
        int palmThick = upright ? (2 + random.nextInt(scale >= 0.7f ? 3 : 2)) : (scale >= 0.72f ? 3 : 2);
        int fingerThick = Math.max(1, scale >= 0.85f ? 2 : 1);
        float palmTilt = upright ? (0.02f + random.nextFloat() * 0.16f) : (0.22f + random.nextFloat() * 0.18f);
        float splay = upright ? (0.18f + random.nextFloat() * 0.62f) : 1.0f;
        float curlMul = upright ? (0.35f + random.nextFloat() * 2.4f) : 1.0f;
        float verticalBias = upright ? (0.72f + random.nextFloat() * 0.55f) : (palmTilt + 0.15f);

        placePalm(world, wrist, facing, palmLength, palmWidth, palmThick, palmTilt, upright, random);

        int inner = Math.max(1, Math.round(palmWidth * (0.28f + random.nextFloat() * 0.18f)));
        int[] spreads = { -palmWidth, -inner, inner, palmWidth };
        float[] lengths = {
            0.55f + random.nextFloat() * 0.18f,
            0.78f + random.nextFloat() * 0.14f,
            0.92f + random.nextFloat() * 0.16f,
            0.82f + random.nextFloat() * 0.14f
        };
        float[] yawOut = { -0.50f, -0.18f, 0.08f, 0.32f };
        float[] curl = upright
            ? new float[] { 0.08f * curlMul, 0.10f * curlMul, 0.07f * curlMul, 0.09f * curlMul }
            : new float[] { 0.38f, 0.34f, 0.30f, 0.32f };

        for (int i = 0; i < 4; i++) {
            BlockPos root;
            if (upright) {
                int nest = Math.max(1, palmLength / 3 + random.nextInt(2));
                int lift = Math.max(2, palmLength - 1 + (random.nextBoolean() ? 0 : 1));
                root = wrist.offset(
                    facing.fx * nest + facing.rx * spreads[i],
                    lift,
                    facing.fz * nest + facing.rz * spreads[i]
                );
            } else {
                root = offsetFacing(wrist, facing, palmLength, spreads[i], Math.round(palmLength * palmTilt));
            }
            placeBlob(world, root, fingerThick, Direction.Axis.Y);
            int len = Math.max(5, Math.round(fingerLength * lengths[i]));
            double fx = facing.fx + facing.rx * yawOut[i] * splay;
            double fz = facing.fz + facing.rz * yawOut[i] * splay;
            double fy = upright ? (verticalBias + (random.nextFloat() - 0.5f) * 0.22f) : (palmTilt + 0.15f);
            growLimb(world, root, fx, fy, fz, len, fingerThick, curl[i], random);
        }

        int thumbSpread = side * (palmWidth + 1);
        int thumbAlong = Math.max(1, palmLength / 2);
        int thumbY = upright ? Math.max(1, palmLength / 3 + random.nextInt(2)) : Math.round(thumbAlong * palmTilt);
        BlockPos thumbRoot = offsetFacing(wrist, facing, thumbAlong, thumbSpread, thumbY);
        placeBlob(world, thumbRoot, fingerThick, Direction.Axis.Y);
        int thumbLen = Math.max(5, Math.round(fingerLength * (0.48f + random.nextFloat() * 0.16f)));
        double tx = facing.rx * side * (0.70 + random.nextFloat() * 0.35) + facing.fx * (0.28 + random.nextFloat() * 0.30);
        double tz = facing.rz * side * (0.70 + random.nextFloat() * 0.35) + facing.fz * (0.28 + random.nextFloat() * 0.30);
        double ty = upright ? (0.32 + random.nextFloat() * 0.40) : 0.28;
        growLimb(world, thumbRoot, tx, ty, tz, thumbLen, fingerThick, 0.16f + random.nextFloat() * 0.22f, random);

        // thicken wrist so palm reads as attached to trunk
        placeRuggedDisc(world, wrist, Math.max(trunkRadius, palmWidth - 1), random);
        placeRuggedDisc(world, wrist.above(), Math.max(2, trunkRadius - 1), random);
    }

    private static void placePalm(
        LevelAccessor world,
        BlockPos wrist,
        Facing facing,
        int palmLength,
        int palmWidth,
        int palmThick,
        float tilt,
        boolean upright,
        RandomSource random
    ) {
        if (upright) {
            for (int h = 0; h <= palmLength; h++) {
                for (int w = -palmWidth; w <= palmWidth; w++) {
                    for (int d = 0; d < palmThick; d++) {
                        if (Math.abs(w) == palmWidth && (h == 0 || h == palmLength) && random.nextInt(3) == 0) {
                            continue;
                        }
                        BlockPos p = wrist.offset(
                            facing.fx * d + facing.rx * w,
                            h,
                            facing.fz * d + facing.rz * w
                        );
                        Direction.Axis axis = Math.abs(w) > d ? horizAxis(facing.rx, facing.rz) : Direction.Axis.Y;
                        placeLogIfReplaceable(world, p, axis);
                    }
                }
            }
            return;
        }

        for (int f = 0; f <= palmLength; f++) {
            int y = Math.round(f * tilt);
            int width = f < 2 ? Math.max(palmWidth - 1, 1) : palmWidth;
            for (int w = -width; w <= width; w++) {
                for (int t = 0; t < palmThick; t++) {
                    BlockPos p = offsetFacing(wrist, facing, f, w, y + t);
                    Direction.Axis axis = dominantAxis(facing.fx, tilt, facing.fz);
                    placeLogIfReplaceable(world, p, axis);
                }
            }
        }
    }

    private static void growLimb(
        LevelAccessor world,
        BlockPos start,
        double dx,
        double dy,
        double dz,
        int length,
        int thick,
        float curl,
        RandomSource random
    ) {
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.001) {
            return;
        }
        dx /= len;
        dy /= len;
        dz /= len;

        double x = 0;
        double y = 0;
        double z = 0;
        BlockPos last = start;
        placeBlob(world, start, Math.max(1, thick), dominantAxis(dx, dy, dz));
        for (int i = 0; i < length; i++) {
            double curlT = (i / (double) Math.max(1, length - 1)) * curl;
            dy += curlT * 0.08;
            double n = Math.sqrt(dx * dx + dy * dy + dz * dz);
            dx /= n;
            dy /= n;
            dz /= n;

            x += dx;
            y += dy;
            z += dz;
            BlockPos core = start.offset((int) Math.round(x), (int) Math.round(y), (int) Math.round(z));
            Direction.Axis axis = dominantAxis(dx, dy, dz);
            fillLimbSegment(world, last, core, Math.max(1, thick), axis);
            if (i > 0 && i % 4 == 0) {
                thickenKnuckle(world, core, axis);
            }
            last = core;
        }
    }

    private static void fillLimbSegment(LevelAccessor world, BlockPos from, BlockPos to, int thick, Direction.Axis axis) {
        int dx = to.getX() - from.getX();
        int dy = to.getY() - from.getY();
        int dz = to.getZ() - from.getZ();
        int steps = Math.max(1, Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz))));
        for (int i = 0; i <= steps; i++) {
            int x = from.getX() + Math.round(dx * (i / (float) steps));
            int y = from.getY() + Math.round(dy * (i / (float) steps));
            int z = from.getZ() + Math.round(dz * (i / (float) steps));
            placeBlob(world, new BlockPos(x, y, z), thick, axis);
        }
    }

    private static boolean placeBlob(LevelAccessor world, BlockPos center, int thick, Direction.Axis along) {
        int radius = Math.max(1, thick);
        boolean placed = placeLogIfReplaceable(world, center, along);
        for (int ox = -radius; ox <= radius; ox++) {
            for (int oy = -radius; oy <= radius; oy++) {
                for (int oz = -radius; oz <= radius; oz++) {
                    if (Math.abs(ox) + Math.abs(oy) + Math.abs(oz) > radius + 1) {
                        continue;
                    }
                    placed |= placeLogIfReplaceable(world, center.offset(ox, oy, oz), along);
                }
            }
        }
        return placed;
    }

    private static BlockPos offsetFacing(BlockPos origin, Facing facing, int forward, int right, int up) {
        return origin.offset(
            facing.fx * forward + facing.rx * right,
            up,
            facing.fz * forward + facing.rz * right
        );
    }

    private static Direction.Axis dominantAxis(double dx, double dy, double dz) {
        double ax = Math.abs(dx);
        double ay = Math.abs(dy);
        double az = Math.abs(dz);
        if (ay >= ax && ay >= az) {
            return Direction.Axis.Y;
        }
        return ax >= az ? Direction.Axis.X : Direction.Axis.Z;
    }

    private static Direction.Axis horizAxis(int rx, int rz) {
        return Math.abs(rx) >= Math.abs(rz) ? Direction.Axis.X : Direction.Axis.Z;
    }

    private static void placeAltarChamber(LevelAccessor world, BlockPos altarPos) {
        if (!(world instanceof net.minecraft.world.level.WorldGenLevel sw)) {
            return;
        }
        BlockPos murkFloorY = altarPos.below();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos floorPos = murkFloorY.offset(dx, 0, dz);
                if (!sw.getBlockState(floorPos).is(ModBlocks.END_MURK)) {
                    sw.setBlock(floorPos, ModBlocks.END_MURK.defaultBlockState(), 3);
                }
            }
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                sw.setBlock(altarPos.offset(dx, 0, dz), Blocks.AIR.defaultBlockState(), 3);
            }
        }
        sw.setBlock(altarPos, ModBlocks.SHADOW_ALTAR.defaultBlockState(), 3);

        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = 1; dy <= 8; dy++) {
                    BlockPos wallPos = altarPos.offset(dx, dy, dz);
                    BlockState wallState = sw.getBlockState(wallPos);
                    if (!wallState.isAir() && !wallState.is(ModBlocks.SHADOW_CRYPTOMYCOTA)) {
                        Direction.Axis axis = Direction.Axis.Y;
                        if (Math.abs(dx) == 2 || Math.abs(dz) == 2) {
                            axis = Math.abs(dx) > Math.abs(dz) ? Direction.Axis.X : Direction.Axis.Z;
                        }
                        sw.setBlock(wallPos, ModBlocks.SHADOW_CRYPTOMYCOTA.defaultBlockState().setValue(EtherealSporocarpBlock.AXIS, axis), 3);
                    }
                }
            }
        }
    }

    private static boolean hasTrunkSpace(LevelAccessor world, BlockPos base, int radius, int height) {
        int checked = 0;
        int blocked = 0;
        for (int y = 1; y < height; y++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos p = base.offset(dx, y, dz);
                    var state = world.getBlockState(p);
                    checked++;
                    if (!state.isAir() && !state.canBeReplaced()) {
                        blocked++;
                    }
                }
            }
        }
        if (checked == 0) {
            return true;
        }
        int tolerance = Math.max(8, (int) Math.ceil(checked * 0.12));
        return blocked <= tolerance;
    }

    private static void buildUpwardBiasedFinger(LevelAccessor world, BlockPos origin, Direction dir, int length, RandomSource random) {
        Direction.Axis axis = dir.getAxis();
        int yBias = 0;
        float slope = 0.85f + random.nextFloat() * 0.15f;
        float accum = 0f;
        int blockedSteps = 0;
        for (int i = 1; i <= length; i++) {
            accum += slope;
            if (accum >= 1f) {
                yBias++;
                accum -= 1f;
            }
            BlockPos core = origin.relative(dir, i).above(yBias);
            boolean anyPlaced = false;
            if (axis == Direction.Axis.X) {
                anyPlaced |= placeFingerSlice(world, core, Direction.Axis.X);
            } else if (axis == Direction.Axis.Z) {
                anyPlaced |= placeFingerSlice(world, core, Direction.Axis.Z);
            }
            if (!anyPlaced) {
                blockedSteps++;
                if (blockedSteps >= 2) {
                    break;
                }
            } else {
                blockedSteps = 0;
            }
            if (i % 4 == 0) {
                thickenKnuckle(world, core, axis);
            }
        }
    }

    private static boolean placeFingerSlice(LevelAccessor world, BlockPos center, Direction.Axis along) {
        boolean placedAny = false;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = 0; dy <= 2; dy++) {
                BlockPos p;
                if (along == Direction.Axis.X) {
                    p = center.offset(0, dy, dx);
                } else {
                    p = center.offset(dx, dy, 0);
                }
                placedAny |= placeLogIfReplaceable(world, p, along);
            }
        }
        return placedAny;
    }

    private static void buildUpwardFinger(LevelAccessor world, BlockPos origin, int height, RandomSource random) {
        int blocked = 0;
        for (int y = 0; y < height; y++) {
            BlockPos core = origin.above(y);
            boolean anyPlaced = false;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    anyPlaced |= placeLogIfReplaceable(world, core.offset(dx, 0, dz), Direction.Axis.Y);
                }
            }
            if (!anyPlaced) {
                blocked++;
                if (blocked >= 2) {
                    break;
                }
            } else {
                blocked = 0;
            }
            if (y % 4 == 0) {
                thickenKnuckle(world, core, Direction.Axis.Y);
            }
        }
    }

    private static void thickenKnuckle(LevelAccessor world, BlockPos center, Direction.Axis along) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos p = center.offset(dx, dy, dz);
                    if (along == Direction.Axis.X && Math.abs(dz) <= 1
                        || along == Direction.Axis.Z && Math.abs(dx) <= 1
                        || along == Direction.Axis.Y && (Math.abs(dx) + Math.abs(dz) <= 2)) {
                        placeLogIfAttach(world, p, along);
                    }
                }
            }
        }
    }

    private static void placeRuggedDisc(LevelAccessor world, BlockPos center, int radius, RandomSource random) {
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
                    if (!inCircle && (adx == radius + 1 || adz == radius + 1)) {
                        if (random.nextInt(3) != 0) {
                            continue;
                        }
                    }
                    placeLogIfReplaceable(world, center.offset(dx, 0, dz), Direction.Axis.Y);
                }
            }
        }
    }

    private static void placeHollowDisc(LevelAccessor world, BlockPos center, int radius, RandomSource random) {
        int r2 = radius * radius;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int dist2 = dx * dx + dz * dz;
                if (dist2 > r2) {
                    continue;
                }
                if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) {
                    world.setBlock(center.offset(dx, 0, dz), Blocks.AIR.defaultBlockState(), 3);
                } else {
                    placeLogIfReplaceable(world, center.offset(dx, 0, dz), Direction.Axis.Y);
                }
            }
        }
    }

    private static int computeRingRadius(int trunkRadius, int y, int trunkHeight, RandomSource random) {
        int minR = Math.max(1, trunkRadius - 1);
        int maxR = Math.min(4, trunkRadius + 1);
        int base = trunkRadius;
        if (y < Math.min(6, trunkHeight / 6)) {
            base = Math.min(maxR, trunkRadius + 1);
        } else if (y > trunkHeight * 0.7f) {
            base = Math.max(minR, trunkRadius - 1);
        }
        if (random.nextInt(7) == 0) {
            base = Math.max(minR, Math.min(maxR, base + (random.nextBoolean() ? 1 : -1)));
        }
        return base;
    }

    private static void placeButtressFlares(LevelAccessor world, BlockPos center, int radius, RandomSource random) {
        int out = radius + 1;
        placeLogIfAttach(world, center.offset(0, 0, -out), Direction.Axis.Y);
        placeLogIfAttach(world, center.offset(0, 0, out), Direction.Axis.Y);
        placeLogIfAttach(world, center.offset(out, 0, 0), Direction.Axis.Y);
        placeLogIfAttach(world, center.offset(-out, 0, 0), Direction.Axis.Y);
        if (random.nextBoolean()) {
            placeLogIfAttach(world, center.offset(out, 0, out), Direction.Axis.Y);
        }
        if (random.nextBoolean()) {
            placeLogIfAttach(world, center.offset(-out, 0, out), Direction.Axis.Y);
        }
        if (random.nextBoolean()) {
            placeLogIfAttach(world, center.offset(out, 0, -out), Direction.Axis.Y);
        }
        if (random.nextBoolean()) {
            placeLogIfAttach(world, center.offset(-out, 0, -out), Direction.Axis.Y);
        }
    }

    private static boolean placeLogIfAttach(LevelAccessor world, BlockPos pos, Direction.Axis axis) {
        BlockState state = world.getBlockState(pos);
        if (!(state.isAir() || state.canBeReplaced())) {
            return false;
        }
        for (Direction d : Direction.values()) {
            BlockPos n = pos.relative(d);
            if (world.getBlockState(n).is(ModBlocks.SHADOW_CRYPTOMYCOTA)) {
                world.setBlock(pos, ModBlocks.SHADOW_CRYPTOMYCOTA.defaultBlockState().setValue(EtherealSporocarpBlock.AXIS, axis), 3);
                return true;
            }
        }
        return false;
    }

    private static boolean placeLogIfReplaceable(LevelAccessor world, BlockPos pos, Direction.Axis axis) {
        BlockState state = world.getBlockState(pos);
        if (state.isAir() || state.canBeReplaced()) {
            world.setBlock(pos, ModBlocks.SHADOW_CRYPTOMYCOTA.defaultBlockState().setValue(EtherealSporocarpBlock.AXIS, axis), 3);
            return true;
        }
        return false;
    }

    private static void prepareGround(LevelAccessor world, BlockPos trunkBase, int trunkRadius) {
        int radius = trunkRadius + 2;
        int radiusSquared = radius * radius;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radiusSquared) {
                    continue;
                }
                BlockPos cursor = trunkBase.offset(dx, 0, dz);
                BlockState state = world.getBlockState(cursor);
                int depth = 0;
                while (depth < 3 && cursor.getY() > world.getMinY() && (state.isAir() || state.canBeReplaced())) {
                    cursor = cursor.below();
                    state = world.getBlockState(cursor);
                    depth++;
                }
                if (state.is(Blocks.END_STONE)) {
                    world.setBlock(cursor, ModBlocks.END_MURK.defaultBlockState(), 3);
                }
            }
        }
    }

    private record Facing(int fx, int fz, int rx, int rz) {}
}
