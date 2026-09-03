package com.theendupdate.world.feature;

import com.mojang.serialization.Codec;
import com.theendupdate.registry.ModBlocks;
import com.theendupdate.registry.ModWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Replaces the top exposed End Stone on Mirelands islands with 80% End Mire and 20% Mold Block.
 * Runs as a TOP_LAYER_MODIFICATION placed feature.
 */
public class MirelandsGroundCoverFeature extends Feature<NoneFeatureConfiguration> {
    public MirelandsGroundCoverFeature(Codec<NoneFeatureConfiguration> codec) {
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

        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = startX + dx;
                int z = startZ + dz;
                BlockPos topPos = world.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, new BlockPos(x, 0, z)).below();
                if (topPos.getY() <= world.getMinY()) {
                    continue;
                }

                BlockState current = world.getBlockState(topPos);
                if (!current.is(Blocks.END_STONE)) {
                    continue;
                }

                // Mirelands biomes only
                Holder<Biome> biome = world.getBiome(topPos);
                boolean isMire = biome.is(ModWorldgen.MIRELANDS_HIGHLANDS_KEY)
                    || biome.is(ModWorldgen.MIRELANDS_MIDLANDS_KEY)
                    || biome.is(ModWorldgen.MIRELANDS_BARRENS_KEY);
                if (!isMire) {
                    continue;
                }

                boolean placeMire = random.nextFloat() < 0.8f;
                BlockState replacement = placeMire ? ModBlocks.END_MIRE.defaultBlockState() : ModBlocks.MOLD_BLOCK.defaultBlockState();
                // force replace regardless of what's above, keeps top layer authoritative
                world.setBlock(topPos, replacement, Block.UPDATE_ALL);
            }
        }

        return true;
    }
}

