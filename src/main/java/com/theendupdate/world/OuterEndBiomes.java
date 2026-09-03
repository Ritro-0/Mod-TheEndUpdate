package com.theendupdate.world;

import com.theendupdate.registry.ModWorldgen;
import java.util.Objects;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.QuartPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * Cached biome holders and the final outer-End biome pick used by mixins.
 */
public final class OuterEndBiomes {
    public enum Band {
        HIGHLANDS,
        MIDLANDS,
        BARRENS,
        SMALL_ISLANDS
    }

    public static volatile Holder<Biome> VANILLA_END;
    public static volatile Holder<Biome> VANILLA_HIGHLANDS;
    public static volatile Holder<Biome> VANILLA_MIDLANDS;
    public static volatile Holder<Biome> VANILLA_BARRENS;
    public static volatile Holder<Biome> VANILLA_SMALL_ISLANDS;
    public static volatile Holder<Biome> MIRE_HIGHLANDS;
    public static volatile Holder<Biome> MIRE_MIDLANDS;
    public static volatile Holder<Biome> MIRE_BARRENS;
    public static volatile Holder<Biome> SHADOW_HIGHLANDS;
    public static volatile Holder<Biome> SHADOW_MIDLANDS;
    public static volatile Holder<Biome> SHADOW_BARRENS;

    private OuterEndBiomes() {}

    public static void init(RegistryAccess access) {
        try {
            init(access.lookupOrThrow(Registries.BIOME));
        } catch (Throwable ignored) {
            // mixins fall back to vanilla holders if this fails
        }
    }

    public static void init(HolderGetter<Biome> biomes) {
        VANILLA_END = get(biomes, Biomes.THE_END);
        VANILLA_HIGHLANDS = get(biomes, Biomes.END_HIGHLANDS);
        VANILLA_MIDLANDS = get(biomes, Biomes.END_MIDLANDS);
        VANILLA_BARRENS = get(biomes, Biomes.END_BARRENS);
        VANILLA_SMALL_ISLANDS = get(biomes, Biomes.SMALL_END_ISLANDS);
        MIRE_HIGHLANDS = get(biomes, ModWorldgen.MIRELANDS_HIGHLANDS_KEY);
        MIRE_MIDLANDS = get(biomes, ModWorldgen.MIRELANDS_MIDLANDS_KEY);
        MIRE_BARRENS = get(biomes, ModWorldgen.MIRELANDS_BARRENS_KEY);
        SHADOW_HIGHLANDS = get(biomes, ModWorldgen.SHADOWLANDS_HIGHLANDS_KEY);
        SHADOW_MIDLANDS = get(biomes, ModWorldgen.SHADOWLANDS_MIDLANDS_KEY);
        SHADOW_BARRENS = get(biomes, ModWorldgen.SHADOWLANDS_BARRENS_KEY);

        ShadowlandsBiomeIdentity.HIGHLANDS = SHADOW_HIGHLANDS;
        ShadowlandsBiomeIdentity.MIDLANDS = SHADOW_MIDLANDS;
        ShadowlandsBiomeIdentity.BARRENS = SHADOW_BARRENS;
    }

    public static Stream<Holder<Biome>> customBiomes() {
        return Stream.of(
            MIRE_HIGHLANDS,
            MIRE_MIDLANDS,
            MIRE_BARRENS,
            SHADOW_HIGHLANDS,
            SHADOW_MIDLANDS,
            SHADOW_BARRENS
        ).filter(Objects::nonNull);
    }

    public static Band bandFromErosion(double erosion) {
        if (erosion > 0.25) {
            return Band.HIGHLANDS;
        }
        if (erosion >= -0.0625) {
            return Band.MIDLANDS;
        }
        if (erosion < -0.21875) {
            return Band.SMALL_ISLANDS;
        }
        return Band.BARRENS;
    }

    public static Holder<Biome> biomeAt(int biomeX, int biomeY, int biomeZ, Climate.Sampler sampler) {
        int blockX = QuartPos.toBlock(biomeX);
        int blockY = QuartPos.toBlock(biomeY);
        int blockZ = QuartPos.toBlock(biomeZ);
        int sectionX = SectionPos.blockToSectionCoord(blockX);
        int sectionZ = SectionPos.blockToSectionCoord(blockZ);
        if ((long) sectionX * (long) sectionX + (long) sectionZ * (long) sectionZ <= 4096L) {
            return or(VANILLA_END, VANILLA_HIGHLANDS);
        }

        int sampleX = (sectionX * 2 + 1) * 8;
        int sampleZ = (sectionZ * 2 + 1) * 8;
        double erosion = sampler.erosion().compute(new DensityFunction.SinglePointContext(sampleX, blockY, sampleZ));
        return pick(OuterEndLayout.familyAt(blockX, blockZ), bandFromErosion(erosion));
    }

    public static Holder<Biome> pick(OuterEndLayout.Family family, Band band) {
        return switch (family) {
            case CENTER, VANILLA -> vanillaBand(band);
            case MIRELANDS -> switch (band) {
                case HIGHLANDS -> or(MIRE_HIGHLANDS, VANILLA_HIGHLANDS);
                case MIDLANDS -> or(MIRE_MIDLANDS, VANILLA_MIDLANDS);
                case BARRENS -> or(MIRE_BARRENS, VANILLA_BARRENS);
                case SMALL_ISLANDS -> or(VANILLA_SMALL_ISLANDS, VANILLA_BARRENS);
            };
            case SHADOWLANDS -> switch (band) {
                case HIGHLANDS -> or(SHADOW_HIGHLANDS, VANILLA_HIGHLANDS);
                case MIDLANDS -> or(SHADOW_MIDLANDS, VANILLA_MIDLANDS);
                case BARRENS -> or(SHADOW_BARRENS, VANILLA_BARRENS);
                case SMALL_ISLANDS -> or(VANILLA_SMALL_ISLANDS, VANILLA_BARRENS);
            };
        };
    }

    private static Holder<Biome> vanillaBand(Band band) {
        return switch (band) {
            case HIGHLANDS -> or(VANILLA_HIGHLANDS, VANILLA_END);
            case MIDLANDS -> or(VANILLA_MIDLANDS, VANILLA_END);
            case BARRENS -> or(VANILLA_BARRENS, VANILLA_END);
            case SMALL_ISLANDS -> or(VANILLA_SMALL_ISLANDS, VANILLA_END);
        };
    }

    private static Holder<Biome> or(Holder<Biome> preferred, Holder<Biome> fallback) {
        return preferred != null ? preferred : fallback;
    }

    private static Holder<Biome> get(HolderGetter<Biome> biomes, ResourceKey<Biome> key) {
        try {
            return biomes.getOrThrow(key);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
