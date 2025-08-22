package com.theendupdate.world.feature;

import com.mojang.serialization.Codec;
import com.theendupdate.block.EnderChrysanthemumBlock;
import com.theendupdate.registry.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

/**
 * Scans exposed faces of End Stone within the chunk and attaches Ender Chrysanthemums
 * to any air-exposed faces with a ~0.8% chance per face. Intended for SMALL_END_ISLANDS.
 */
public class EnderChrysanthemumIslandsFeature extends Feature<DefaultFeatureConfig> {
    private static final float PER_FACE_CHANCE = 0.008f; // 0.8%

    public EnderChrysanthemumIslandsFeature(Codec<DefaultFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
        StructureWorldAccess world = context.getWorld();
        Random random = context.getRandom();

        ChunkPos chunkPos = new ChunkPos(context.getOrigin());
        int startX = chunkPos.getStartX();
        int startZ = chunkPos.getStartZ();

        int bottomY = world.getBottomY();
        int topYExclusive = bottomY + world.getHeight();

        boolean placedAny = false;

        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = startX + dx;
                int z = startZ + dz;

                for (int y = bottomY; y < topYExclusive; y++) {
                    BlockPos stonePos = new BlockPos(x, y, z);
                    BlockState stoneState = world.getBlockState(stonePos);
                    if (!stoneState.isOf(Blocks.END_STONE)) {
                        continue;
                    }

                    for (Direction dir : Direction.values()) {
                        BlockPos placePos = stonePos.offset(dir);
                        if (!world.getBlockState(placePos).isAir()) {
                            continue;
                        }

                        // Restrict strictly to SMALL_END_ISLANDS biome columns
                        RegistryEntry<Biome> biomeEntry = world.getBiome(placePos);
                        if (!biomeEntry.matchesKey(BiomeKeys.SMALL_END_ISLANDS)) {
                            continue;
                        }

                        if (random.nextFloat() >= PER_FACE_CHANCE) {
                            continue;
                        }

                        BlockState attached = ModBlocks.ENDER_CHRYSANTHEMUM.getDefaultState()
                            .with(EnderChrysanthemumBlock.ATTACHMENT_FACE, dir.getOpposite());

                        if (attached.canPlaceAt(world, placePos)) {
                            world.setBlockState(placePos, attached, Block.NOTIFY_LISTENERS);
                            placedAny = true;
                        }
                    }
                }
            }
        }

        return placedAny;
    }
}


