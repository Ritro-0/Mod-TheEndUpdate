package com.theendupdate.mixin;

import com.theendupdate.registry.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.entity.decoration.LeashKnotEntity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LeashKnotEntity.class)
public abstract class LeashKnotEntityMixin {

    /**
     * Intercept the method that checks if the leash knot can stay attached.
     * In Yarn 1.21.8, this is likely canStayAttached() or similar.
     * We inject at HEAD and return true early if attached to our custom fences.
     */
    @Inject(method = "canStayAttached", at = @At("HEAD"), cancellable = true)
    private void theendupdate$allowCustomFences(CallbackInfoReturnable<Boolean> cir) {
        LeashKnotEntity self = (LeashKnotEntity)(Object)this;
        BlockPos pos = self.getAttachedBlockPos();
        if (pos == null) return;
        
        BlockState state = self.getEntityWorld().getBlockState(pos);
        
        // If attached to our custom fences, force it to stay attached
        if (state.isOf(ModBlocks.ETHEREAL_FENCE) || state.isOf(ModBlocks.SHADOW_FENCE)) {
            cir.setReturnValue(true);
        }
    }
}
