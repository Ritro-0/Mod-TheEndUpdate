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

public class EndMurkBlock extends Block implements BonemealableBlock {
    public EndMurkBlock(BlockBehaviour.Properties settings) {
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
        generateShadowClaws(world, pos, random); // otherwise, grow Shadow Claws (Shadowlands vegetation only)
    }

    private boolean trySpreadToEndStone(ServerLevel world, BlockPos origin, RandomSource random) {
        boolean convertedAny = false;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos targetPos = origin.relative(direction);
            BlockState targetState = world.getBlockState(targetPos);
            if (targetState.is(Blocks.END_STONE)) {
                world.setBlock(targetPos, this.defaultBlockState(), Block.UPDATE_ALL);
                convertedAny = true;
            }
        }
        return convertedAny;
    }

    private void generateShadowClaws(ServerLevel world, BlockPos origin, RandomSource random) {
        int attempts = 24; // denser than spores
        for (int i = 0; i < attempts; i++) {
            BlockPos target = origin.offset(random.nextIntBetweenInclusive(-3, 3), random.nextIntBetweenInclusive(-1, 1), random.nextIntBetweenInclusive(-3, 3));
            BlockPos above = target.above();
            if (!world.getBlockState(target).is(this)) continue;
            if (!world.isEmptyBlock(above)) continue;
            int variant = random.nextIntBetweenInclusive(0, 3);
            BlockState claw = ModBlocks.SHADOW_CLAW.defaultBlockState().setValue(ShadowClawBlock.VARIANT, variant);
            if (claw.canSurvive(world, above)) {
                world.setBlock(above, claw, Block.UPDATE_ALL);
            }
        }
    }

}


