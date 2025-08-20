package com.theendupdate.world.feature;

import com.mojang.serialization.Codec;
import com.theendupdate.registry.ModBlocks;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

/**
 * Generates angled crystal spikes on exposed faces of End islands.
 * Spikes are 4-9 blocks long, taper with distance, and have an Astral Remnant base
 * transitioning to Stellarith Crystal.
 */
public class EndCrystalSpikeFeature extends Feature<DefaultFeatureConfig> {
    private static final int MAIN_ISLAND_EXCLUSION_RADIUS = 1100; // blocks

    public EndCrystalSpikeFeature(Codec<DefaultFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
        StructureWorldAccess world = context.getWorld();
        Random random = context.getRandom();
        BlockPos origin = context.getOrigin();

        // Exclude the dragon fight area (central island vicinity)
        int cx = origin.getX() + 8;
        int cz = origin.getZ() + 8;
        if (Math.sqrt((double) (cx * cx) + (double) (cz * cz)) < MAIN_ISLAND_EXCLUSION_RADIUS) {
            return false;
        }

        // ~3.75% chance per chunk to attempt a spike (rarer than before)
        if (random.nextFloat() > 0.0375f) {
            return false;
        }

        int spikesToTry = 1 + (random.nextFloat() < 0.35f ? 1 : 0);
        boolean placedAny = false;

        for (int i = 0; i < spikesToTry; i++) {
            Anchor anchor = findAnchor(world, origin, random);
            if (anchor == null) continue;

            int height = 12 + random.nextInt(11); // 12-22: noticeably longer spikes
            int baseRadius = (height >= 18 ? 3 : 2); // keep slim bases

            if (placeSpike(world, anchor, height, baseRadius, random)) {
                placedAny = true;
            }
        }

        return placedAny;
    }

    private static class Anchor {
        final BlockPos islandBlockPos; // solid island block to which we anchor
        final Direction outwardFace;    // face that is exposed to air

        Anchor(BlockPos islandBlockPos, Direction outwardFace) {
            this.islandBlockPos = islandBlockPos;
            this.outwardFace = outwardFace;
        }
    }

    private static Anchor findAnchor(StructureWorldAccess world, BlockPos origin, Random random) {
        ChunkPos chunkPos = new ChunkPos(origin);
        int startX = chunkPos.getStartX();
        int startZ = chunkPos.getStartZ();
        int bottomY = world.getBottomY();

        Direction[] faces = Direction.values();

        // Try a bounded number of random samples for performance
        for (int tries = 0; tries < 28; tries++) {
            int x = startX + random.nextInt(16);
            int z = startZ + random.nextInt(16);
            int y = bottomY + 16 + random.nextInt(Math.max(1, world.getHeight() - 32));

            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = world.getBlockState(pos);
            if (!isEndIslandBlock(state)) continue;

            List<Direction> sideOrDown = new ArrayList<>();
            List<Direction> upFaces = new ArrayList<>();

            for (Direction face : faces) {
                BlockPos outwardPos = pos.offset(face);
                BlockState outwardState = world.getBlockState(outwardPos);
                if (!outwardState.isAir()) continue; // require exposed face

                // Ensure a bit of clear space along the direction for spike base (shorter requirement)
                boolean spaceOk = true;
                for (int s = 1; s <= 2; s++) {
                    BlockPos step = pos.offset(face, s);
                    if (!world.getBlockState(step).isAir()) {
                        spaceOk = false;
                        break;
                    }
                }
                if (!spaceOk) continue;

                if (face == Direction.UP) {
                    upFaces.add(face);
                } else {
                    sideOrDown.add(face);
                }
            }

            if (!sideOrDown.isEmpty() || !upFaces.isEmpty()) {
                if (!sideOrDown.isEmpty() && (upFaces.isEmpty() || random.nextFloat() < 0.7f)) {
                    return new Anchor(pos, sideOrDown.get(random.nextInt(sideOrDown.size())));
                } else if (!upFaces.isEmpty()) {
                    return new Anchor(pos, upFaces.get(random.nextInt(upFaces.size())));
                }
            }
        }

        // Fallback: rarely try top surfaces (we prefer sides/bottom)
        if (random.nextFloat() < 0.6f) {
            for (int tries = 0; tries < 4; tries++) {
                int x = startX + random.nextInt(16);
                int z = startZ + random.nextInt(16);
                BlockPos surface = world.getTopPosition(net.minecraft.world.Heightmap.Type.WORLD_SURFACE_WG, new BlockPos(x, 0, z)).down();
                if (surface.getY() <= bottomY) continue;
                BlockState state = world.getBlockState(surface);
                if (!isEndIslandBlock(state)) continue;
                if (!world.getBlockState(surface.up()).isAir()) continue;
                return new Anchor(surface, Direction.UP);
            }
        }

        return null;
    }

