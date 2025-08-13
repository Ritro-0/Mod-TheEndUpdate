package com.theendupdate.world.feature;

import com.mojang.serialization.Codec;
import com.theendupdate.registry.ModBlocks;
import com.theendupdate.registry.ModWorldgen;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.Block;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;
import net.minecraft.util.math.random.Random;

/**
 * Replaces the top exposed End Stone on Mirelands islands with 80% End Mire and 20% Mold Block.
 * Runs as a TOP_LAYER_MODIFICATION placed feature.
 */
public class MirelandsGroundCoverFeature extends Feature<DefaultFeatureConfig> {
    public MirelandsGroundCoverFeature(Codec<DefaultFeatureConfig> codec) {
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

        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = startX + dx;
                int z = startZ + dz;
                BlockPos topPos = world.getTopPosition(Heightmap.Type.WORLD_SURFACE_WG, new BlockPos(x, 0, z)).down();
                if (topPos.getY() <= world.getBottomY()) {
                    continue;
                }

                BlockState current = world.getBlockState(topPos);
                if (!current.isOf(Blocks.END_STONE)) {
                    continue;
                }

                // Only within our Mirelands biome set
                RegistryEntry<Biome> biome = world.getBiome(topPos);
                boolean isMire = biome.matchesKey(ModWorldgen.MIRELANDS_HIGHLANDS_KEY)
                    || biome.matchesKey(ModWorldgen.MIRELANDS_MIDLANDS_KEY)
                    || biome.matchesKey(ModWorldgen.MIRELANDS_BARRENS_KEY);
                if (!isMire) {
                    continue;
                }

                boolean placeMire = random.nextFloat() < 0.8f;
                BlockState replacement = placeMire ? ModBlocks.END_MIRE.getDefaultState() : ModBlocks.MOLD_BLOCK.getDefaultState();
                // Force replace regardless of what is above; this keeps the top layer authoritative
                world.setBlockState(topPos, replacement, Block.NOTIFY_ALL);
            }
        }

        return true;
    }
}

