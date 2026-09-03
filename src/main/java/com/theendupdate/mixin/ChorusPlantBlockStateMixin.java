package com.theendupdate.mixin;

import com.theendupdate.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChorusPlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChorusPlantBlock.class)
public class ChorusPlantBlockStateMixin {

    @Inject(method = "updateShape", at = @At("RETURN"), cancellable = true)
    private void ensureDownConnectionOnCustomBases(
        BlockState state,
        LevelReader world,
        ScheduledTickAccess scheduledTickView,
        BlockPos pos,
        Direction direction,
        BlockPos neighborPos,
        BlockState neighborState,
        RandomSource random,
        CallbackInfoReturnable<BlockState> cir
    ) {
        BlockState current = cir.getReturnValue();
        if (current == null) {
            return;
        }
        BlockState below = world.getBlockState(pos.below());
        boolean downValid =
            below.is(Blocks.CHORUS_PLANT)
                || below.is(Blocks.END_STONE)
                || below.is(ModBlocks.END_MIRE)
                || below.is(ModBlocks.MOLD_BLOCK);

        if (current.hasProperty(ChorusPlantBlock.DOWN)) {
            cir.setReturnValue(current.setValue(ChorusPlantBlock.DOWN, downValid));
        }
    }

    @Inject(method = "getStateForPlacement", at = @At("RETURN"), cancellable = true)
    private void ensureDownConnectionOnPlacement(BlockPlaceContext ctx, CallbackInfoReturnable<BlockState> cir) {
        BlockState current = cir.getReturnValue();
        if (current == null) {
            return;
        }
        BlockPos pos = ctx.getClickedPos();
        BlockState below = ctx.getLevel().getBlockState(pos.below());
        boolean downValid =
            below.is(Blocks.CHORUS_PLANT)
                || below.is(Blocks.END_STONE)
                || below.is(ModBlocks.END_MIRE)
                || below.is(ModBlocks.MOLD_BLOCK);

        if (current.hasProperty(ChorusPlantBlock.DOWN)) {
            cir.setReturnValue(current.setValue(ChorusPlantBlock.DOWN, downValid));
        }
    }
}


