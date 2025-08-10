package com.theendupdate.block;

import net.minecraft.block.Block;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.block.Fertilizable;

public class MoldBlock extends Block implements Fertilizable {
    public MoldBlock(AbstractBlock.Settings settings) {
        super(settings);
    }

    @Override
    public boolean isFertilizable(WorldView world, BlockPos pos, BlockState state) {
        // Always allow bonemeal usage
        return true;
    }

    @Override
    public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
        // Reduced spread compared to moss: do ~half the attempts in a small radius
        // Only place this block, no carpets/azalea/etc
        int attempts = 64; // moss typically attempts more; we intentionally reduce
        int radius = 2;    // small radius

        for (int i = 0; i < attempts; i++) {
            BlockPos target = pos.add(
                random.nextBetween(-radius, radius),
                random.nextBetween(-1, 1),
                random.nextBetween(-radius, radius)
            );

            BlockState targetState = world.getBlockState(target);

            // Skip invalid targets: air, fluids (water/lava/waterlogged), flowers, crops, bedrock
            if (world.isAir(target)) continue;
            if (!targetState.getFluidState().isEmpty()) continue;
            if (targetState.isIn(BlockTags.FLOWERS)) continue;
            if (targetState.isIn(BlockTags.CROPS)) continue;
            if (targetState.isOf(Blocks.BEDROCK)) continue;

            world.setBlockState(target, state, Block.NOTIFY_ALL);
        }
    }
}


