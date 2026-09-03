package com.theendupdate.block;

import com.theendupdate.registry.ModBlocks;
import net.minecraft.world.level.block.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class EndMireBlock extends Block implements BonemealableBlock {
    public EndMireBlock(BlockBehaviour.Properties settings) {
        super(settings);
    }

    @Override
    public void playerDestroy(net.minecraft.world.level.Level world, net.minecraft.world.entity.player.Player player, net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.block.entity.BlockEntity blockEntity, net.minecraft.world.item.ItemStack tool) {
        super.playerDestroy(world, player, pos, state, blockEntity, tool);
        if (!world.isClientSide() && world instanceof ServerLevel serverWorld) {
            // matches nylium, 0 XP on break
            this.popExperience(serverWorld, pos, 0);
        }
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader world, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public boolean isBonemealSuccess(Level world, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel world, RandomSource random, BlockPos pos, BlockState state) {
        // spread onto adjacent End Stone first, like nylium onto netherrack
        boolean spread = trySpreadToEndStone(world, pos, random);
        if (spread) {
            return;
        }
        generateMoldSpores(world, pos, random); // otherwise, a few Mold Spores, like warped roots on nylium
    }

    private boolean trySpreadToEndStone(ServerLevel world, BlockPos origin, RandomSource random) {
        boolean convertedAny = false;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos targetPos = origin.relative(direction);
            BlockState targetState = world.getBlockState(targetPos);
            if (targetState.is(Blocks.END_STONE)) {
                world.setBlock(targetPos, ModBlocks.END_MIRE.defaultBlockState(), Block.UPDATE_ALL);
                convertedAny = true;
            }
        }
        return convertedAny;
    }

    private void generateMoldSpores(ServerLevel world, BlockPos origin, RandomSource random) {
        int attempts = 16; // modest amount like roots generation
        for (int i = 0; i < attempts; i++) {
            BlockPos target = origin.offset(random.nextIntBetweenInclusive(-2, 2), random.nextIntBetweenInclusive(-1, 1), random.nextIntBetweenInclusive(-2, 2));
            BlockPos above = target.above();

            if (!world.getBlockState(target).is(ModBlocks.END_MIRE)) continue;
            if (!world.isEmptyBlock(above)) continue;

            // make sure the ground is actually solid, not floating over a crater
            BlockState targetState = world.getBlockState(target);
            if (!targetState.isSolid() || targetState.isAir()) continue;
            BlockPos targetBelow = target.below();
            BlockState targetBelowState = world.getBlockState(targetBelow);
            if (targetBelowState.isAir() && target.getY() > world.getMinY() + 5) continue;

            int choice = random.nextInt(3);
            if (choice == 0) {
                world.setBlock(above, ModBlocks.MOLD_SPORE.defaultBlockState(), Block.UPDATE_ALL);
            } else if (choice == 1) {
                world.setBlock(above, ModBlocks.MOLD_SPORE_TUFT.defaultBlockState(), Block.UPDATE_ALL);
            } else {
                BlockPos top = above.above();
                if (world.isEmptyBlock(top)) {
                    net.minecraft.world.level.block.DoublePlantBlock.placeAt(world, ModBlocks.MOLD_SPORE_SPROUT.defaultBlockState(), above, Block.UPDATE_ALL);
                }
            }
        }
    }

}
