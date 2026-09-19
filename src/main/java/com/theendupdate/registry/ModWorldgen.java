package com.theendupdate.registry;

import com.theendupdate.TheEndUpdate;
import com.theendupdate.world.feature.BlueIceRiverFeature;
import com.theendupdate.world.feature.EndCrystalSpikeFeature;
import com.theendupdate.world.feature.EnderChrysanthemumIslandsFeature;
import com.theendupdate.world.feature.GravititeOreNodeFeature;
import com.theendupdate.world.feature.MirelandsGroundCoverFeature;
import com.theendupdate.world.feature.MirelandsTreeClusterFeature;
import com.theendupdate.world.feature.MirelandsVegetationFeature;
import com.theendupdate.world.feature.NebulaCraterFeature;
import com.theendupdate.world.feature.ShadowClawScatterFeature;
import com.theendupdate.world.feature.ShadowlandsChorusCleanupFeature;
import com.theendupdate.world.feature.ShadowlandsGroundCoverFeature;
import com.theendupdate.world.feature.ShadowlandsHugeTreeFeature;
import com.theendupdate.world.feature.VoidBloomChorusAttachmentFeature;
import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.biome.v1.TheEndBiomes;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * Registers End biome distribution and ground cover features for Mirelands.
 */
public final class ModWorldgen {
    static {
        registerFeatureType("mirelands_ground_cover", MirelandsGroundCoverFeature.CODEC);
        registerFeatureType("mirelands_vegetation", MirelandsVegetationFeature.CODEC);
        registerFeatureType("mirelands_tree_cluster", MirelandsTreeClusterFeature.CODEC);
        registerFeatureType("void_bloom_chorus_attachment", VoidBloomChorusAttachmentFeature.CODEC);
        registerFeatureType("end_crystal_spike", EndCrystalSpikeFeature.CODEC);
        registerFeatureType("gravitite_ore_node", GravititeOreNodeFeature.CODEC);
        registerFeatureType("ender_chrysanthemum_islands", EnderChrysanthemumIslandsFeature.CODEC);
        registerFeatureType("blue_ice_river", BlueIceRiverFeature.CODEC);
        registerFeatureType("nebula_crater", NebulaCraterFeature.CODEC);
        registerFeatureType("shadowlands_ground_cover", ShadowlandsGroundCoverFeature.CODEC);
        registerFeatureType("shadowlands_chorus_cleanup", ShadowlandsChorusCleanupFeature.CODEC);
        registerFeatureType("shadowlands_huge_tree", ShadowlandsHugeTreeFeature.CODEC);
        registerFeatureType("shadow_claw_scatter", ShadowClawScatterFeature.CODEC);
    }

    public static final ResourceKey<PlacedFeature> MIRELANDS_GROUND_COVER_PLACED_KEY = ResourceKey.create(
        Registries.PLACED_FEATURE, id("mirelands_ground_cover"));
    public static final ResourceKey<PlacedFeature> MIRELANDS_VEGETATION_PLACED_KEY = ResourceKey.create(
        Registries.PLACED_FEATURE, id("mirelands_vegetation"));
    public static final ResourceKey<PlacedFeature> MIRELANDS_TREE_CLUSTER_PLACED_KEY = ResourceKey.create(
        Registries.PLACED_FEATURE, id("mirelands_tree_cluster"));

    public static final ResourceKey<PlacedFeature> VOID_BLOOM_CHORUS_ATTACHMENT_PLACED_KEY = ResourceKey.create(
        Registries.PLACED_FEATURE, id("void_bloom_chorus_attachment"));
    public static final ResourceKey<PlacedFeature> END_CRYSTAL_SPIKE_PLACED_KEY = ResourceKey.create(
        Registries.PLACED_FEATURE, id("end_crystal_spike"));

    public static final ResourceKey<PlacedFeature> ENDER_CHRYSANTHEMUM_ISLANDS_PLACED_KEY = ResourceKey.create(
        Registries.PLACED_FEATURE, id("ender_chrysanthemum_islands"));

