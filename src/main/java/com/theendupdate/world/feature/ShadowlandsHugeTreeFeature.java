package com.theendupdate.world.feature;

import com.mojang.serialization.Codec;
import com.theendupdate.world.ShadowClawTreeGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import com.theendupdate.registry.ModBlocks;

/**
 * Places massive shadow trees sparsely across Shadowlands. Designed for very low density but monumental scale.
 */
public class ShadowlandsHugeTreeFeature extends Feature<NoneFeatureConfiguration> {
    public ShadowlandsHugeTreeFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel world = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();

        ChunkPos chunkPos = ChunkPos.containing(origin);
        int startX = chunkPos.getMinBlockX();
        int startZ = chunkPos.getMinBlockZ();

        // very sparse: ~16% of chunks attempt, up to 3 tries each
        if (random.nextFloat() > 0.16f) {
            return false;
        }

        boolean placedAny = false;
        int attempts = 3;
        for (int i = 0; i < attempts; i++) {
            int x = startX + random.nextInt(16);
            int z = startZ + random.nextInt(16);
            BlockPos surface = world.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, new BlockPos(x, 0, z)).below();
            if (surface.getY() <= world.getMinY()) continue;
            var biome = world.getBiome(surface);
            boolean isShadow = biome.is(com.theendupdate.registry.ModWorldgen.SHADOWLANDS_HIGHLANDS_KEY)
                || biome.is(com.theendupdate.registry.ModWorldgen.SHADOWLANDS_MIDLANDS_KEY)
                || biome.is(com.theendupdate.registry.ModWorldgen.SHADOWLANDS_BARRENS_KEY);
            if (!isShadow) continue;
            BlockState ground = world.getBlockState(surface);
            if (!(ground.is(ModBlocks.END_MURK) || ground.is(net.minecraft.world.level.block.Blocks.END_STONE))) continue;
            if (ground.is(net.minecraft.world.level.block.Blocks.END_STONE)) {
                world.setBlock(surface, ModBlocks.END_MURK.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
            }
            BlockPos trunkBase = surface.above();
            if (!world.isEmptyBlock(trunkBase)) continue;

            ShadowClawTreeGenerator.generate(world, trunkBase, random);
            // success if any trunk appeared
            if (world.getBlockState(trunkBase).is(ModBlocks.SHADOW_CRYPTOMYCOTA)) {
                placedAny = true;
                break;
            }
        }

        return placedAny;
    }

    // no far-biome sampling, avoids chunk-availability crashes during worldgen
}


