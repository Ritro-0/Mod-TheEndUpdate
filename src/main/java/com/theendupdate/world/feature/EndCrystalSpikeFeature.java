package com.theendupdate.world.feature;

import com.mojang.serialization.Codec;
import com.theendupdate.block.StellarithCrystalBlock;
import com.theendupdate.registry.ModBlocks;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.phys.Vec3;

/**
 * Generates angled crystal spikes on exposed faces of End islands.
 * Spikes are 4-9 blocks long, taper with distance, and have an Astral Remnant base
 * transitioning to Stellarith Crystal.
 */
public class EndCrystalSpikeFeature extends Feature<NoneFeatureConfiguration> {
    private static final int MAIN_ISLAND_EXCLUSION_RADIUS = 1100; // blocks
    private static final boolean DEBUG_SHULKER_SPAWNS = false; // logs spike anchor placements when true

    public EndCrystalSpikeFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel world = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();

        // spikes are global, no mask suppression here

        // exclude the dragon fight area (central island vicinity)
        int cx = origin.getX() + 8;
        int cz = origin.getZ() + 8;
        if (Math.sqrt((double) (cx * cx) + (double) (cz * cz)) < MAIN_ISLAND_EXCLUSION_RADIUS) {
            return false;
        }

        // ~3.75% chance per chunk, restores original rarity
        if (random.nextFloat() > 0.0375f) {
            return false;
        }

        int spikesToTry = 1 + (random.nextFloat() < 0.35f ? 1 : 0);
        boolean placedAny = false;

        for (int i = 0; i < spikesToTry; i++) {
            Anchor anchor = findAnchor(world, origin, random);
            if (anchor == null) continue;

			// skip generation inside Shadowlands biomes
			net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> biomeEntry = world.getBiome(anchor.islandBlockPos);
			java.util.Optional<net.minecraft.resources.ResourceKey<net.minecraft.world.level.biome.Biome>> biomeKeyOpt = biomeEntry.unwrapKey();
			if (biomeKeyOpt.isPresent()) {
				String bpath = biomeKeyOpt.get().identifier().getPath();
				if ("shadowlands_highlands".equals(bpath) || "shadowlands_midlands".equals(bpath) || "shadowlands_barrens".equals(bpath)) {
					continue;
				}
			}

            if (DEBUG_SHULKER_SPAWNS) {
                System.out.println("[EndUpdate] Spike anchor at " + anchor.islandBlockPos + " facing " + anchor.outwardFace);
            }

            int height = 12 + random.nextInt(11); // 12-22, noticeably longer spikes
            int baseRadius = (height >= 18 ? 3 : 2); // keep bases slim

            java.util.ArrayList<BlockPos> placedBlocks = new java.util.ArrayList<>();
            if (placeSpike(world, anchor, height, baseRadius, random, placedBlocks)) {
                placedAny = true;
            }
        }

