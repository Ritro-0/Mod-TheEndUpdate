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
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Scatters mold vegetation on Mirelands islands. Places a mix of mold_spore, mold_spore_tuft,
 * and mold_spore_sprout atop end_mire and mold_block. Designed to feel dense but passable.
 */
public class MirelandsVegetationFeature extends Feature<NoneFeatureConfiguration> {
    public MirelandsVegetationFeature(Codec<NoneFeatureConfiguration> codec) {
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
                // per-column chance, tune this for density
                if (random.nextFloat() > 0.35f) {
                    continue;
                }

                int x = startX + dx;
                int z = startZ + dz;
                BlockPos surface = world.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, new BlockPos(x, 0, z)).below();
                if (surface.getY() <= world.getMinY()) {
                    continue;
                }

                BlockPos placePos = surface.above();
                if (!world.isEmptyBlock(placePos)) {
                    continue;
                }

                BlockState ground = world.getBlockState(surface);
                if (!(ground.is(ModBlocks.END_MIRE) || ground.is(ModBlocks.MOLD_BLOCK))) {
                    continue;
                }

                // ground must be solid and supported, not floating over a crater
                if (!ground.isSolid() || ground.isAir()) {
                    continue;
                }
                BlockPos groundBelow = surface.below();
                BlockState groundBelowState = world.getBlockState(groundBelow);
                if (groundBelowState.isAir() && surface.getY() > world.getMinY() + 5) {
                    continue;
                }

                Holder<Biome> biome = world.getBiome(surface);
                boolean isMire = biome.is(ModWorldgen.MIRELANDS_HIGHLANDS_KEY)
                    || biome.is(ModWorldgen.MIRELANDS_MIDLANDS_KEY)
                    || biome.is(ModWorldgen.MIRELANDS_BARRENS_KEY);
                if (!isMire) {
                    continue;
                }

                float roll = random.nextFloat();
                if (roll < 0.45f) {
                    // mold_spore (small)
                    BlockState state = ModBlocks.MOLD_SPORE.defaultBlockState();
                    if (state.canSurvive(world, placePos)) {
                        world.setBlock(placePos, state, Block.UPDATE_CLIENTS);
                    }
                } else if (roll < 0.85f) {
                    // mold_spore_tuft (small, bushier)
                    BlockState state = ModBlocks.MOLD_SPORE_TUFT.defaultBlockState();
                    if (state.canSurvive(world, placePos)) {
                        world.setBlock(placePos, state, Block.UPDATE_CLIENTS);
                    }
                } else {
                    // mold_spore_sprout (double tall)
                    if (world.isEmptyBlock(placePos.above())) {
                        BlockState state = ModBlocks.MOLD_SPORE_SPROUT.defaultBlockState();
                        if (state.canSurvive(world, placePos)) {
                            DoublePlantBlock.placeAt(world, state, placePos, Block.UPDATE_CLIENTS);
                        }
                    }
                }
            }
        }

        return true;
    }
}


