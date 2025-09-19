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

        // Very sparse placement: ~16% of chunks attempt, up to 3 tries within the chunk
        if (random.nextFloat() > 0.16f) {
            return false;
        }

        boolean placedAny = false;
        int attempts = 3;
        for (int i = 0; i < attempts; i++) {
            int x = startX + random.nextInt(16);
            int z = startZ + random.nextInt(16);
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
            // consider success if any trunk appeared
            if (world.getBlockState(trunkBase).isOf(ModBlocks.SHADOW_CRYPTOMYCOTA)) {
                placedAny = true;
                break;
            }
        }

        return placedAny;
    }

    // No far-biome sampling to avoid chunk-availability crashes during worldgen
}