        return placedAny;
    }

    // orb spawning on natural crystal tips is handled separately by EtherealOrbOnCrystalsSpawner on chunk load

    private static class Anchor {
        final BlockPos islandBlockPos; // solid island block to which we anchor
        final Direction outwardFace;    // face that is exposed to air

        Anchor(BlockPos islandBlockPos, Direction outwardFace) {
            this.islandBlockPos = islandBlockPos;
            this.outwardFace = outwardFace;
        }
    }

    private static Anchor findAnchor(WorldGenLevel world, BlockPos origin, RandomSource random) {
        ChunkPos chunkPos = ChunkPos.containing(origin);
        int startX = chunkPos.getMinBlockX();
        int startZ = chunkPos.getMinBlockZ();
        int bottomY = world.getMinY();

        Direction[] faces = Direction.values();

        // bounded random samples, keeps this cheap
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
                BlockPos outwardPos = pos.relative(face);
                BlockState outwardState = world.getBlockState(outwardPos);
                if (!outwardState.isAir()) continue; // require exposed face

                // need a little clear space along the direction for the spike base
                boolean spaceOk = true;
                for (int s = 1; s <= 2; s++) {
                    BlockPos step = pos.relative(face, s);
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

        // fallback: rarely try top surfaces, sides/bottom are preferred
        if (random.nextFloat() < 0.6f) {
            for (int tries = 0; tries < 4; tries++) {
                int x = startX + random.nextInt(16);
                int z = startZ + random.nextInt(16);
                BlockPos surface = world.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG, new BlockPos(x, 0, z)).below();
                if (surface.getY() <= bottomY) continue;
                BlockState state = world.getBlockState(surface);
                if (!isEndIslandBlock(state)) continue;
                if (!world.getBlockState(surface.above()).isAir()) continue;
                return new Anchor(surface, Direction.UP);
            }
        }

        return null;
    }

    private static boolean isEndIslandBlock(BlockState state) {
        return state.is(Blocks.END_STONE)
            || state.is(ModBlocks.END_MIRE)
            || state.is(ModBlocks.MOLD_BLOCK);
    }

    private static boolean placeSpike(WorldGenLevel world, Anchor anchor, int height, int baseRadius, RandomSource random, java.util.List<BlockPos> placedOut) {
        Vec3 n = Vec3.atLowerCornerOf(anchor.outwardFace.getUnitVec3i()).normalize();

        // tilt the face normal by a random vector in its tangent plane for an angled spike
        Vec3 tmp = Math.abs(n.y) < 0.99 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        Vec3 u = n.cross(tmp).normalize();
        Vec3 v = n.cross(u).normalize();
        double tiltMag = 0.2 + random.nextDouble() * 0.45; // a bit more tilt for visual thinness
        double tiltAngle = random.nextDouble() * Math.PI * 2.0;
        Vec3 tilt = u.scale(Math.cos(tiltAngle) * tiltMag).add(v.scale(Math.sin(tiltAngle) * tiltMag));
        Vec3 dir = n.add(tilt).normalize();

        Vec3 start = Vec3.atCenterOf(anchor.islandBlockPos).add(n.scale(0.45));

        int baseAstralDepth = 3 + (random.nextFloat() < 0.5f ? 1 : 0); // 3-4 layers of Astral Remnant at the base
        boolean placedAny = false;

        for (int step = 0; step < height; step++) {
            Vec3 center = start.add(dir.scale(step));

            double t = step / (double) height;
            double radius = Math.max(0.30, baseRadius * (1.0 - t * 1.45)); // floor avoids vanishing tips
            radius += (random.nextDouble() - 0.5) * 0.08;

            int r = (int) Math.ceil(radius + 0.5);

            boolean placedInThisStep = false;
            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -r; dy <= r; dy++) {
                    for (int dz = -r; dz <= r; dz++) {
                        BlockPos bp = BlockPos.containing(center.x + dx, center.y + dy, center.z + dz);
                        // clip to same chunk, the engine rejects far-chunk writes here
                        ChunkPos anchorChunk = ChunkPos.containing(anchor.islandBlockPos);
                        if (ChunkPos.containing(bp).x() != anchorChunk.x() || ChunkPos.containing(bp).z() != anchorChunk.z()) {
                            continue;
                        }

                        // perpendicular distance from the spike axis
                        Vec3 cellCenter = new Vec3(bp.getX() + 0.5, bp.getY() + 0.5, bp.getZ() + 0.5);
                        Vec3 delta = cellCenter.subtract(center);
                        double axial = delta.dot(dir);
                        if (axial < -0.5 || axial > 0.5) continue; // thicker shell for continuity
                        double perpSq = delta.lengthSqr() - axial * axial;
                        if (perpSq > (radius * radius)) continue;

                        BlockState existing = world.getBlockState(bp);
                        if (!(existing.isAir() || existing.canBeReplaced())) continue;

                        if (step < baseAstralDepth) {
                            world.setBlock(bp, ModBlocks.ASTRAL_REMNANT.defaultBlockState(), Block.UPDATE_CLIENTS);
                        } else {
                            world.setBlock(bp, ModBlocks.STELLARITH_CRYSTAL.defaultBlockState().setValue(StellarithCrystalBlock.NATURAL, Boolean.TRUE), Block.UPDATE_CLIENTS);
                        }
                        if (placedOut != null) placedOut.add(bp.immutable());
                        placedAny = true;
                        placedInThisStep = true;
                    }
                }
            }

            // guarantee a connected spine so tips never float
            if (!placedInThisStep) {
                BlockPos core = BlockPos.containing(center);
                BlockState existing = world.getBlockState(core);
                if (existing.isAir() || existing.canBeReplaced()) {
                    if (step < baseAstralDepth) {
                        world.setBlock(core, ModBlocks.ASTRAL_REMNANT.defaultBlockState(), Block.UPDATE_CLIENTS);
                    } else {
                        world.setBlock(core, ModBlocks.STELLARITH_CRYSTAL.defaultBlockState().setValue(StellarithCrystalBlock.NATURAL, Boolean.TRUE), Block.UPDATE_CLIENTS);
                    }
                    if (placedOut != null) placedOut.add(core.immutable());
                    placedAny = true;
                }
            }
        }

        // thicken and hug the base at the island face so it reads as firmly rooted
        if (placedAny) {
            thickenBase(world, anchor, random);
            if (random.nextFloat() < 0.7f) {
                hugIslandFaceWithAstral(world, anchor, random);
            }
        }

        return placedAny;
    }

    private static void thickenBase(WorldGenLevel world, Anchor anchor, RandomSource random) {
        Vec3 n = Vec3.atLowerCornerOf(anchor.outwardFace.getUnitVec3i()).normalize();
        Vec3 baseCenter = Vec3.atCenterOf(anchor.islandBlockPos).add(n.scale(0.75));
        int baseR = random.nextFloat() < 0.4f ? 2 : 1; // mostly 1, sometimes 2 for a stronger base
        for (int dx = -baseR; dx <= baseR; dx++) {
            for (int dy = -baseR; dy <= baseR; dy++) {
                for (int dz = -baseR; dz <= baseR; dz++) {
                    BlockPos bp = BlockPos.containing(baseCenter.x + dx, baseCenter.y + dy, baseCenter.z + dz);
                    double distSq = (dx * dx) + (dy * dy) + (dz * dz);
                    if (distSq > (baseR + 0.25) * (baseR + 0.25)) continue;
                    BlockState existing = world.getBlockState(bp);
                    if (existing.isAir() || existing.canBeReplaced()) {
                        world.setBlock(bp, ModBlocks.ASTRAL_REMNANT.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
    }

    private static void hugIslandFaceWithAstral(WorldGenLevel world, Anchor anchor, RandomSource random) {
        Direction out = anchor.outwardFace;
        // tangent plane axes relative to the outward face
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

        BlockPos faceCenter = anchor.islandBlockPos.relative(out); // first air cell outside the island
        int ringR = 1 + (random.nextFloat() < 0.25f ? 1 : 0); // 1, sometimes 2 to emphasize base connection
        for (int i = -ringR; i <= ringR; i++) {
            for (int j = -ringR; j <= ringR; j++) {
                if (i == 0 && j == 0) continue;
                if ((i * i) + (j * j) > (ringR + 0.25) * (ringR + 0.25)) continue; // disk on the tangent plane
                BlockPos place = faceCenter.relative(uDir, i).relative(vDir, j);

                // only place if it would visually connect to the island surface
                BlockPos behind = place.relative(out.getOpposite());
                BlockState behindState = world.getBlockState(behind);
                boolean touchesIsland = isEndIslandBlock(behindState)
                    || isEndIslandBlock(world.getBlockState(behind.relative(uDir)))
                    || isEndIslandBlock(world.getBlockState(behind.relative(vDir)))
                    || isEndIslandBlock(world.getBlockState(behind.relative(uDir.getOpposite())))
                    || isEndIslandBlock(world.getBlockState(behind.relative(vDir.getOpposite())));

                if (!touchesIsland) continue;

                int layers = (random.nextFloat() < 0.5f ? 2 : 1); // 1-2 layers toward outward face, a more confident cuff
                for (int t = 0; t < layers; t++) {
                    BlockPos cuff = place.relative(out, t);
                    BlockState existing = world.getBlockState(cuff);
                    if (existing.isAir() || existing.canBeReplaced()) {
                        world.setBlock(cuff, ModBlocks.ASTRAL_REMNANT.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
    }
}


