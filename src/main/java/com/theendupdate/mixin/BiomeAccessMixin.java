package com.theendupdate.mixin;

// Shadowlands biome removed
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Forces Shadowlands biome identity inside the ShadowlandsRegion mask, so F3 and /locate biome
 * point to Shadowlands across the entire region instead of tiny End Biome API patches.
 */
@Mixin(BiomeAccess.class)
public abstract class BiomeAccessMixin {

    @Inject(method = "getBiome", at = @At("TAIL"), cancellable = true)
    private void theendupdate$shadowlandsMaskIdentity(BlockPos pos, CallbackInfoReturnable<RegistryEntry<Biome>> cir) {
        // Disabled: Shadowlands are first-class biomes via TheEndBiomes; let source provide identity
    }
}


