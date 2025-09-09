package com.theendupdate.mixin;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.registry.DynamicRegistryManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public class TheEndUpdateMixin {
    @Inject(at = @At("HEAD"), method = "loadWorld")
    private void init(CallbackInfo info) {
        try {
            MinecraftServer self = (MinecraftServer)(Object)this;
            ServerWorld end = self.getWorld(net.minecraft.world.World.END);
            if (end != null) {
                DynamicRegistryManager manager = end.getRegistryManager();
                // Seed region rarity/placement masks and capture biome entries
                long seed = self.getOverworld().getSeed();
                com.theendupdate.world.ShadowlandsRegion.setSeed(seed);
                com.theendupdate.world.MirelandsRegion.setSeed(seed);
                com.theendupdate.world.ShadowlandsBiomeIdentity.init(manager);
            }
        } catch (Throwable ignored) {}
    }
}


