package com.theendupdate.mixin.client;

import com.theendupdate.SoundHooks;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundManager.class)
public class SoundManagerMixin {
    @Inject(method = "play", at = @At("HEAD"), require = 0)
    private void theendupdate$onPlayReturnable(SoundInstance sound, CallbackInfoReturnable<?> cir) {
        SoundHooks.onSoundPlayed(sound);
    }

    @Inject(method = "play", at = @At("HEAD"), require = 0)
    private void theendupdate$onPlayVoid(SoundInstance sound, CallbackInfo ci) {
        SoundHooks.onSoundPlayed(sound);
    }

    @Inject(method = "playDelayed", at = @At("HEAD"), require = 0)
    private void theendupdate$onPlayDelayed(SoundInstance sound, int delay, CallbackInfo ci) {
        SoundHooks.onSoundPlayed(sound);
    }
}
