package com.theendupdate.world.feature;

import com.mojang.serialization.MapCodec;
import com.theendupdate.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * Shadowlands ground cover: Converts the exposed top End Stone to End Murk wherever there is air above.
 * Runs as a TOP_LAYER_MODIFICATION placed feature.
 */
public class ShadowlandsGroundCoverFeature implements Feature {
    public static final MapCodec<ShadowlandsGroundCoverFeature> CODEC = MapCodec.unit(ShadowlandsGroundCoverFeature::new);

    public ShadowlandsGroundCoverFeature() {}

    @Override
    public MapCodec<ShadowlandsGroundCoverFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel world, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        // no RandomSource needed, placement is a strict air-only rule now

        // injected only into Shadowlands biomes via BiomeModifications, no region scan needed here

        ChunkPos chunkPos = ChunkPos.containing(origin);
        int startX = chunkPos.getMinBlockX();
        int startZ = chunkPos.getMinBlockZ();

        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = startX + dx;
                int z = startZ + dz;
                BlockPos topPos = world.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, new BlockPos(x, 0, z)).below();
                if (topPos.getY() <= world.getMinY()) continue;

                BlockState current = world.getBlockState(topPos);
                // Shadowlands takes priority over neighboring covers
                boolean replaceable = current.is(Blocks.END_STONE)
                    || current.is(ModBlocks.END_MIRE)
                    || current.is(ModBlocks.MOLD_BLOCK);
                if (!replaceable) continue;

                // strictly Shadowlands biomes only (End Biomes API)
                var biome = world.getBiome(topPos);
                boolean isShadow = biome.is(com.theendupdate.registry.ModWorldgen.SHADOWLANDS_HIGHLANDS_KEY)
                    || biome.is(com.theendupdate.registry.ModWorldgen.SHADOWLANDS_MIDLANDS_KEY)
                    || biome.is(com.theendupdate.registry.ModWorldgen.SHADOWLANDS_BARRENS_KEY);
                if (!isShadow) continue;

                if (!world.isEmptyBlock(topPos.above())) continue;

                world.setBlock(topPos, ModBlocks.END_MURK.defaultBlockState(), Block.UPDATE_ALL);
            }
        }

        return true;
    }

    // no far-biome sampling, avoids chunk-availability crashes during worldgen
}


