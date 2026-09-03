package com.theendupdate.mixin;

import net.minecraft.core.RegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public class TheEndUpdateMixin {
    @Inject(at = @At("HEAD"), method = "loadLevel")
    private void init(CallbackInfo info) {
        try {
            @SuppressWarnings("resource")
            MinecraftServer self = (MinecraftServer)(Object)this;
            ServerLevel end = self.getLevel(net.minecraft.world.level.Level.END);
            if (end != null) {
                RegistryAccess manager = end.registryAccess();
                long seed = self.overworld().getSeed();
                com.theendupdate.world.OuterEndLayout.setSeed(seed);
                com.theendupdate.world.OuterEndBiomes.init(manager);
            }
        } catch (Throwable ignored) {}
    }
}


