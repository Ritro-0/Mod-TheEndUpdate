package com.theendupdate.mixin.client;

// Sound detection moved to client tick - mixin was causing crashes
@org.spongepowered.asm.mixin.Mixin(net.minecraft.client.sound.SoundManager.class)
public class SoundManagerMixin {
    // Empty - functionality moved to TemplateModClient client tick handler
}
