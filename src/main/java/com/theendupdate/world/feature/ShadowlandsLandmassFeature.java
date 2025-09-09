package com.theendupdate.world.feature;

import com.mojang.serialization.Codec;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import com.theendupdate.world.ShadowlandsRegion;
import com.theendupdate.registry.ModWorldgen;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

/**
 * Expands Shadowlands landmass by filling void columns up to a target height with End Stone.
 * Runs before top-layer modification so End Murk can claim the surface afterward.
 */
public class ShadowlandsLandmassFeature extends Feature<DefaultFeatureConfig> {
    public ShadowlandsLandmassFeature(Codec<DefaultFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
        // Keep disabled landmass extension; rough up edges will be handled by ground cover claiming randomness
        return false;
    }
}


