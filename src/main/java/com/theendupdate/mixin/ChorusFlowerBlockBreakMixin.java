package com.theendupdate.mixin;

import com.theendupdate.TemplateMod;
import com.theendupdate.registry.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChorusFlowerBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.WorldView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChorusFlowerBlock.class)
public class ChorusFlowerBlockBreakMixin {

    @Inject(method = "canPlaceAt", at = @At("HEAD"), cancellable = true)
    private void preventBreakingNearVoidBloom(BlockState state, WorldView world, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        BlockPos below = pos.down();
        BlockState belowState = world.getBlockState(below);

        // Vanilla supports CHORUS_PLANT or END_STONE; add exceptions for END_MIRE and MOLD_BLOCK
        boolean hasValidBase =
            belowState.isOf(Blocks.CHORUS_PLANT)
                || belowState.isOf(Blocks.END_STONE)
                || belowState.isOf(ModBlocks.END_MIRE)
                || belowState.isOf(ModBlocks.MOLD_BLOCK);

        if (hasValidBase) {
            cir.setReturnValue(true);
            return;
        }

        if (state.isOf(Blocks.CHORUS_FLOWER)) {
            if (!hasValidBase) {
                return;
            }

            for (Direction direction : Direction.values()) {
                BlockPos adjacentPos = pos.offset(direction);
                BlockState adjacentState = world.getBlockState(adjacentPos);
                if (adjacentState.isOf(ModBlocks.VOID_BLOOM)) {
                    Direction attachmentFace = adjacentState.get(com.theendupdate.block.VoidBloomBlock.ATTACHMENT_FACE);
                    if (attachmentFace == direction.getOpposite()) {
                        TemplateMod.LOGGER.info("Preventing chorus flower break at {} due to properly attached Void Bloom at {} (base supported)", pos, adjacentPos);
                        cir.setReturnValue(true);
                        return;
                    }
                }
            }
        }
    }
}
