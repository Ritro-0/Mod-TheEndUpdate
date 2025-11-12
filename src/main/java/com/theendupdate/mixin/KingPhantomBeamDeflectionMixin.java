package com.theendupdate.mixin;

import com.theendupdate.entity.KingPhantomEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Allows players to deflect King Phantom beam attacks by hitting them
 */
@Mixin(PlayerEntity.class)
public abstract class KingPhantomBeamDeflectionMixin {
    
    @Inject(method = "attack", at = @At("HEAD"))
    private void theendupdate$checkBeamDeflection(net.minecraft.entity.Entity target, CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        if (!(self.getEntityWorld() instanceof ServerWorld sw)) return;
        
        // Check if player is attacking (has attack cooldown active or just swung)
        // Also check all King Phantoms nearby for active beams that could be deflected
        for (KingPhantomEntity phantom : sw.getEntitiesByClass(KingPhantomEntity.class, 
                self.getBoundingBox().expand(15.0), (p) -> true)) {
            if (phantom.tryDeflectBeam(self)) {
                // Beam was deflected - play sound and visual feedback handled in tryDeflectBeam
                break;
            }
        }
    }
}

