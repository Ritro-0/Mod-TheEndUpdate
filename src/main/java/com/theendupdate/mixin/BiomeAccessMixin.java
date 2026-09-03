package com.theendupdate.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Forces Shadowlands biome identity inside the ShadowlandsRegion mask, so F3 and /locate biome
 * point to Shadowlands across the entire region instead of tiny End Biome API patches.
 */
@Mixin(BiomeManager.class)
public abstract class BiomeAccessMixin {

    @Inject(method = "getBiome", at = @At("TAIL"), cancellable = true)
    private void theendupdate$shadowlandsMaskIdentity(BlockPos pos, CallbackInfoReturnable<Holder<Biome>> cir) {
        // Disabled: Shadowlands are first-class biomes via TheEndBiomes; let source provide identity
    }
}


