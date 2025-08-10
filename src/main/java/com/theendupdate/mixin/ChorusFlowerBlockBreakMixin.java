package com.theendupdate.mixin;

import com.theendupdate.registry.ModBlocks;
import net.minecraft.block.BlockState;
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
        // Only prevent breaking if there's specifically a Void Bloom adjacent to this chorus flower
        if (state.isOf(net.minecraft.block.Blocks.CHORUS_FLOWER)) {
            // FIRST: Check if the flower has proper stem support (chorus plant below)
            BlockPos below = pos.down();
            BlockState belowState = world.getBlockState(below);
            if (!belowState.isOf(net.minecraft.block.Blocks.CHORUS_PLANT) && !belowState.isOf(net.minecraft.block.Blocks.END_STONE)) {
                // No stem support - let it break naturally regardless of Void Blooms
                return;
            }
            
            // ONLY if stem support exists, then check for Void Bloom protection
            for (Direction direction : Direction.values()) {
                BlockPos adjacentPos = pos.offset(direction);
                if (world.getBlockState(adjacentPos).isOf(ModBlocks.VOID_BLOOM)) {
                    com.theendupdate.TemplateMod.LOGGER.info("Preventing chorus flower break at {} due to adjacent Void Bloom at {} (stem supported)", pos, adjacentPos);
                    cir.setReturnValue(true); // Force the flower to stay valid only when Void Bloom is present AND stem exists
                    return;
                }
            }
        }
    }
}