    public static final ResourceKey<PlacedFeature> BLUE_ICE_RIVER_PLACED_KEY = ResourceKey.create(
        Registries.PLACED_FEATURE, id("blue_ice_river"));

    public static final ResourceKey<PlacedFeature> NEBULA_CRATER_PLACED_KEY = ResourceKey.create(
        Registries.PLACED_FEATURE, id("nebula_crater"));

    public static final ResourceKey<PlacedFeature> GRAVITITE_ORE_PLACED_KEY = ResourceKey.create(
        Registries.PLACED_FEATURE, id("gravitite_ore"));
    public static final ResourceKey<PlacedFeature> GRAVITITE_ORE_NODE_PLACED_KEY = ResourceKey.create(
        Registries.PLACED_FEATURE, id("gravitite_ore_node"));

	public static final ResourceKey<PlacedFeature> SHADOWLANDS_GROUND_COVER_PLACED_KEY = ResourceKey.create(
		Registries.PLACED_FEATURE, id("shadowlands_ground_cover"));
	public static final ResourceKey<PlacedFeature> SHADOWLANDS_HUGE_TREE_PLACED_KEY = ResourceKey.create(
		Registries.PLACED_FEATURE, id("shadowlands_huge_tree"));
	public static final ResourceKey<PlacedFeature> SHADOW_CLAW_SCATTER_PLACED_KEY = ResourceKey.create(
		Registries.PLACED_FEATURE, id("shadow_claw_scatter"));
	public static final ResourceKey<PlacedFeature> SHADOWLANDS_CHORUS_CLEANUP_PLACED_KEY = ResourceKey.create(
		Registries.PLACED_FEATURE, id("shadowlands_chorus_cleanup"));

	public static final ResourceKey<Biome> MIRELANDS_HIGHLANDS_KEY = ResourceKey.create(Registries.BIOME, id("mirelands_highlands"));
	public static final ResourceKey<Biome> MIRELANDS_MIDLANDS_KEY = ResourceKey.create(Registries.BIOME, id("mirelands_midlands"));
	public static final ResourceKey<Biome> MIRELANDS_BARRENS_KEY = ResourceKey.create(Registries.BIOME, id("mirelands_barrens"));
	public static final ResourceKey<Biome> SHADOWLANDS_HIGHLANDS_KEY = ResourceKey.create(Registries.BIOME, id("shadowlands_highlands"));
	public static final ResourceKey<Biome> SHADOWLANDS_MIDLANDS_KEY = ResourceKey.create(Registries.BIOME, id("shadowlands_midlands"));
	public static final ResourceKey<Biome> SHADOWLANDS_BARRENS_KEY = ResourceKey.create(Registries.BIOME, id("shadowlands_barrens"));

