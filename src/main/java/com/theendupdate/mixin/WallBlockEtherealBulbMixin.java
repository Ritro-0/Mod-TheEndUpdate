package com.theendupdate.mixin;

import com.theendupdate.block.EtherealBulbButtonBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.ButtonBlock;
import net.minecraft.block.WallBlock;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.util.shape.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WallBlock.class)
public class WallBlockEtherealBulbMixin {
    
    /**
     * Inject into shouldHavePost to make walls recognize ethereal bulbs above as solid blocks.
     * This method is called by walls to determine if they should show their center post/pillar.
     */
    @Inject(method = "shouldHavePost", at = @At("HEAD"), cancellable = true)
    private void recognizeEtherealBulbAbove(BlockState aboveState, BlockState belowState, VoxelShape voxelShape,
                                           CallbackInfoReturnable<Boolean> cir) {
        // Check if the block above is an ethereal bulb mounted on the floor (on top of this wall)
        if (aboveState.getBlock() instanceof EtherealBulbButtonBlock) {
            try {
                BlockFace face = aboveState.get(ButtonBlock.FACE);
                if (face == BlockFace.FLOOR) {
                    // Force the wall to show its center post when ethereal bulb is on top
                    cir.setReturnValue(true);
                }
            } catch (Exception ignored) {
                // If we can't get the face property, ignore this check
            }
        }
    }
}

