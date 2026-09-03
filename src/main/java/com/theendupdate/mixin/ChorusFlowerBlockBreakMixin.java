package com.theendupdate.mixin;

import com.theendupdate.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChorusFlowerBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChorusFlowerBlock.class)
public class ChorusFlowerBlockBreakMixin {

    @Inject(method = "canSurvive", at = @At("HEAD"), cancellable = true)
    private void preventBreakingNearVoidBloom(BlockState state, LevelReader world, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        BlockPos below = pos.below();
        BlockState belowState = world.getBlockState(below);

        // Vanilla supports CHORUS_PLANT or END_STONE; add exceptions for END_MIRE and MOLD_BLOCK
        boolean hasValidBase =
            belowState.is(Blocks.CHORUS_PLANT)
                || belowState.is(Blocks.END_STONE)
                || belowState.is(ModBlocks.END_MIRE)
                || belowState.is(ModBlocks.MOLD_BLOCK);

        if (hasValidBase) {
            cir.setReturnValue(true);
            return;
        }

        if (state.is(Blocks.CHORUS_FLOWER)) {
            if (!hasValidBase) {
                return;
            }

            for (Direction direction : Direction.values()) {
                BlockPos adjacentPos = pos.relative(direction);
                BlockState adjacentState = world.getBlockState(adjacentPos);
                if (adjacentState.is(ModBlocks.VOID_BLOOM)) {
                    Direction attachmentFace = adjacentState.getValue(com.theendupdate.block.VoidBloomBlock.ATTACHMENT_FACE);
                    if (attachmentFace == direction.getOpposite()) {
                        cir.setReturnValue(true);
                        return;
                    }
                }
            }
        }
    }
}
