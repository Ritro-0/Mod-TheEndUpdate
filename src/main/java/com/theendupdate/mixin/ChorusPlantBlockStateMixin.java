package com.theendupdate.mixin;

import com.theendupdate.registry.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChorusPlantBlock;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChorusPlantBlock.class)
public class ChorusPlantBlockStateMixin {

    @Inject(method = "getStateForNeighborUpdate", at = @At("RETURN"), cancellable = true)
    private void ensureDownConnectionOnCustomBases(
        BlockState state,
        WorldView world,
        ScheduledTickView scheduledTickView,
        BlockPos pos,
        Direction direction,
        BlockPos neighborPos,
        BlockState neighborState,
        Random random,
        CallbackInfoReturnable<BlockState> cir
    ) {
        BlockState current = cir.getReturnValue();
        if (current == null) {
            return;
        }
        BlockState below = world.getBlockState(pos.down());
        boolean downValid =
            below.isOf(Blocks.CHORUS_PLANT)
                || below.isOf(Blocks.END_STONE)
                || below.isOf(ModBlocks.END_MIRE)
                || below.isOf(ModBlocks.MOLD_BLOCK);

        if (current.contains(ChorusPlantBlock.DOWN)) {
            cir.setReturnValue(current.with(ChorusPlantBlock.DOWN, downValid));
        }
    }

    @Inject(method = "getPlacementState", at = @At("RETURN"), cancellable = true)
    private void ensureDownConnectionOnPlacement(ItemPlacementContext ctx, CallbackInfoReturnable<BlockState> cir) {
        BlockState current = cir.getReturnValue();
        if (current == null) {
            return;
        }
        BlockPos pos = ctx.getBlockPos();
        BlockState below = ctx.getWorld().getBlockState(pos.down());
        boolean downValid =
            below.isOf(Blocks.CHORUS_PLANT)
                || below.isOf(Blocks.END_STONE)
                || below.isOf(ModBlocks.END_MIRE)
                || below.isOf(ModBlocks.MOLD_BLOCK);

        if (current.contains(ChorusPlantBlock.DOWN)) {
            cir.setReturnValue(current.with(ChorusPlantBlock.DOWN, downValid));
        }
    }
}


