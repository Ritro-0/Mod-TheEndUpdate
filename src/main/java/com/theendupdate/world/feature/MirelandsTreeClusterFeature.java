package com.theendupdate.world.feature;

import com.mojang.serialization.Codec;
import com.theendupdate.registry.ModBlocks;
import com.theendupdate.registry.ModWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import com.theendupdate.block.MoldcrawlBlock;

/**
 * Generates small clusters of Ethereal Tendril trees across Mirelands islands.
 * Trees are spaced to avoid overlap but appear in localized patches.
 */
public class MirelandsTreeClusterFeature extends Feature<NoneFeatureConfiguration> {
    public MirelandsTreeClusterFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel world = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();

        ChunkPos chunkPos = ChunkPos.containing(origin);
        int startX = chunkPos.getMinBlockX();
        int startZ = chunkPos.getMinBlockZ();

        // low chance per chunk, clusters should stay rare
        if (random.nextFloat() > 0.12f) {
            return false;
        }

        int clusters = 2 + (random.nextFloat() < 0.35f ? 1 : 0); // 2-3, ~2x previous density

        boolean anyPlaced = false;
        for (int c = 0; c < clusters; c++) {
            int centerX = startX + random.nextInt(16);
            int centerZ = startZ + random.nextInt(16);
            BlockPos centerSurface = topSurface(world, new BlockPos(centerX, 0, centerZ));
            if (centerSurface == null) continue;

            if (!isMirelandsBiome(world, centerSurface)) continue;

            int trees = 4 + random.nextInt(3); // 4-6, slightly larger clusters
            int radius = 8 + random.nextInt(6); // radius 8-13
            int minSpacing = 5; // keep trunks apart

            java.util.List<BlockPos> placedTrunks = new java.util.ArrayList<>();
            for (int i = 0; i < trees; i++) {
                // polar offset within radius
                double theta = random.nextDouble() * Math.PI * 2.0;
                double r = 2.0 + random.nextDouble() * (radius - 2.0);
                int px = centerSurface.getX() + (int)Math.round(Math.cos(theta) * r);
                int pz = centerSurface.getZ() + (int)Math.round(Math.sin(theta) * r);
                BlockPos candidateSurface = topSurface(world, new BlockPos(px, 0, pz));
                if (candidateSurface == null) continue;
                if (!isMirelandsBiome(world, candidateSurface)) continue;
                // end stone allowed too, in case feature ordering differs
                BlockState ground = world.getBlockState(candidateSurface);
                if (!(ground.is(ModBlocks.END_MIRE) || ground.is(ModBlocks.MOLD_BLOCK) || ground.is(net.minecraft.world.level.block.Blocks.END_STONE))) {
                    continue;
                }

                BlockPos trunkPos = candidateSurface.above();
                if (!world.isEmptyBlock(trunkPos)) continue;

                boolean tooClose = false;
                for (BlockPos other : placedTrunks) {
                    if (other.distManhattan(trunkPos) < minSpacing) {
                        tooClose = true;
                        break;
                    }
                }
                if (tooClose) continue;

                if (generateSingleTree(world, trunkPos, random)) {
                    placedTrunks.add(trunkPos);
                    anyPlaced = true;
                }
            }
        }

