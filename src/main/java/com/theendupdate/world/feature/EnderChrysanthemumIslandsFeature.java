package com.theendupdate.world.feature;

import com.mojang.serialization.MapCodec;
import com.theendupdate.block.EnderChrysanthemumBlock;
import com.theendupdate.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * Scans exposed faces of End Stone within the chunk and attaches Ender Chrysanthemums
 * to any air-exposed faces with a ~0.8% chance per face. Intended for SMALL_END_ISLANDS.
 */
public class EnderChrysanthemumIslandsFeature implements Feature {
    private static final float PER_FACE_CHANCE = 0.008f; // 0.8%

    public static final MapCodec<EnderChrysanthemumIslandsFeature> CODEC = MapCodec.unit(EnderChrysanthemumIslandsFeature::new);

    public EnderChrysanthemumIslandsFeature() {}

    @Override
    public MapCodec<EnderChrysanthemumIslandsFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel world, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        ChunkPos chunkPos = ChunkPos.containing(origin);
        int startX = chunkPos.getMinBlockX();
        int startZ = chunkPos.getMinBlockZ();

        int bottomY = world.getMinY();
        int topYExclusive = bottomY + world.getHeight();

        boolean placedAny = false;

        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = startX + dx;
                int z = startZ + dz;

                for (int y = bottomY; y < topYExclusive; y++) {
                    BlockPos stonePos = new BlockPos(x, y, z);
                    BlockState stoneState = world.getBlockState(stonePos);
                    if (!stoneState.is(Blocks.END_STONE)) {
                        continue;
                    }

                    for (Direction dir : Direction.values()) {
                        BlockPos placePos = stonePos.relative(dir);
                        if (!world.getBlockState(placePos).isAir()) {
                            continue;
                        }

                        // SMALL_END_ISLANDS biome only
                        Holder<Biome> biomeEntry = world.getBiome(placePos);
                        if (!biomeEntry.is(Biomes.SMALL_END_ISLANDS)) {
                            continue;
                        }

                        if (random.nextFloat() >= PER_FACE_CHANCE) {
                            continue;
                        }

                        BlockState attached = ModBlocks.ENDER_CHRYSANTHEMUM.defaultBlockState()
                            .setValue(EnderChrysanthemumBlock.ATTACHMENT_FACE, dir.getOpposite());

                        if (attached.canSurvive(world, placePos)) {
                            world.setBlock(placePos, attached, Block.UPDATE_CLIENTS);
                            placedAny = true;
                        }
                    }
                }
            }
        }

        return placedAny;
    }
}