    private static boolean isEndIslandBlock(BlockState state) {
        return state.isOf(Blocks.END_STONE)
            || state.isOf(ModBlocks.END_MIRE)
            || state.isOf(ModBlocks.MOLD_BLOCK);
    }

    private static boolean placeSpike(StructureWorldAccess world, Anchor anchor, int height, int baseRadius, Random random) {
        Vec3d n = Vec3d.of(anchor.outwardFace.getVector()).normalize();

        // Build an angled direction by tilting the face normal with a random vector in its tangent plane
        Vec3d tmp = Math.abs(n.y) < 0.99 ? new Vec3d(0, 1, 0) : new Vec3d(1, 0, 0);
        Vec3d u = n.crossProduct(tmp).normalize();
        Vec3d v = n.crossProduct(u).normalize();
        double tiltMag = 0.2 + random.nextDouble() * 0.45; // slightly more tilt for visual thinness
        double tiltAngle = random.nextDouble() * Math.PI * 2.0;
        Vec3d tilt = u.multiply(Math.cos(tiltAngle) * tiltMag).add(v.multiply(Math.sin(tiltAngle) * tiltMag));
        Vec3d dir = n.add(tilt).normalize();

        // Start just outside the island face
        Vec3d start = Vec3d.ofCenter(anchor.islandBlockPos).add(n.multiply(0.45));

        // Emphasize Astral Remnant at the base/support: 3-4 layers
        int baseAstralDepth = 3 + (random.nextFloat() < 0.5f ? 1 : 0);
        boolean placedAny = false;

        for (int step = 0; step < height; step++) {
            Vec3d center = start.add(dir.multiply(step));

            double t = step / (double) height;
            // Slightly thicker minimum radius to avoid vanishing tips
            double radius = Math.max(0.30, baseRadius * (1.0 - t * 1.45));
            radius += (random.nextDouble() - 0.5) * 0.08;

            int r = (int) Math.ceil(radius + 0.5);

            boolean placedInThisStep = false;
            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -r; dy <= r; dy++) {
                    for (int dz = -r; dz <= r; dz++) {
                        BlockPos bp = BlockPos.ofFloored(center.x + dx, center.y + dy, center.z + dz);

                        // Distance from the spike axis (perpendicular component)
                        Vec3d cellCenter = new Vec3d(bp.getX() + 0.5, bp.getY() + 0.5, bp.getZ() + 0.5);
                        Vec3d delta = cellCenter.subtract(center);
                        double axial = delta.dotProduct(dir);
                        if (axial < -0.5 || axial > 0.5) continue; // slightly thicker shell for continuity
                        double perpSq = delta.lengthSquared() - axial * axial;
                        if (perpSq > (radius * radius)) continue;

                        BlockState existing = world.getBlockState(bp);
                        if (!(existing.isAir() || existing.isReplaceable())) continue;

                        BlockState placeState = (step < baseAstralDepth)
                            ? ModBlocks.ASTRAL_REMNANT.getDefaultState()
                            : ModBlocks.STELLARITH_CRYSTAL.getDefaultState();

                        world.setBlockState(bp, placeState, Block.NOTIFY_LISTENERS);
                        placedAny = true;
                        placedInThisStep = true;
                    }
                }
            }

