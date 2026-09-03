package com.theendupdate.world;

import com.theendupdate.registry.ModWorldgen;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.biome.Biome;

/**
 * Holds registry entries for Shadowlands biomes so mixins can return proper entries without
 * directly accessing global registries from hot paths.
 */
public final class ShadowlandsBiomeIdentity {
    public static volatile Holder<Biome> HIGHLANDS;
    public static volatile Holder<Biome> MIDLANDS;
    public static volatile Holder<Biome> BARRENS;

    private ShadowlandsBiomeIdentity() {}

    public static void init(RegistryAccess registryManager) {
        OuterEndBiomes.init(registryManager);
    }

    public static Holder<Biome> highlandsOr(Holder<Biome> fallback) {
        return HIGHLANDS != null ? HIGHLANDS : fallback;
    }

    public static Holder<Biome> midlandsOr(Holder<Biome> fallback) {
        return MIDLANDS != null ? MIDLANDS : fallback;
    }

    public static Holder<Biome> barrensOr(Holder<Biome> fallback) {
        return BARRENS != null ? BARRENS : fallback;
    }

    public static boolean isShadowlands(Holder<Biome> biome) {
        return biome.is(ModWorldgen.SHADOWLANDS_HIGHLANDS_KEY)
            || biome.is(ModWorldgen.SHADOWLANDS_MIDLANDS_KEY)
            || biome.is(ModWorldgen.SHADOWLANDS_BARRENS_KEY);
    }
}
