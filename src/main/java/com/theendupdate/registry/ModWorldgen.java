package com.theendupdate.registry;

import com.theendupdate.TemplateMod;
import com.theendupdate.world.feature.MirelandsGroundCoverFeature;
import com.theendupdate.world.feature.BlueIceRiverFeature;
import com.theendupdate.world.feature.ShadowlandsGroundCoverFeature;
import com.theendupdate.world.feature.ShadowlandsChorusCleanupFeature;
import com.theendupdate.world.feature.ShadowlandsHugeTreeFeature;
import com.theendupdate.world.feature.ShadowlandsLandmassFeature;
import com.theendupdate.world.feature.ShadowClawScatterFeature;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.GenerationStep;
import net.fabricmc.fabric.api.biome.v1.TheEndBiomes;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.PlacedFeature;
// import removed: not using explicit structure injection due to unavailable API

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

    // Gravitite single-node ore feature
    public static final Feature<DefaultFeatureConfig> GRAVITITE_ORE_NODE = Registry.register(
        Registries.FEATURE,
        id("gravitite_ore_node"),
        new com.theendupdate.world.feature.GravititeOreNodeFeature(DefaultFeatureConfig.CODEC)
    );

    // Ender chrysanthemum attachment on small end islands
    public static final Feature<DefaultFeatureConfig> ENDER_CHRYSANTHEMUM_ISLANDS = Registry.register(
        Registries.FEATURE,
        id("ender_chrysanthemum_islands"),
        new com.theendupdate.world.feature.EnderChrysanthemumIslandsFeature(DefaultFeatureConfig.CODEC)
    );

    // Blue ice rivers hanging from island edges
    public static final Feature<DefaultFeatureConfig> BLUE_ICE_RIVER = Registry.register(
        Registries.FEATURE,
        id("blue_ice_river"),
        new BlueIceRiverFeature(DefaultFeatureConfig.CODEC)
    );

    // Shadowlands features
    public static final Feature<DefaultFeatureConfig> SHADOWLANDS_GROUND_COVER = Registry.register(
        Registries.FEATURE,
        id("shadowlands_ground_cover"),
        new ShadowlandsGroundCoverFeature(DefaultFeatureConfig.CODEC)
    );

    public static final Feature<DefaultFeatureConfig> SHADOWLANDS_CHORUS_CLEANUP = Registry.register(
        Registries.FEATURE,
        id("shadowlands_chorus_cleanup"),
        new ShadowlandsChorusCleanupFeature(DefaultFeatureConfig.CODEC)
    );

    public static final Feature<DefaultFeatureConfig> SHADOWLANDS_HUGE_TREE = Registry.register(
        Registries.FEATURE,
        id("shadowlands_huge_tree"),
        new ShadowlandsHugeTreeFeature(DefaultFeatureConfig.CODEC)
    );

    public static final Feature<DefaultFeatureConfig> SHADOWLANDS_LANDMASS = Registry.register(
        Registries.FEATURE,
        id("shadowlands_landmass"),
        new ShadowlandsLandmassFeature(DefaultFeatureConfig.CODEC)
    );

    public static final Feature<DefaultFeatureConfig> SHADOW_CLAW_SCATTER = Registry.register(
        Registries.FEATURE,
        id("shadow_claw_scatter"),
        new ShadowClawScatterFeature(DefaultFeatureConfig.CODEC)
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

    public static final RegistryKey<PlacedFeature> ENDER_CHRYSANTHEMUM_ISLANDS_PLACED_KEY = RegistryKey.of(
        RegistryKeys.PLACED_FEATURE, id("ender_chrysanthemum_islands"));

    public static final RegistryKey<PlacedFeature> BLUE_ICE_RIVER_PLACED_KEY = RegistryKey.of(
        RegistryKeys.PLACED_FEATURE, id("blue_ice_river"));

    // Ores
    public static final RegistryKey<PlacedFeature> GRAVITITE_ORE_PLACED_KEY = RegistryKey.of(
        RegistryKeys.PLACED_FEATURE, id("gravitite_ore"));
    public static final RegistryKey<PlacedFeature> GRAVITITE_ORE_NODE_PLACED_KEY = RegistryKey.of(
        RegistryKeys.PLACED_FEATURE, id("gravitite_ore_node"));

	// Shadowlands placed features
	public static final RegistryKey<PlacedFeature> SHADOWLANDS_GROUND_COVER_PLACED_KEY = RegistryKey.of(
		RegistryKeys.PLACED_FEATURE, id("shadowlands_ground_cover"));
	public static final RegistryKey<PlacedFeature> SHADOWLANDS_HUGE_TREE_PLACED_KEY = RegistryKey.of(
		RegistryKeys.PLACED_FEATURE, id("shadowlands_huge_tree"));
	public static final RegistryKey<PlacedFeature> SHADOW_CLAW_SCATTER_PLACED_KEY = RegistryKey.of(
		RegistryKeys.PLACED_FEATURE, id("shadow_claw_scatter"));
	public static final RegistryKey<PlacedFeature> SHADOWLANDS_CHORUS_CLEANUP_PLACED_KEY = RegistryKey.of(
		RegistryKeys.PLACED_FEATURE, id("shadowlands_chorus_cleanup"));

	// Biome keys
	public static final RegistryKey<Biome> MIRELANDS_HIGHLANDS_KEY = RegistryKey.of(RegistryKeys.BIOME, id("mirelands_highlands"));
	public static final RegistryKey<Biome> MIRELANDS_MIDLANDS_KEY = RegistryKey.of(RegistryKeys.BIOME, id("mirelands_midlands"));
	public static final RegistryKey<Biome> MIRELANDS_BARRENS_KEY = RegistryKey.of(RegistryKeys.BIOME, id("mirelands_barrens"));
	public static final RegistryKey<Biome> SHADOWLANDS_HIGHLANDS_KEY = RegistryKey.of(RegistryKeys.BIOME, id("shadowlands_highlands"));
	public static final RegistryKey<Biome> SHADOWLANDS_MIDLANDS_KEY = RegistryKey.of(RegistryKeys.BIOME, id("shadowlands_midlands"));
	public static final RegistryKey<Biome> SHADOWLANDS_BARRENS_KEY = RegistryKey.of(RegistryKeys.BIOME, id("shadowlands_barrens"));

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

        // Crystal spikes: include all End biomes (debug-wide coverage)
        BiomeModifications.addFeature(
            BiomeSelectors.foundInTheEnd(),
            GenerationStep.Feature.VEGETAL_DECORATION,
            END_CRYSTAL_SPIKE_PLACED_KEY
        );

        // Blue ice rivers across all End biomes, including Mirelands
        BiomeModifications.addFeature(
            BiomeSelectors.foundInTheEnd(),
            GenerationStep.Feature.LOCAL_MODIFICATIONS,
            BLUE_ICE_RIVER_PLACED_KEY
        );

        // Inject Gravitite ore into End biomes during underground ore generation
        BiomeModifications.addFeature(
            BiomeSelectors.foundInTheEnd(),
            GenerationStep.Feature.UNDERGROUND_ORES,
            GRAVITITE_ORE_PLACED_KEY
        );

        // Also inject Gravitite single-node placements to reach desired density
        BiomeModifications.addFeature(
            BiomeSelectors.foundInTheEnd(),
            GenerationStep.Feature.UNDERGROUND_ORES,
            GRAVITITE_ORE_NODE_PLACED_KEY
        );

        // Ender chrysanthemums on Small End Islands only (from previous iteration)
        BiomeModifications.addFeature(
            BiomeSelectors.includeByKey(BiomeKeys.SMALL_END_ISLANDS),
            GenerationStep.Feature.VEGETAL_DECORATION,
            ENDER_CHRYSANTHEMUM_ISLANDS_PLACED_KEY
        );

        // End biome API: add our Mirelands highlands and link midlands/barrens so worldgen places the biomes
        TheEndBiomes.addHighlandsBiome(MIRELANDS_HIGHLANDS_KEY, 2);
        TheEndBiomes.addMidlandsBiome(MIRELANDS_HIGHLANDS_KEY, MIRELANDS_MIDLANDS_KEY, 1);
        TheEndBiomes.addBarrensBiome(MIRELANDS_HIGHLANDS_KEY, MIRELANDS_BARRENS_KEY, 1);

        // Shadowlands biomes: register just like Mirelands, but with lower weight so they are rarer
        TheEndBiomes.addHighlandsBiome(SHADOWLANDS_HIGHLANDS_KEY, 8);
        TheEndBiomes.addMidlandsBiome(SHADOWLANDS_HIGHLANDS_KEY, SHADOWLANDS_MIDLANDS_KEY, 1);
        TheEndBiomes.addBarrensBiome(SHADOWLANDS_HIGHLANDS_KEY, SHADOWLANDS_BARRENS_KEY, 1);

		// Shadowlands injections limited to Shadowlands biomes
		BiomeModifications.addFeature(
			BiomeSelectors.includeByKey(SHADOWLANDS_HIGHLANDS_KEY, SHADOWLANDS_MIDLANDS_KEY, SHADOWLANDS_BARRENS_KEY),
			GenerationStep.Feature.TOP_LAYER_MODIFICATION,
			SHADOWLANDS_GROUND_COVER_PLACED_KEY
		);

		BiomeModifications.addFeature(
			BiomeSelectors.includeByKey(SHADOWLANDS_HIGHLANDS_KEY, SHADOWLANDS_MIDLANDS_KEY, SHADOWLANDS_BARRENS_KEY),
			GenerationStep.Feature.VEGETAL_DECORATION,
			SHADOW_CLAW_SCATTER_PLACED_KEY
		);

		BiomeModifications.addFeature(
			BiomeSelectors.includeByKey(SHADOWLANDS_HIGHLANDS_KEY, SHADOWLANDS_MIDLANDS_KEY, SHADOWLANDS_BARRENS_KEY),
			GenerationStep.Feature.VEGETAL_DECORATION,
			SHADOWLANDS_HUGE_TREE_PLACED_KEY
		);

		BiomeModifications.addFeature(
			BiomeSelectors.includeByKey(SHADOWLANDS_HIGHLANDS_KEY, SHADOWLANDS_MIDLANDS_KEY, SHADOWLANDS_BARRENS_KEY),
			GenerationStep.Feature.TOP_LAYER_MODIFICATION,
			SHADOWLANDS_CHORUS_CLEANUP_PLACED_KEY
		);


        // Registration complete
	}

	private static Identifier id(String path) {
		return Identifier.of(TemplateMod.MOD_ID, path);
	}

	private ModWorldgen() {}
}


