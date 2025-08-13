package com.theendupdate.block;

import com.theendupdate.registry.ModBlocks;
import net.minecraft.block.*;
// no unused imports
import net.minecraft.server.world.ServerWorld;
// no unused imports
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
// no unused imports
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;

public class EndMireBlock extends Block implements Fertilizable {
    public EndMireBlock(AbstractBlock.Settings settings) {
        super(settings);
    }

    // Always allow bonemeal interaction
    @Override
    public boolean isFertilizable(WorldView world, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
        // First: try to spread to adjacent End Stone like Nylium -> Netherrack
        boolean spread = trySpreadToEndStone(world, pos, random);
        if (spread) {
            return;
        }
        // Otherwise: generate a few Mold Spores around, like warped roots on nylium
        generateMoldSpores(world, pos, random);
    }

    private boolean trySpreadToEndStone(ServerWorld world, BlockPos origin, Random random) {
        boolean convertedAny = false;
        for (Direction direction : Direction.Type.HORIZONTAL) {
            BlockPos targetPos = origin.offset(direction);
            BlockState targetState = world.getBlockState(targetPos);
            if (targetState.isOf(Blocks.END_STONE)) {
                world.setBlockState(targetPos, ModBlocks.END_MIRE.getDefaultState(), Block.NOTIFY_ALL);
                convertedAny = true;
            }
        }
        return convertedAny;
    }

    private void generateMoldSpores(ServerWorld world, BlockPos origin, Random random) {
        int attempts = 16; // modest amount like roots generation
        for (int i = 0; i < attempts; i++) {
            BlockPos target = origin.add(random.nextBetween(-2, 2), random.nextBetween(-1, 1), random.nextBetween(-2, 2));
            BlockPos above = target.up();

            if (!world.getBlockState(target).isOf(ModBlocks.END_MIRE)) continue;
            if (!world.isAir(above)) continue;

            int choice = random.nextInt(3);
            if (choice == 0) {
                world.setBlockState(above, ModBlocks.MOLD_SPORE.getDefaultState(), Block.NOTIFY_ALL);
            } else if (choice == 1) {
                world.setBlockState(above, ModBlocks.MOLD_SPORE_TUFT.getDefaultState(), Block.NOTIFY_ALL);
            } else {
                BlockPos top = above.up();
                if (world.isAir(top)) {
                    // Place double tall sprout
                    net.minecraft.block.TallPlantBlock.placeAt(world, ModBlocks.MOLD_SPORE_SPROUT.getDefaultState(), above, Block.NOTIFY_ALL);
                }
            }
        }
    }

    @Override
    public void afterBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, net.minecraft.block.entity.BlockEntity blockEntity, ItemStack tool) {
        // Manually handle drops to mirror nylium: Silk Touch -> End Mire; else -> End Stone
        if (!world.isClient) {
            int silk = 0;
            try {
                ItemEnchantmentsComponent ench = tool.get(DataComponentTypes.ENCHANTMENTS);
                if (ench != null) {
                    silk = ench.toString().contains("minecraft:silk_touch") ? 1 : 0;
                }
            } catch (Throwable t) {}
            if (silk > 0) {
                Block.dropStack(world, pos, new ItemStack(com.theendupdate.registry.ModBlocks.END_MIRE.asItem()));
            } else {
                Block.dropStack(world, pos, new ItemStack(Blocks.END_STONE.asItem()));
            }
            ((ServerWorld) world).emitGameEvent(player, net.minecraft.world.event.GameEvent.BLOCK_DESTROY, pos);
        }
        // Intentionally do not call super.afterBreak to prevent default loot table drops (avoids double-drops)
    }
}
