package com.theendupdate.world.feature;

import com.mojang.serialization.Codec;
import com.theendupdate.registry.ModBlocks;
import com.theendupdate.world.OuterEndLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Loosely scatters Shadow Claw plants across Shadowlands surfaces.
 */
public class ShadowClawScatterFeature extends Feature<NoneFeatureConfiguration> {
    public ShadowClawScatterFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel world = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();

        ChunkPos chunkPos = ChunkPos.containing(origin);
        int startX = chunkPos.getMinBlockX();
        int startZ = chunkPos.getMinBlockZ();

        boolean any = false;

        // injected only into Shadowlands biomes via BiomeModifications, no region scan needed here

        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = startX + dx;
                int z = startZ + dz;
                if (random.nextFloat() > OuterEndLayout.shadowClawChance(x, z)) continue;
                BlockPos surface = world.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, new BlockPos(x, 0, z)).below();
                if (surface.getY() <= world.getMinY()) continue;
                if (!world.isEmptyBlock(surface.above())) continue;

                // strictly Shadowlands biomes only (End Biomes API)
                var biome = world.getBiome(surface);
                boolean isShadow = biome.is(com.theendupdate.registry.ModWorldgen.SHADOWLANDS_HIGHLANDS_KEY)
                    || biome.is(com.theendupdate.registry.ModWorldgen.SHADOWLANDS_MIDLANDS_KEY)
                    || biome.is(com.theendupdate.registry.ModWorldgen.SHADOWLANDS_BARRENS_KEY);
                if (!isShadow) continue;

                BlockState ground = world.getBlockState(surface);
                if (!(ground.is(ModBlocks.END_MURK) || ground.is(net.minecraft.world.level.block.Blocks.END_STONE))) continue;

                BlockPos place = surface.above();
                // randomize variant so all four appear naturally
                int variant = random.nextIntBetweenInclusive(0, 3);
                BlockState claw = ModBlocks.SHADOW_CLAW.defaultBlockState().setValue(com.theendupdate.block.ShadowClawBlock.VARIANT, variant);
                if (claw.canSurvive(world, place) && !wouldCompleteFlat3x3(world, place)) {
                    // swap End Stone for End Murk under natural spawns
                    if (ground.is(net.minecraft.world.level.block.Blocks.END_STONE)) {
                        world.setBlock(surface, ModBlocks.END_MURK.defaultBlockState(), 3);
                    }
                    world.setBlock(place, claw, 3);
                    any = true;
                }
            }
        }

        return any;
    }

    /** Packs Shadow Claws densely around a hollow tree / altar. */
    public static void placeThicket(LevelAccessor world, BlockPos origin, int radius, float chance, RandomSource random) {
        placeThicket(world, origin, radius, chance, random, null);
    }

    public static void placeThicket(
        LevelAccessor world,
        BlockPos origin,
        int radius,
        float chance,
        RandomSource random,
        BoundingBox box
    ) {
        int r2 = radius * radius;
        int inner = 3;
        int inner2 = inner * inner;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int dist2 = dx * dx + dz * dz;
                if (dist2 > r2 || dist2 < inner2) {
                    continue;
                }
                if (random.nextFloat() > chance) {
                    continue;
                }
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;
                if (box != null && (x < box.minX() || x > box.maxX() || z < box.minZ() || z > box.maxZ())) {
                    continue;
                }
                if (!world.hasChunk(x >> 4, z >> 4)) {
                    continue;
                }
                int y = world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                BlockPos surface = new BlockPos(x, y, z);
                if (surface.getY() <= world.getMinY()) {
                    continue;
                }
                if (box != null && (surface.getY() < box.minY() || surface.getY() > box.maxY())) {
                    continue;
                }
                BlockState ground = world.getBlockState(surface);
                if (!(ground.is(ModBlocks.END_MURK) || ground.is(Blocks.END_STONE))) {
                    continue;
                }
                BlockPos place = surface.above();
                if (!world.isEmptyBlock(place)) {
                    continue;
                }
                int variant = random.nextIntBetweenInclusive(0, 3);
                BlockState claw = ModBlocks.SHADOW_CLAW.defaultBlockState()
                    .setValue(com.theendupdate.block.ShadowClawBlock.VARIANT, variant);
                if (!claw.canSurvive(world, place) || wouldCompleteFlat3x3(world, place)) {
                    continue;
                }
                if (ground.is(Blocks.END_STONE)) {
                    world.setBlock(surface, ModBlocks.END_MURK.defaultBlockState(), 3);
                }
                world.setBlock(place, claw, 3);
            }
        }
    }

    /**
     * Trees grow from a same-Y 3x3 of Shadow Claws. Worldgen never places that last claw.
     */
    private static boolean wouldCompleteFlat3x3(LevelAccessor world, BlockPos pos) {
        for (int ox = -2; ox <= 0; ox++) {
            for (int oz = -2; oz <= 0; oz++) {
                if (isFull3x3Except(world, pos.offset(ox, 0, oz), pos)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isFull3x3Except(LevelAccessor world, BlockPos nw, BlockPos except) {
        for (int dx = 0; dx < 3; dx++) {
            for (int dz = 0; dz < 3; dz++) {
                BlockPos check = nw.offset(dx, 0, dz);
                if (check.equals(except)) {
                    continue;
                }
                if (!world.hasChunk(check.getX() >> 4, check.getZ() >> 4)) {
                    return false;
                }
                if (!world.getBlockState(check).is(ModBlocks.SHADOW_CLAW)) {
                    return false;
                }
            }
        }
        return true;
    }
}


