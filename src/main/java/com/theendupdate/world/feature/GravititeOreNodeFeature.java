package com.theendupdate.world.feature;

import com.theendupdate.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Places single Gravitite Ore nodes that are fully encased in End Stone.
 *
 * Spacing is enforced with a chunk grid so we never read outside the worldgen
 * write radius (cross-chunk cube scans caused severe lag in 26.2).
 *
 * Density target: ~0.5 nodes per chunk on average across eligible chunks.
 */
public class GravititeOreNodeFeature extends Feature<NoneFeatureConfiguration> {
    /** Place candidates only in every Nth chunk along X/Z (~32 block spacing). */
    private static final int CHUNK_SPACING = 2;

    public GravititeOreNodeFeature(com.mojang.serialization.Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel world = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();

        ChunkPos chunkPos = ChunkPos.containing(origin);
        if (Math.floorMod(chunkPos.x(), CHUNK_SPACING) != 0
            || Math.floorMod(chunkPos.z(), CHUNK_SPACING) != 0) {
            return false;
        }

        // probabilistic gate, averages ~0.5 nodes per eligible chunk
        if (random.nextFloat() > 0.5f) {
            return false;
        }

        int startX = chunkPos.getMinBlockX();
        int startZ = chunkPos.getMinBlockZ();
        int bottomY = world.getMinY();
        int topY = world.getHeight();

        // at most one node per eligible chunk, scan stays inside this chunk
        if (chunkContainsGravitite(world, chunkPos, bottomY, topY)) {
            return false;
        }

        // try several samples for a valid fully-encased spot, this chunk only
        final int maxAttempts = 48;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            int x = startX + random.nextInt(16);
            int z = startZ + random.nextInt(16);
            int y = bottomY + 8 + random.nextInt(Math.max(1, (topY - bottomY) - 16));

            BlockPos pos = new BlockPos(x, y, z);
            if (!world.ensureCanWrite(pos)) {
                continue;
            }
            if (!isFullyEncasedInEndStone(world, pos)) {
                continue;
            }

            world.setBlock(pos, ModBlocks.GRAVITITE_ORE.defaultBlockState(), Block.UPDATE_CLIENTS);
            return true;
        }
        return false;
    }

    private static boolean isFullyEncasedInEndStone(WorldGenLevel world, BlockPos pos) {
        BlockState center = world.getBlockState(pos);
        if (!center.is(Blocks.END_STONE)) return false;
        for (Direction d : Direction.values()) {
            BlockPos neighbor = pos.relative(d);
            // unsafe edge read, treat as not encased
            if (!world.ensureCanWrite(neighbor)) return false;
            BlockState s = world.getBlockState(neighbor);
            if (!s.is(Blocks.END_STONE)) return false;
        }
        return true;
    }

    /** Only scan the current chunk — never touches neighboring chunks. */
    private static boolean chunkContainsGravitite(WorldGenLevel world, ChunkPos chunkPos, int bottomY, int topY) {
        int minX = chunkPos.getMinBlockX();
        int minZ = chunkPos.getMinBlockZ();
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        // sparse scan, every 4 blocks is enough to catch a prior placement here
        for (int x = minX; x < minX + 16; x += 4) {
            for (int z = minZ; z < minZ + 16; z += 4) {
                for (int y = bottomY + 8; y < topY - 8; y += 4) {
                    mutable.set(x, y, z);
                    if (!world.ensureCanWrite(mutable)) continue;
                    if (world.getBlockState(mutable).is(ModBlocks.GRAVITITE_ORE)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
