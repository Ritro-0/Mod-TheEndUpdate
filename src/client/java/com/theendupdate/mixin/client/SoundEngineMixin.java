package com.theendupdate.mixin.client;

import com.theendupdate.SoundHooks;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundEngine.class)
public class SoundEngineMixin {
    @Inject(method = "play", at = @At("HEAD"), require = 0)
    private void theendupdate$onPlay(SoundInstance sound, CallbackInfoReturnable<?> cir) {
        SoundHooks.onSoundPlayed(sound);
    }

    @Inject(method = "playDelayed", at = @At("HEAD"), require = 0)
    private void theendupdate$onPlayDelayed(SoundInstance sound, int delay, CallbackInfo ci) {
        SoundHooks.onSoundPlayed(sound);
    }
}
