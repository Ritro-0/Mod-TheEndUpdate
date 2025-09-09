package com.theendupdate.world.feature;

import com.mojang.serialization.Codec;
import com.theendupdate.world.ShadowClawTreeGenerator;
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
 * Places massive shadow trees sparsely across Shadowlands. Designed for very low density but monumental scale.
 */
public class ShadowlandsHugeTreeFeature extends Feature<DefaultFeatureConfig> {
    public ShadowlandsHugeTreeFeature(Codec<DefaultFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
        StructureWorldAccess world = context.getWorld();
        BlockPos origin = context.getOrigin();
        Random random = context.getRandom();

        ChunkPos chunkPos = new ChunkPos(origin);
        int startX = chunkPos.getStartX();
        int startZ = chunkPos.getStartZ();

        // Feature is injected only into Shadowlands biomes via BiomeModifications; no extra region scan here

        boolean placedAny = false;
        int attempts = 192; // even more random attempts for higher density
        // Grid attempts every 2 blocks in both dimensions to maximize coverage
        for (int gx = 1; gx < 16; gx += 2) {
            for (int gz = 1; gz < 16; gz += 2) {
                int x = startX + gx;
                int z = startZ + gz;
                BlockPos surface = world.getTopPosition(Heightmap.Type.WORLD_SURFACE_WG, new BlockPos(x, 0, z)).down();
                if (surface.getY() <= world.getBottomY()) continue;
                var biome = world.getBiome(surface);
                boolean isShadow = biome.matchesKey(com.theendupdate.registry.ModWorldgen.SHADOWLANDS_HIGHLANDS_KEY)
                    || biome.matchesKey(com.theendupdate.registry.ModWorldgen.SHADOWLANDS_MIDLANDS_KEY)
                    || biome.matchesKey(com.theendupdate.registry.ModWorldgen.SHADOWLANDS_BARRENS_KEY);
                if (!isShadow) continue;
                BlockState ground = world.getBlockState(surface);
                if (!(ground.isOf(ModBlocks.END_MURK) || ground.isOf(net.minecraft.block.Blocks.END_STONE))) continue;
                if (ground.isOf(net.minecraft.block.Blocks.END_STONE)) {
                    world.setBlockState(surface, ModBlocks.END_MURK.getDefaultState(), net.minecraft.block.Block.NOTIFY_LISTENERS);
                }
                BlockPos trunkBase = surface.up();
                if (!world.isAir(trunkBase)) continue;
                ShadowClawTreeGenerator.generate(world, trunkBase, random);
                placedAny = true;
            }
        }
        for (int i = 0; i < attempts; i++) {
            int x = startX + random.nextInt(16);
            int z = startZ + random.nextInt(16);
            BlockPos surface = world.getTopPosition(Heightmap.Type.WORLD_SURFACE_WG, new BlockPos(x, 0, z)).down();
            if (surface.getY() <= world.getBottomY()) continue;
            var biome2 = world.getBiome(surface);
            boolean isShadow2 = biome2.matchesKey(com.theendupdate.registry.ModWorldgen.SHADOWLANDS_HIGHLANDS_KEY)
                || biome2.matchesKey(com.theendupdate.registry.ModWorldgen.SHADOWLANDS_MIDLANDS_KEY)
                || biome2.matchesKey(com.theendupdate.registry.ModWorldgen.SHADOWLANDS_BARRENS_KEY);
            if (!isShadow2) continue;
            // Acceptable ground
            BlockState ground = world.getBlockState(surface);
            if (!(ground.isOf(ModBlocks.END_MURK) || ground.isOf(net.minecraft.block.Blocks.END_STONE))) continue;
            // Favor End Murk by converting surface if needed to allow large trees
            if (ground.isOf(net.minecraft.block.Blocks.END_STONE)) {
                world.setBlockState(surface, ModBlocks.END_MURK.getDefaultState(), net.minecraft.block.Block.NOTIFY_LISTENERS);
            }
            BlockPos trunkBase = surface.up();
            if (!world.isAir(trunkBase)) continue;

            // Delegate to generator (works with WorldAccess)
            ShadowClawTreeGenerator.generate(world, trunkBase, random);
            placedAny = true;
        }

        return placedAny;
    }

    // No far-biome sampling to avoid chunk-availability crashes during worldgen
}