        return anyPlaced;
    }

    private static BlockPos topSurface(WorldGenLevel world, BlockPos colBase) {
        BlockPos pos = world.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, colBase).below();
        if (pos.getY() <= world.getMinY()) return null;
        return world.isEmptyBlock(pos.above()) ? pos : null; // must be exposed
    }

    private static boolean isMirelandsBiome(WorldGenLevel world, BlockPos pos) {
        var biome = world.getBiome(pos);
        return biome.is(ModWorldgen.MIRELANDS_HIGHLANDS_KEY)
            || biome.is(ModWorldgen.MIRELANDS_MIDLANDS_KEY)
            || biome.is(ModWorldgen.MIRELANDS_BARRENS_KEY);
    }

    private static boolean generateSingleTree(WorldGenLevel world, BlockPos trunkBase, RandomSource random) {
        // 5x5x8 clearance check
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                for (int y = 0; y <= 7; y++) {
                    BlockPos check = trunkBase.offset(x, y, z);
                    var state = world.getBlockState(check);
                    if (!state.isAir() && !state.canBeReplaced()) {
                        return false;
                    }
                }
            }
        }

        java.util.List<BlockPos> placedLogs = new java.util.ArrayList<>();
        int trunkHeight = 3 + random.nextInt(4); // 3-6 tall
        for (int y = 0; y < trunkHeight; y++) {
            BlockPos pos = trunkBase.above(y);
            world.setBlock(pos, ModBlocks.ETHEREAL_SPOROCARP.defaultBlockState()
                .setValue(com.theendupdate.block.EtherealSporocarpBlock.AXIS, Direction.Axis.Y), Block.UPDATE_CLIENTS);
            placedLogs.add(pos);
        }

        int branchCount = 2 + random.nextInt(3); // 2-4
        int startH = Math.max(1, trunkHeight / 2);
        Direction[] horiz = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
        for (int i = 0; i < branchCount; i++) {
            int by = startH + random.nextInt(Math.max(1, trunkHeight - startH));
            Direction dir = horiz[random.nextInt(horiz.length)];
            int len = 1 + random.nextInt(3);
            for (int step = 1; step <= len; step++) {
                BlockPos bpos = trunkBase.above(by).relative(dir, step);
                var state = world.getBlockState(bpos);
                if (state.isAir() || state.canBeReplaced()) {
                    world.setBlock(bpos, ModBlocks.ETHEREAL_SPOROCARP.defaultBlockState()
                        .setValue(com.theendupdate.block.EtherealSporocarpBlock.AXIS, dir.getAxis()), Block.UPDATE_CLIENTS);
                    placedLogs.add(bpos);
                }
            }
        }

        int tendrils = 1 + random.nextInt(3);
        for (int i = 0; i < tendrils; i++) {
            int ty = Math.max(1, trunkHeight - 2) + random.nextInt(2);
            Direction dir = horiz[random.nextInt(horiz.length)];
            BlockPos tpos = trunkBase.above(ty).relative(dir);
            var state = world.getBlockState(tpos);
            if (state.isAir() || state.canBeReplaced()) {
                if (random.nextBoolean()) {
                    world.setBlock(tpos, ModBlocks.ETHEREAL_SPOROCARP.defaultBlockState()
                        .setValue(com.theendupdate.block.EtherealSporocarpBlock.AXIS, dir.getAxis()), Block.UPDATE_CLIENTS);
                } else {
                    BlockPos vpos = random.nextBoolean() ? tpos.below() : tpos.above();
                    var vstate = world.getBlockState(vpos);
                    if (vstate.isAir() || vstate.canBeReplaced()) {
                        world.setBlock(vpos, ModBlocks.ETHEREAL_SPOROCARP.defaultBlockState()
                            .setValue(com.theendupdate.block.EtherealSporocarpBlock.AXIS, Direction.Axis.Y), Block.UPDATE_CLIENTS);
                    }
                }
            }
        }

        // ~1.2% chance per face to grow mold crawl on adjacent faces
        for (BlockPos logPos : placedLogs) {
            attemptMoldCrawlPlacements(world, logPos, random);
        }

        return true;
    }

    private static void attemptMoldCrawlPlacements(WorldGenLevel world, BlockPos logPos, RandomSource random) {
        Direction[] horizontal = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
        for (Direction outward : horizontal) {
            BlockPos moldPos = logPos.relative(outward);
            if (!world.getBlockState(moldPos).isAir()) {
                continue;
            }
            if (random.nextFloat() >= 0.012f) {
                continue;
            }
            BlockState mold = ModBlocks.MOLD_CRAWL.defaultBlockState().setValue(MoldcrawlBlock.FACING, outward);
            if (mold.canSurvive(world, moldPos)) {
                world.setBlock(moldPos, mold, Block.UPDATE_CLIENTS);
            }
        }
    }
}
