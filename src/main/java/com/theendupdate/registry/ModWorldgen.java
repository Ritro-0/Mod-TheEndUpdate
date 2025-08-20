package com.theendupdate.registry;

import com.theendupdate.TemplateMod;
import com.theendupdate.world.feature.MirelandsGroundCoverFeature;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.biome.v1.TheEndBiomes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.PlacedFeature;

/**
 * Registers End biome distribution and ground cover features for Mirelands.
 */
public final class ModWorldgen {
	// Feature registration
    public static final Feature<DefaultFeatureConfig> MIRELANDS_GROUND_COVER = Registry.register(
        Registries.FEATURE,
        id("mirelands_ground_cover"),
        new MirelandsGroundCoverFeature(DefaultFeatureConfig.CODEC)
    );

    public static final Feature<DefaultFeatureConfig> MIRELANDS_VEGETATION = Registry.register(
        Registries.FEATURE,
        id("mirelands_vegetation"),
        new com.theendupdate.world.feature.MirelandsVegetationFeature(DefaultFeatureConfig.CODEC)
    );

    public static final Feature<DefaultFeatureConfig> MIRELANDS_TREE_CLUSTER = Registry.register(
        Registries.FEATURE,
        id("mirelands_tree_cluster"),
        new com.theendupdate.world.feature.MirelandsTreeClusterFeature(DefaultFeatureConfig.CODEC)
    );

    // Post-chorus bloom attachment feature: scans for chorus flowers and attaches void blooms to mature buds
    public static final Feature<DefaultFeatureConfig> VOID_BLOOM_CHORUS_ATTACHMENT = Registry.register(
        Registries.FEATURE,
        id("void_bloom_chorus_attachment"),
        new com.theendupdate.world.feature.VoidBloomChorusAttachmentFeature(DefaultFeatureConfig.CODEC)
    );

    // End crystal spikes: generate on island faces across all End biomes (outside main island radius)
    public static final Feature<DefaultFeatureConfig> END_CRYSTAL_SPIKE = Registry.register(
        Registries.FEATURE,
        id("end_crystal_spike"),
        new com.theendupdate.world.feature.EndCrystalSpikeFeature(DefaultFeatureConfig.CODEC)
    );

    public static final RegistryKey<PlacedFeature> MIRELANDS_GROUND_COVER_PLACED_KEY = RegistryKey.of(
        RegistryKeys.PLACED_FEATURE, id("mirelands_ground_cover"));
    public static final RegistryKey<PlacedFeature> MIRELANDS_VEGETATION_PLACED_KEY = RegistryKey.of(
        RegistryKeys.PLACED_FEATURE, id("mirelands_vegetation"));
    public static final RegistryKey<PlacedFeature> MIRELANDS_TREE_CLUSTER_PLACED_KEY = RegistryKey.of(
        RegistryKeys.PLACED_FEATURE, id("mirelands_tree_cluster"));

    public static final RegistryKey<PlacedFeature> VOID_BLOOM_CHORUS_ATTACHMENT_PLACED_KEY = RegistryKey.of(
        RegistryKeys.PLACED_FEATURE, id("void_bloom_chorus_attachment"));
    public static final RegistryKey<PlacedFeature> END_CRYSTAL_SPIKE_PLACED_KEY = RegistryKey.of(
        RegistryKeys.PLACED_FEATURE, id("end_crystal_spike"));

	// Biome keys
	public static final RegistryKey<Biome> MIRELANDS_HIGHLANDS_KEY = RegistryKey.of(RegistryKeys.BIOME, id("mirelands_highlands"));
	public static final RegistryKey<Biome> MIRELANDS_MIDLANDS_KEY = RegistryKey.of(RegistryKeys.BIOME, id("mirelands_midlands"));
	public static final RegistryKey<Biome> MIRELANDS_BARRENS_KEY = RegistryKey.of(RegistryKeys.BIOME, id("mirelands_barrens"));

	public static void registerAll() {
		// Inject feature into our Mirelands biomes
		BiomeModifications.addFeature(
			BiomeSelectors.includeByKey(MIRELANDS_HIGHLANDS_KEY, MIRELANDS_MIDLANDS_KEY, MIRELANDS_BARRENS_KEY),
			GenerationStep.Feature.TOP_LAYER_MODIFICATION,
			MIRELANDS_GROUND_COVER_PLACED_KEY
		);

        BiomeModifications.addFeature(
            BiomeSelectors.includeByKey(MIRELANDS_HIGHLANDS_KEY, MIRELANDS_MIDLANDS_KEY, MIRELANDS_BARRENS_KEY),
            GenerationStep.Feature.TOP_LAYER_MODIFICATION,
            MIRELANDS_VEGETATION_PLACED_KEY
        );

        // Trees: place in underground decoration to ensure after surface is set but before vegetation that relies on surface quirks
        BiomeModifications.addFeature(
            BiomeSelectors.includeByKey(MIRELANDS_HIGHLANDS_KEY, MIRELANDS_MIDLANDS_KEY, MIRELANDS_BARRENS_KEY),
            GenerationStep.Feature.VEGETAL_DECORATION,
            MIRELANDS_TREE_CLUSTER_PLACED_KEY
        );

        // Run after chorus generation: inject into Vegetal Decoration for all End biomes
        BiomeModifications.addFeature(
            BiomeSelectors.foundInTheEnd(),
            GenerationStep.Feature.VEGETAL_DECORATION,
            VOID_BLOOM_CHORUS_ATTACHMENT_PLACED_KEY
        );

        // Crystal spikes: add to surface structures to avoid clashing with flora; include all End biomes
        BiomeModifications.addFeature(
            BiomeSelectors.foundInTheEnd(),
            GenerationStep.Feature.LOCAL_MODIFICATIONS,
            END_CRYSTAL_SPIKE_PLACED_KEY
        );

        // End biome API: add our highlands and link its midlands/barrens
		TheEndBiomes.addHighlandsBiome(MIRELANDS_HIGHLANDS_KEY, 5);
		TheEndBiomes.addMidlandsBiome(MIRELANDS_HIGHLANDS_KEY, MIRELANDS_MIDLANDS_KEY, 1);
		TheEndBiomes.addBarrensBiome(MIRELANDS_HIGHLANDS_KEY, MIRELANDS_BARRENS_KEY, 1);

        // Registration complete
	}

	private static Identifier id(String path) {
		return Identifier.of(TemplateMod.MOD_ID, path);
	}

	private ModWorldgen() {}
}


