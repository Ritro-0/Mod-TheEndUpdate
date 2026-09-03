package com.theendupdate.mixin;

import com.theendupdate.entity.KingPhantomEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Allows players to deflect King Phantom beam attacks by hitting them
 */
@Mixin(Player.class)
public abstract class KingPhantomBeamDeflectionMixin {
    
    @Inject(method = "attack", at = @At("HEAD"))
    private void theendupdate$checkBeamDeflection(net.minecraft.world.entity.Entity target, CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (!(self.level() instanceof ServerLevel sw)) return;

        // fires on every swing, check nearby king phantoms for a beam to deflect
        for (KingPhantomEntity phantom : sw.getEntitiesOfClass(KingPhantomEntity.class, 
                self.getBoundingBox().inflate(15.0), (p) -> true)) {
            if (phantom.tryDeflectBeam(self)) {
                // sound/visual feedback handled inside tryDeflectBeam
                break;
            }
        }
    }
}

