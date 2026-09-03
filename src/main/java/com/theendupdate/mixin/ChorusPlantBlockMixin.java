package com.theendupdate.mixin;

import com.theendupdate.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChorusPlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChorusPlantBlock.class)
public class ChorusPlantBlockMixin {

    @Inject(method = "canSurvive", at = @At("HEAD"), cancellable = true)
    private void allowOnEndMireAndMold(BlockState state, LevelReader world, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        // no chorus plant placement inside shadowlands
        if (com.theendupdate.world.ShadowlandsRegion.isInRegion(pos.getX() >> 4, pos.getZ() >> 4)) { cir.setReturnValue(false); return; }
        BlockPos below = pos.below();
        BlockState belowState = world.getBlockState(below);

        boolean hasValidBase =
            belowState.is(Blocks.CHORUS_PLANT)
                || belowState.is(Blocks.END_STONE)
                || belowState.is(ModBlocks.END_MIRE)
                || belowState.is(ModBlocks.MOLD_BLOCK);

        if (hasValidBase) {
            cir.setReturnValue(true);
        }
    }
}


