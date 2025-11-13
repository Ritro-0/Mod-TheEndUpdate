package com.theendupdate.block;

import com.theendupdate.registry.ModBlocks;
import net.minecraft.block.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

public class EndMireBlock extends Block implements Fertilizable {
    public EndMireBlock(AbstractBlock.Settings settings) {
        super(settings);
    }

    @Override
    public void afterBreak(net.minecraft.world.World world, net.minecraft.entity.player.PlayerEntity player, net.minecraft.util.math.BlockPos pos, net.minecraft.block.BlockState state, net.minecraft.block.entity.BlockEntity blockEntity, net.minecraft.item.ItemStack tool) {
        super.afterBreak(world, player, pos, state, blockEntity, tool);
        if (!world.isClient() && world instanceof ServerWorld serverWorld) {
            // Override to drop 0 XP (match nylium)
            this.dropExperience(serverWorld, pos, 0);
        }
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

            // Verify the ground block is actually solid and supported (not floating in air above a crater)
            BlockState targetState = world.getBlockState(target);
            if (!targetState.isSolid() || targetState.isAir()) continue;
            BlockPos targetBelow = target.down();
            BlockState targetBelowState = world.getBlockState(targetBelow);
            // If there's air below the ground block, we might be over a crater - skip placement
            if (targetBelowState.isAir() && target.getY() > world.getBottomY() + 5) continue;

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

}
