package com.theendupdate.block;

import net.minecraft.block.Block;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
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

            // Skip invalid targets: air, fluids, plants, crops, bedrock, storage, redstone, command blocks
            if (world.isAir(target)) continue;
            if (!targetState.getFluidState().isEmpty()) continue;
            if (targetState.isIn(BlockTags.FLOWERS)) continue;
            if (targetState.isIn(BlockTags.CROPS)) continue;
            if (targetState.isOf(Blocks.BEDROCK)) continue;
            
            // Protect storage blocks
            if (targetState.isOf(Blocks.CHEST)) continue;
            if (targetState.isOf(Blocks.TRAPPED_CHEST)) continue;
            if (targetState.isOf(Blocks.ENDER_CHEST)) continue;
            if (targetState.isOf(Blocks.BARREL)) continue;
            if (targetState.isOf(Blocks.SHULKER_BOX)) continue;
            if (targetState.isOf(Blocks.DISPENSER)) continue;
            if (targetState.isOf(Blocks.DROPPER)) continue;
            if (targetState.isOf(Blocks.HOPPER)) continue;
            if (targetState.isOf(Blocks.FURNACE)) continue;
            if (targetState.isOf(Blocks.BLAST_FURNACE)) continue;
            if (targetState.isOf(Blocks.SMOKER)) continue;
            if (targetState.isOf(Blocks.BREWING_STAND)) continue;
            if (targetState.isOf(Blocks.ENCHANTING_TABLE)) continue;
            if (targetState.isOf(Blocks.ANVIL)) continue;
            if (targetState.isOf(Blocks.CHIPPED_ANVIL)) continue;
            if (targetState.isOf(Blocks.DAMAGED_ANVIL)) continue;
            
            // Protect redstone components
            if (targetState.isOf(Blocks.REDSTONE_WIRE)) continue;
            if (targetState.isOf(Blocks.REPEATER)) continue;
            if (targetState.isOf(Blocks.COMPARATOR)) continue;
            if (targetState.isOf(Blocks.REDSTONE_BLOCK)) continue;
            if (targetState.isOf(Blocks.REDSTONE_LAMP)) continue;
            if (targetState.isOf(Blocks.REDSTONE_TORCH)) continue;
            if (targetState.isOf(Blocks.REDSTONE_WALL_TORCH)) continue;
            
            // Protect command exclusive blocks
            if (targetState.isOf(Blocks.COMMAND_BLOCK)) continue;
            if (targetState.isOf(Blocks.CHAIN_COMMAND_BLOCK)) continue;
            if (targetState.isOf(Blocks.REPEATING_COMMAND_BLOCK)) continue;
            if (targetState.isOf(Blocks.STRUCTURE_BLOCK)) continue;
            if (targetState.isOf(Blocks.JIGSAW)) continue;
            if (targetState.isOf(Blocks.STRUCTURE_VOID)) continue;
            
            // Protect other important blocks
            if (targetState.isOf(Blocks.SPAWNER)) continue;
            if (targetState.isOf(Blocks.BEACON)) continue;
            if (targetState.isOf(Blocks.END_PORTAL_FRAME)) continue;
            if (targetState.isOf(Blocks.END_PORTAL)) continue;
            if (targetState.isOf(Blocks.NETHER_PORTAL)) continue;
            if (targetState.isOf(Blocks.END_GATEWAY)) continue;
            
            // Check if block has colored name (rarity indicator)
            // This would require checking the block's item form, but for now we'll protect specific rare blocks
            if (targetState.isOf(Blocks.DRAGON_EGG)) continue;
            if (targetState.isOf(Blocks.ANCIENT_DEBRIS)) continue;
            if (targetState.isOf(Blocks.NETHERITE_BLOCK)) continue;

            world.setBlockState(target, state, Block.NOTIFY_ALL);
        }
    }
}


