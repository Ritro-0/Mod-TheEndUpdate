package com.theendupdate.mixin;

import com.theendupdate.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LeashFenceKnotEntity.class)
public abstract class LeashKnotEntityMixin {

    // survives() controls whether the knot stays attached; force true for our custom fences
    @Inject(method = "survives", at = @At("HEAD"), cancellable = true)
    private void theendupdate$allowCustomFences(CallbackInfoReturnable<Boolean> cir) {
        LeashFenceKnotEntity self = (LeashFenceKnotEntity)(Object)this;
        BlockPos pos = self.getPos();
        if (pos == null) return;
        
        BlockState state = self.level().getBlockState(pos);

        if (state.is(ModBlocks.ETHEREAL_FENCE) || state.is(ModBlocks.SHADOW_FENCE)) {
            cir.setReturnValue(true);
        }
    }
}
