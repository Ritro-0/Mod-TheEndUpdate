package com.theendupdate.world.feature;

import com.mojang.serialization.Codec;
import com.theendupdate.registry.ModBlocks;
import com.theendupdate.registry.ModWorldgen;
import com.theendupdate.block.MoldcrawlBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

/**
 * Generates small clusters of Ethereal Tendril trees across Mirelands islands.
 * Trees are spaced to avoid overlap but appear in localized patches.
 */
public class MirelandsTreeClusterFeature extends Feature<DefaultFeatureConfig> {
    public MirelandsTreeClusterFeature(Codec<DefaultFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
        StructureWorldAccess world = context.getWorld();
        BlockPos origin = context.getOrigin();
        Random random = context.getRandom();

        ChunkPos chunkPos = new ChunkPos(origin);
        int startX = chunkPos.getStartX();
        int startZ = chunkPos.getStartZ();

        // Low chance per chunk to attempt a cluster (clusters should be rare)
        if (random.nextFloat() > 0.12f) {
            return false;
        }

        // Pick 2-3 clusters in this chunk (roughly ~2x previous)
        int clusters = 2 + (random.nextFloat() < 0.35f ? 1 : 0);

        boolean anyPlaced = false;
        for (int c = 0; c < clusters; c++) {
            int centerX = startX + random.nextInt(16);
            int centerZ = startZ + random.nextInt(16);
            BlockPos centerSurface = topSurface(world, new BlockPos(centerX, 0, centerZ));
            if (centerSurface == null) continue;

            // Validate biome (must be our Mirelands family)
            if (!isMirelandsBiome(world, centerSurface)) continue;

            // How many trees in this cluster
            int trees = 4 + random.nextInt(3); // 4-6 (slightly larger clusters)
            int radius = 8 + random.nextInt(6); // radius 8-13
            int minSpacing = 5; // keep trunks apart

            java.util.List<BlockPos> placedTrunks = new java.util.ArrayList<>();
            for (int i = 0; i < trees; i++) {
                // Polar offset within radius
                double theta = random.nextDouble() * Math.PI * 2.0;
                double r = 2.0 + random.nextDouble() * (radius - 2.0);
                int px = centerSurface.getX() + (int)Math.round(Math.cos(theta) * r);
                int pz = centerSurface.getZ() + (int)Math.round(Math.sin(theta) * r);
                BlockPos candidateSurface = topSurface(world, new BlockPos(px, 0, pz));
                if (candidateSurface == null) continue;
                if (!isMirelandsBiome(world, candidateSurface)) continue;
                // Ensure ground is acceptable (we allow end stone too in case ordering differs)
                BlockState ground = world.getBlockState(candidateSurface);
                if (!(ground.isOf(ModBlocks.END_MIRE) || ground.isOf(ModBlocks.MOLD_BLOCK) || ground.isOf(net.minecraft.block.Blocks.END_STONE))) {
                    continue;
                }

                BlockPos trunkPos = candidateSurface.up();
                if (!world.isAir(trunkPos)) continue;

                // Spacing check vs already placed trunks in this cluster
                boolean tooClose = false;
                for (BlockPos other : placedTrunks) {
                    if (other.getManhattanDistance(trunkPos) < minSpacing) {
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

    private static BlockPos topSurface(StructureWorldAccess world, BlockPos colBase) {
        BlockPos pos = world.getTopPosition(Heightmap.Type.WORLD_SURFACE_WG, colBase).down();
        if (pos.getY() <= world.getBottomY()) return null;
        // Must be exposed
        return world.isAir(pos.up()) ? pos : null;
    }

    private static boolean isMirelandsBiome(StructureWorldAccess world, BlockPos pos) {
        var biome = world.getBiome(pos);
        return biome.matchesKey(ModWorldgen.MIRELANDS_HIGHLANDS_KEY)
            || biome.matchesKey(ModWorldgen.MIRELANDS_MIDLANDS_KEY)
            || biome.matchesKey(ModWorldgen.MIRELANDS_BARRENS_KEY);
    }

    private static boolean generateSingleTree(StructureWorldAccess world, BlockPos trunkBase, Random random) {
        // Basic space check 5x5x8
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                for (int y = 0; y <= 7; y++) {
                    BlockPos check = trunkBase.add(x, y, z);
                    var state = world.getBlockState(check);
                    if (!state.isAir() && !state.isReplaceable()) {
                        return false;
                    }
                }
            }
        }

        java.util.List<BlockPos> placedLogs = new java.util.ArrayList<>();
        // Trunk 3-6 tall
        int trunkHeight = 3 + random.nextInt(4);
        for (int y = 0; y < trunkHeight; y++) {
            BlockPos pos = trunkBase.up(y);
            world.setBlockState(pos, ModBlocks.ETHEREAL_SPOROCARP.getDefaultState()
                .with(com.theendupdate.block.EtherealSporocarpBlock.AXIS, Direction.Axis.Y), Block.NOTIFY_LISTENERS);
            placedLogs.add(pos);
        }

        // Branches 2-4
        int branchCount = 2 + random.nextInt(3);
        int startH = Math.max(1, trunkHeight / 2);
        Direction[] horiz = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
        for (int i = 0; i < branchCount; i++) {
            int by = startH + random.nextInt(Math.max(1, trunkHeight - startH));
            Direction dir = horiz[random.nextInt(horiz.length)];
            int len = 1 + random.nextInt(3);
            for (int step = 1; step <= len; step++) {
                BlockPos bpos = trunkBase.up(by).offset(dir, step);
                var state = world.getBlockState(bpos);
                if (state.isAir() || state.isReplaceable()) {
                    world.setBlockState(bpos, ModBlocks.ETHEREAL_SPOROCARP.getDefaultState()
                        .with(com.theendupdate.block.EtherealSporocarpBlock.AXIS, dir.getAxis()), Block.NOTIFY_LISTENERS);
                    placedLogs.add(bpos);
                }
            }
        }

        // Occasional tendrils
        int tendrils = 1 + random.nextInt(3);
        for (int i = 0; i < tendrils; i++) {
            int ty = Math.max(1, trunkHeight - 2) + random.nextInt(2);
            Direction dir = horiz[random.nextInt(horiz.length)];
            BlockPos tpos = trunkBase.up(ty).offset(dir);
            var state = world.getBlockState(tpos);
            if (state.isAir() || state.isReplaceable()) {
                if (random.nextBoolean()) {
                    world.setBlockState(tpos, ModBlocks.ETHEREAL_SPOROCARP.getDefaultState()
                        .with(com.theendupdate.block.EtherealSporocarpBlock.AXIS, dir.getAxis()), Block.NOTIFY_LISTENERS);
                } else {
                    BlockPos vpos = random.nextBoolean() ? tpos.down() : tpos.up();
                    var vstate = world.getBlockState(vpos);
                    if (vstate.isAir() || vstate.isReplaceable()) {
                        world.setBlockState(vpos, ModBlocks.ETHEREAL_SPOROCARP.getDefaultState()
                            .with(com.theendupdate.block.EtherealSporocarpBlock.AXIS, Direction.Axis.Y), Block.NOTIFY_LISTENERS);
                    }
                }
            }
        }

        // After placing logs, optionally place mold crawl on adjacent faces (~1.2% chance per face)
        for (BlockPos logPos : placedLogs) {
            attemptMoldCrawlPlacements(world, logPos, random);
        }

        return true;
    }

    private static void attemptMoldCrawlPlacements(StructureWorldAccess world, BlockPos logPos, Random random) {
        Direction[] horizontal = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
        for (Direction outward : horizontal) {
            BlockPos moldPos = logPos.offset(outward);
            if (!world.getBlockState(moldPos).isAir()) {
                continue;
            }
            if (random.nextFloat() >= 0.012f) {
                continue;
            }
            BlockState mold = ModBlocks.MOLD_CRAWL.getDefaultState().with(MoldcrawlBlock.FACING, outward);
            if (mold.canPlaceAt(world, moldPos)) {
                world.setBlockState(moldPos, mold, Block.NOTIFY_LISTENERS);
            }
        }
    }
}
