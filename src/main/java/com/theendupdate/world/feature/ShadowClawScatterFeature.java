package com.theendupdate.world.feature;

import com.mojang.serialization.Codec;
import com.theendupdate.registry.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

/**
 * Loosely scatters Shadow Claw plants across Shadowlands surfaces.
 */
public class ShadowClawScatterFeature extends Feature<DefaultFeatureConfig> {
    public ShadowClawScatterFeature(Codec<DefaultFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
        StructureWorldAccess world = context.getWorld();
        Random random = context.getRandom();
        BlockPos origin = context.getOrigin();

        ChunkPos chunkPos = new ChunkPos(origin);
        int startX = chunkPos.getStartX();
        int startZ = chunkPos.getStartZ();

        boolean any = false;

        // Feature is injected only into Shadowlands biomes via BiomeModifications; no extra region scan here

        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                // Very dense scatter across the surface (≈80% of columns attempt placement)
                if (random.nextFloat() > 0.20f) continue;

                int x = startX + dx;
                int z = startZ + dz;
                BlockPos surface = world.getTopPosition(Heightmap.Type.WORLD_SURFACE_WG, new BlockPos(x, 0, z)).down();
                if (surface.getY() <= world.getBottomY()) continue;
                if (!world.isAir(surface.up())) continue;

                // Restrict strictly to Shadowlands biomes placed by The End Biomes API
                var biome = world.getBiome(surface);
                boolean isShadow = biome.matchesKey(com.theendupdate.registry.ModWorldgen.SHADOWLANDS_HIGHLANDS_KEY)
                    || biome.matchesKey(com.theendupdate.registry.ModWorldgen.SHADOWLANDS_MIDLANDS_KEY)
                    || biome.matchesKey(com.theendupdate.registry.ModWorldgen.SHADOWLANDS_BARRENS_KEY);
                if (!isShadow) continue;

                BlockState ground = world.getBlockState(surface);
                if (!(ground.isOf(ModBlocks.END_MURK) || ground.isOf(net.minecraft.block.Blocks.END_STONE))) continue;

                BlockPos place = surface.up();
                // Randomize variant on natural placement so all four appear
                int variant = random.nextBetween(0, 3);
                BlockState claw = ModBlocks.SHADOW_CLAW.getDefaultState().with(com.theendupdate.block.ShadowClawBlock.VARIANT, variant);
                if (claw.canPlaceAt(world, place)) {
                    // Ensure substrate is End Murk to avoid End Stone beneath natural spawns
                    if (ground.isOf(net.minecraft.block.Blocks.END_STONE)) {
                        world.setBlockState(surface, ModBlocks.END_MURK.getDefaultState(), 3);
                    }
                    world.setBlockState(place, claw, 3);
                    any = true;
                }
            }
        }

        return any;
    }

    // No far-biome sampling to avoid chunk-availability crashes during worldgen
}


