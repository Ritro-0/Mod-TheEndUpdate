package com.theendupdate.world;

import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;
import net.minecraft.util.Identifier;
import com.theendupdate.TemplateMod;

/**
 * Holds registry entries for Shadowlands biomes so mixins can return proper entries without
 * directly accessing global registries from hot paths.
 */
public final class ShadowlandsBiomeIdentity {
    public static volatile RegistryEntry<Biome> HIGHLANDS;
    public static volatile RegistryEntry<Biome> MIDLANDS;
    public static volatile RegistryEntry<Biome> BARRENS;

    private ShadowlandsBiomeIdentity() {}

    public static void init(DynamicRegistryManager registryManager) {
        try {
            Registry<Biome> biomeRegistry = registryManager.getOrThrow(RegistryKeys.BIOME);
            Identifier hid = Identifier.of(TemplateMod.MOD_ID, "shadowlands_highlands");
            Identifier mid = Identifier.of(TemplateMod.MOD_ID, "shadowlands_midlands");
            Identifier bid = Identifier.of(TemplateMod.MOD_ID, "shadowlands_barrens");
            HIGHLANDS = biomeRegistry.getEntry(hid).orElse(null);
            MIDLANDS  = biomeRegistry.getEntry(mid).orElse(null);
            BARRENS   = biomeRegistry.getEntry(bid).orElse(null);
        } catch (Throwable ignored) {
            // Leave as nulls; mixin will fall back to current entry on failure
        }
    }

    public static RegistryEntry<Biome> highlandsOr(RegistryEntry<Biome> fallback) {
        return HIGHLANDS != null ? HIGHLANDS : fallback;
    }

    public static RegistryEntry<Biome> midlandsOr(RegistryEntry<Biome> fallback) {
        return MIDLANDS != null ? MIDLANDS : fallback;
    }

    public static RegistryEntry<Biome> barrensOr(RegistryEntry<Biome> fallback) {
        return BARRENS != null ? BARRENS : fallback;
    }
}