	public static void registerAll() {
		BiomeModifications.addFeature(
			BiomeSelectors.includeByKey(MIRELANDS_HIGHLANDS_KEY, MIRELANDS_MIDLANDS_KEY, MIRELANDS_BARRENS_KEY),
			GenerationStep.Decoration.TOP_LAYER_MODIFICATION,
			MIRELANDS_GROUND_COVER_PLACED_KEY
		);

        BiomeModifications.addFeature(
            BiomeSelectors.includeByKey(MIRELANDS_HIGHLANDS_KEY, MIRELANDS_MIDLANDS_KEY, MIRELANDS_BARRENS_KEY),
            GenerationStep.Decoration.TOP_LAYER_MODIFICATION,
            MIRELANDS_VEGETATION_PLACED_KEY
        );

        // needs to run after the surface is set but before vegetation that relies on surface quirks
        BiomeModifications.addFeature(
            BiomeSelectors.includeByKey(MIRELANDS_HIGHLANDS_KEY, MIRELANDS_MIDLANDS_KEY, MIRELANDS_BARRENS_KEY),
            GenerationStep.Decoration.VEGETAL_DECORATION,
            MIRELANDS_TREE_CLUSTER_PLACED_KEY
        );

        // must run after chorus generation
        BiomeModifications.addFeature(
            BiomeSelectors.foundInTheEnd(),
            GenerationStep.Decoration.VEGETAL_DECORATION,
            VOID_BLOOM_CHORUS_ATTACHMENT_PLACED_KEY
        );

        BiomeModifications.addFeature(
            BiomeSelectors.foundInTheEnd(),
            GenerationStep.Decoration.VEGETAL_DECORATION,
            END_CRYSTAL_SPIKE_PLACED_KEY
        );

        BiomeModifications.addFeature(
            BiomeSelectors.foundInTheEnd(),
            GenerationStep.Decoration.LOCAL_MODIFICATIONS,
            BLUE_ICE_RIVER_PLACED_KEY
        );

        BiomeModifications.addFeature(
            BiomeSelectors.foundInTheEnd(),
            GenerationStep.Decoration.LOCAL_MODIFICATIONS,
            NEBULA_CRATER_PLACED_KEY
        );

        BiomeModifications.addFeature(
            BiomeSelectors.foundInTheEnd(),
            GenerationStep.Decoration.UNDERGROUND_ORES,
            GRAVITITE_ORE_PLACED_KEY
        );

        // separate single-node placement stacked on top, to reach desired ore density
        BiomeModifications.addFeature(
            BiomeSelectors.foundInTheEnd(),
            GenerationStep.Decoration.UNDERGROUND_ORES,
            GRAVITITE_ORE_NODE_PLACED_KEY
        );

        BiomeModifications.addFeature(
            BiomeSelectors.includeByKey(Biomes.SMALL_END_ISLANDS),
            GenerationStep.Decoration.VEGETAL_DECORATION,
            ENDER_CHRYSANTHEMUM_ISLANDS_PLACED_KEY
        );

        // Mirelands placement is whole 384-block cells in OuterEndLayout.
        // Keep a tiny Fabric weight so the biomes stay registered, without
        // sprinkling 30-block edge slivers everywhere.
        TheEndBiomes.addHighlandsBiome(MIRELANDS_HIGHLANDS_KEY, 0.05);
        TheEndBiomes.addMidlandsBiome(MIRELANDS_HIGHLANDS_KEY, MIRELANDS_MIDLANDS_KEY, 1.0);
        TheEndBiomes.addBarrensBiome(MIRELANDS_HIGHLANDS_KEY, MIRELANDS_BARRENS_KEY, 1.0);

		BiomeModifications.addFeature(
			BiomeSelectors.includeByKey(SHADOWLANDS_HIGHLANDS_KEY, SHADOWLANDS_MIDLANDS_KEY, SHADOWLANDS_BARRENS_KEY),
			GenerationStep.Decoration.TOP_LAYER_MODIFICATION,
			SHADOWLANDS_GROUND_COVER_PLACED_KEY
		);

		BiomeModifications.addFeature(
			BiomeSelectors.includeByKey(SHADOWLANDS_HIGHLANDS_KEY, SHADOWLANDS_MIDLANDS_KEY, SHADOWLANDS_BARRENS_KEY),
			GenerationStep.Decoration.VEGETAL_DECORATION,
			SHADOW_CLAW_SCATTER_PLACED_KEY
		);

		BiomeModifications.addFeature(
			BiomeSelectors.includeByKey(SHADOWLANDS_HIGHLANDS_KEY, SHADOWLANDS_MIDLANDS_KEY, SHADOWLANDS_BARRENS_KEY),
			GenerationStep.Decoration.VEGETAL_DECORATION,
			SHADOWLANDS_HUGE_TREE_PLACED_KEY
		);

		BiomeModifications.addFeature(
			BiomeSelectors.includeByKey(SHADOWLANDS_HIGHLANDS_KEY, SHADOWLANDS_MIDLANDS_KEY, SHADOWLANDS_BARRENS_KEY),
			GenerationStep.Decoration.TOP_LAYER_MODIFICATION,
			SHADOWLANDS_CHORUS_CLEANUP_PLACED_KEY
		);
	}

	private static void registerFeatureType(String path, MapCodec<? extends Feature> codec) {
		Registry.register(BuiltInRegistries.FEATURE_TYPE, id(path), codec);
	}

	private static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(TheEndUpdate.MOD_ID, path);
	}

	private ModWorldgen() {}
}