            // Guarantee a connected spine so tips never float
            if (!placedInThisStep) {
                BlockPos core = BlockPos.ofFloored(center);
                BlockState existing = world.getBlockState(core);
                if (existing.isAir() || existing.isReplaceable()) {
                    BlockState placeStateStep = (step < baseAstralDepth)
                        ? ModBlocks.ASTRAL_REMNANT.getDefaultState()
                        : ModBlocks.STELLARITH_CRYSTAL.getDefaultState();
                    world.setBlockState(core, placeStateStep, Block.NOTIFY_LISTENERS);
                    placedAny = true;
                }
            }
        }

        // Thicken and hug the base at the island face to feel "firmly" rooted
        if (placedAny) {
            thickenBase(world, anchor, random);
            if (random.nextFloat() < 0.7f) {
                hugIslandFaceWithAstral(world, anchor, random);
            }
        }

        return placedAny;
    }

    private static void thickenBase(StructureWorldAccess world, Anchor anchor, Random random) {
        Vec3d n = Vec3d.of(anchor.outwardFace.getVector()).normalize();
        Vec3d baseCenter = Vec3d.ofCenter(anchor.islandBlockPos).add(n.multiply(0.75));
        int baseR = random.nextFloat() < 0.4f ? 2 : 1; // mostly 1, sometimes 2 for a stronger base
        for (int dx = -baseR; dx <= baseR; dx++) {
            for (int dy = -baseR; dy <= baseR; dy++) {
                for (int dz = -baseR; dz <= baseR; dz++) {
                    BlockPos bp = BlockPos.ofFloored(baseCenter.x + dx, baseCenter.y + dy, baseCenter.z + dz);
                    double distSq = (dx * dx) + (dy * dy) + (dz * dz);
                    if (distSq > (baseR + 0.25) * (baseR + 0.25)) continue;
                    BlockState existing = world.getBlockState(bp);
                    if (existing.isAir() || existing.isReplaceable()) {
                        world.setBlockState(bp, ModBlocks.ASTRAL_REMNANT.getDefaultState(), Block.NOTIFY_LISTENERS);
                    }
                }
            }
        }
    }

    private static void hugIslandFaceWithAstral(StructureWorldAccess world, Anchor anchor, Random random) {
        Direction out = anchor.outwardFace;
        // Tangent plane axes relative to the outward face
        Direction uDir;
        Direction vDir;
        if (out.getAxis() == Direction.Axis.Y) { // UP/DOWN -> horizontal plane
            uDir = Direction.EAST;
            vDir = Direction.NORTH;
        } else if (out.getAxis() == Direction.Axis.X) { // EAST/WEST -> YZ plane
            uDir = Direction.UP;
            vDir = Direction.NORTH;
        } else { // Z axis (NORTH/SOUTH) -> XY plane
            uDir = Direction.UP;
            vDir = Direction.EAST;
        }

        BlockPos faceCenter = anchor.islandBlockPos.offset(out); // first air cell outside the island
        int ringR = 1 + (random.nextFloat() < 0.25f ? 1 : 0); // 1, sometimes 2 to emphasize base connection
        for (int i = -ringR; i <= ringR; i++) {
            for (int j = -ringR; j <= ringR; j++) {
                if (i == 0 && j == 0) continue;
                // Prefer a disk on the tangent plane
                if ((i * i) + (j * j) > (ringR + 0.25) * (ringR + 0.25)) continue;
                BlockPos place = faceCenter.offset(uDir, i).offset(vDir, j);

                // Only place if this astral would visually connect to the island surface
                BlockPos behind = place.offset(out.getOpposite());
                BlockState behindState = world.getBlockState(behind);
                boolean touchesIsland = isEndIslandBlock(behindState)
                    || isEndIslandBlock(world.getBlockState(behind.offset(uDir)))
                    || isEndIslandBlock(world.getBlockState(behind.offset(vDir)))
                    || isEndIslandBlock(world.getBlockState(behind.offset(uDir.getOpposite())))
                    || isEndIslandBlock(world.getBlockState(behind.offset(vDir.getOpposite())));

                if (!touchesIsland) continue;

                // Place 1-2 layers thick towards outward face to make a more confident cuff
                int layers = (random.nextFloat() < 0.5f ? 2 : 1);
                for (int t = 0; t < layers; t++) {
                    BlockPos cuff = place.offset(out, t);
                    BlockState existing = world.getBlockState(cuff);
                    if (existing.isAir() || existing.isReplaceable()) {
                        world.setBlockState(cuff, ModBlocks.ASTRAL_REMNANT.getDefaultState(), Block.NOTIFY_LISTENERS);
                    }
                }
            }
        }
    }
}


