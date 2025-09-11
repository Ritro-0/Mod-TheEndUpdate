package com.theendupdate.world.feature;

import com.theendupdate.registry.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

/**
 * Places single Gravitite Ore nodes that are fully encased in End Stone,
 * spaced at least 20 blocks apart from other Gravitite Ore, at random Y levels.
 *
 * Density target: ~0.5 nodes per chunk on average (about 10 per typical island),
 * enforced via a per-chunk probability gate and strict spacing checks.
 */
public class GravititeOreNodeFeature extends Feature<DefaultFeatureConfig> {

    public GravititeOreNodeFeature(com.mojang.serialization.Codec<DefaultFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
        StructureWorldAccess world = context.getWorld();
        Random random = context.getRandom();
        BlockPos origin = context.getOrigin();

        // Probabilistic gate so we average ~0.5 nodes per chunk
        if (random.nextFloat() > 0.5f) {
            return false;
        }

        ChunkPos chunkPos = new ChunkPos(origin);
        int startX = chunkPos.getStartX();
        int startZ = chunkPos.getStartZ();
        int bottomY = world.getBottomY();
        int topY = world.getHeight();

        // Try multiple samples to find a valid fully-encased spot
        final int maxAttempts = 48;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            int x = startX + random.nextInt(16);
            int z = startZ + random.nextInt(16);
            int y = bottomY + 8 + random.nextInt(Math.max(1, (topY - bottomY) - 16));

            BlockPos pos = new BlockPos(x, y, z);
            if (!isFullyEncasedInEndStone(world, pos)) {
                continue;
            }

            if (!isFarFromOtherGravitite(world, pos, 20)) {
                continue;
            }

            // Place the node
            world.setBlockState(pos, ModBlocks.GRAVITITE_ORE.getDefaultState(), Block.NOTIFY_LISTENERS);
            return true;
        }
        return false;
    }

    private static boolean isFullyEncasedInEndStone(StructureWorldAccess world, BlockPos pos) {
        BlockState center = world.getBlockState(pos);
        if (!center.isOf(Blocks.END_STONE)) return false;
        for (Direction d : Direction.values()) {
            BlockState s = world.getBlockState(pos.offset(d));
            if (!s.isOf(Blocks.END_STONE)) return false;
        }
        return true;
    }

    private static boolean isFarFromOtherGravitite(StructureWorldAccess world, BlockPos pos, int minDistance) {
        int r = minDistance; // inclusive cube radius check
        // Scan a cube neighborhood; cheap but robust across chunk boundaries
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    BlockPos check = pos.add(dx, dy, dz);
                    BlockState state = world.getBlockState(check);
                    if (state.isOf(ModBlocks.GRAVITITE_ORE)) {
                        // Enforce strictly greater than or equal minDistance by Manhattan distance
                        int manhattan = Math.abs(dx) + Math.abs(dy) + Math.abs(dz);
                        if (manhattan < minDistance) return false;
                    }
                }
            }
        }
        return true;
    }
}


