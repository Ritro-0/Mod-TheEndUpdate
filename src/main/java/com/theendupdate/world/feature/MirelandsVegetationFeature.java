package com.theendupdate.world.feature;

import com.mojang.serialization.Codec;
import com.theendupdate.registry.ModBlocks;
import com.theendupdate.registry.ModWorldgen;
import net.minecraft.block.BlockState;
import net.minecraft.block.Block;
import net.minecraft.block.TallPlantBlock;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

/**
 * Scatters mold vegetation on Mirelands islands. Places a mix of mold_spore, mold_spore_tuft,
 * and mold_spore_sprout atop end_mire and mold_block. Designed to feel dense but passable.
 */
public class MirelandsVegetationFeature extends Feature<DefaultFeatureConfig> {
    public MirelandsVegetationFeature(Codec<DefaultFeatureConfig> codec) {
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
                // Per-column chance to try placing vegetation; tune for density
                if (random.nextFloat() > 0.35f) {
                    continue;
                }

                int x = startX + dx;
                int z = startZ + dz;
                BlockPos surface = world.getTopPosition(Heightmap.Type.WORLD_SURFACE_WG, new BlockPos(x, 0, z)).down();
                if (surface.getY() <= world.getBottomY()) {
                    continue;
                }

                // Only on exposed top of our ground cover
                BlockPos placePos = surface.up();
                if (!world.isAir(placePos)) {
                    continue;
                }

                BlockState ground = world.getBlockState(surface);
                if (!(ground.isOf(ModBlocks.END_MIRE) || ground.isOf(ModBlocks.MOLD_BLOCK))) {
                    continue;
                }

                // Verify the ground block is actually solid and supported (not floating in air above a crater)
                if (!ground.isSolid() || ground.isAir()) {
                    continue;
                }
                BlockPos groundBelow = surface.down();
                BlockState groundBelowState = world.getBlockState(groundBelow);
                // If there's air below the ground block, we might be over a crater - skip placement
                if (groundBelowState.isAir() && surface.getY() > world.getBottomY() + 5) {
                    continue;
                }

                // Restrict to our Mirelands biomes for safety
                RegistryEntry<Biome> biome = world.getBiome(surface);
                boolean isMire = biome.matchesKey(ModWorldgen.MIRELANDS_HIGHLANDS_KEY)
                    || biome.matchesKey(ModWorldgen.MIRELANDS_MIDLANDS_KEY)
                    || biome.matchesKey(ModWorldgen.MIRELANDS_BARRENS_KEY);
                if (!isMire) {
                    continue;
                }

                // Choose which plant to place
                float roll = random.nextFloat();
                if (roll < 0.45f) {
                    // mold_spore (small)
                    BlockState state = ModBlocks.MOLD_SPORE.getDefaultState();
                    if (state.canPlaceAt(world, placePos)) {
                        world.setBlockState(placePos, state, Block.NOTIFY_LISTENERS);
                    }
                } else if (roll < 0.85f) {
                    // mold_spore_tuft (small, bushier)
                    BlockState state = ModBlocks.MOLD_SPORE_TUFT.getDefaultState();
                    if (state.canPlaceAt(world, placePos)) {
                        world.setBlockState(placePos, state, Block.NOTIFY_LISTENERS);
                    }
                } else {
                    // mold_spore_sprout (double tall)
                    if (world.isAir(placePos.up())) {
                        BlockState state = ModBlocks.MOLD_SPORE_SPROUT.getDefaultState();
                        if (state.canPlaceAt(world, placePos)) {
                            TallPlantBlock.placeAt(world, state, placePos, Block.NOTIFY_LISTENERS);
                        }
                    }
                }
            }
        }

        return true;
    }
}


