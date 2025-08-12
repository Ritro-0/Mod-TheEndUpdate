package com.theendupdate.mixin;

import com.theendupdate.registry.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChorusPlantBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChorusPlantBlock.class)
public class ChorusPlantBlockMixin {

    @Inject(method = "canPlaceAt", at = @At("HEAD"), cancellable = true)
    private void allowOnEndMireAndMold(BlockState state, WorldView world, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        BlockPos below = pos.down();
        BlockState belowState = world.getBlockState(below);

        boolean hasValidBase =
            belowState.isOf(Blocks.CHORUS_PLANT)
                || belowState.isOf(Blocks.END_STONE)
                || belowState.isOf(ModBlocks.END_MIRE)
                || belowState.isOf(ModBlocks.MOLD_BLOCK);

        if (hasValidBase) {
            cir.setReturnValue(true);
        }
    }
}


