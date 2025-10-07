package com.theendupdate.mixin;

import com.theendupdate.block.EtherealFenceBlock;
import com.theendupdate.block.ShadowFenceBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FenceBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FenceBlock.class)
public class FenceBlockConnectivityMixin {
    
    @Inject(method = "canConnect", at = @At("HEAD"), cancellable = true)
    private void allowCustomFenceConnections(BlockState state, boolean neighborIsFullSquare, 
                                           net.minecraft.util.math.Direction dir, 
                                           CallbackInfoReturnable<Boolean> cir) {
        Block block = state.getBlock();
        
        // Allow vanilla fences to connect to our custom fences
        if (block instanceof EtherealFenceBlock || block instanceof ShadowFenceBlock) {
            cir.setReturnValue(true);
        }
    }
}
