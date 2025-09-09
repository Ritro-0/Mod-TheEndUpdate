package com.theendupdate.world.feature;

import com.mojang.serialization.Codec;
import com.theendupdate.registry.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

/**
 * Shadowlands ground cover: Converts the exposed top End Stone to End Murk wherever there is air above.
 * Runs as a TOP_LAYER_MODIFICATION placed feature.
 */
public class ShadowlandsGroundCoverFeature extends Feature<DefaultFeatureConfig> {
    public ShadowlandsGroundCoverFeature(Codec<DefaultFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
        StructureWorldAccess world = context.getWorld();
        BlockPos origin = context.getOrigin();
        // Random not needed after strict air-only rule

        // Feature is injected only into Shadowlands biomes via BiomeModifications; no extra region scan here

        ChunkPos chunkPos = new ChunkPos(origin);
        int startX = chunkPos.getStartX();
        int startZ = chunkPos.getStartZ();

        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = startX + dx;
                int z = startZ + dz;
                BlockPos topPos = world.getTopPosition(Heightmap.Type.WORLD_SURFACE_WG, new BlockPos(x, 0, z)).down();
                if (topPos.getY() <= world.getBottomY()) continue;

                BlockState current = world.getBlockState(topPos);
                // Shadowlands takes priority over neighboring covers: replace End Stone, End Mire, or Mold Block
                boolean replaceable = current.isOf(Blocks.END_STONE)
                    || current.isOf(ModBlocks.END_MIRE)
                    || current.isOf(ModBlocks.MOLD_BLOCK);
                if (!replaceable) continue;

                // Restrict strictly to Shadowlands biomes placed by The End Biomes API
                var biome = world.getBiome(topPos);
                boolean isShadow = biome.matchesKey(com.theendupdate.registry.ModWorldgen.SHADOWLANDS_HIGHLANDS_KEY)
                    || biome.matchesKey(com.theendupdate.registry.ModWorldgen.SHADOWLANDS_MIDLANDS_KEY)
                    || biome.matchesKey(com.theendupdate.registry.ModWorldgen.SHADOWLANDS_BARRENS_KEY);
                if (!isShadow) continue;

                // Only if exposed (air above) as requested
                if (!world.isAir(topPos.up())) continue;

                world.setBlockState(topPos, ModBlocks.END_MURK.getDefaultState(), Block.NOTIFY_ALL);
            }
        }

        return true;
    }

    // No far-biome sampling to avoid chunk-availability crashes during worldgen
}


