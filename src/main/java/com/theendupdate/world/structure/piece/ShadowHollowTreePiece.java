package com.theendupdate.world.structure.piece;

import com.theendupdate.registry.ModBlocks;
import com.theendupdate.world.ShadowClawTreeGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

public class ShadowHollowTreePiece extends StructurePiece {
    private BlockPos pivot;

    public ShadowHollowTreePiece(BlockPos pivot) {
        super(com.theendupdate.registry.ModStructures.SHADOW_HOLLOW_TREE_PIECE, 0, new BoundingBox(pivot.getX() - 24, pivot.getY() - 8, pivot.getZ() - 24, pivot.getX() + 24, pivot.getY() + 72, pivot.getZ() + 24));
        this.pivot = pivot;
        this.setOrientation(Direction.NORTH);
    }

    public ShadowHollowTreePiece(StructurePieceSerializationContext context, CompoundTag nbt) {
        super(com.theendupdate.registry.ModStructures.SHADOW_HOLLOW_TREE_PIECE, nbt);
        this.pivot = new BlockPos(
            nbt.getInt("px").orElse(0),
            nbt.getInt("py").orElse(0),
            nbt.getInt("pz").orElse(0)
        );
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag nbt) {
        nbt.putInt("px", pivot.getX());
        nbt.putInt("py", pivot.getY());
        nbt.putInt("pz", pivot.getZ());
    }

    @Override
    public void postProcess(WorldGenLevel world, StructureManager structureAccessor, ChunkGenerator chunkGenerator, RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivotPos) {
        // scan 9x9 around pivot for a flat, clear anchor spot
        BlockPos bestSurface = null;
        int bestScore = Integer.MIN_VALUE;

        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                int x = pivot.getX() + dx;
                int z = pivot.getZ() + dz;
                BlockPos colTop = world.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, new BlockPos(x, 0, z)).below();
                if (colTop.getY() <= world.getMinY() + 16) continue;

                var groundState = world.getBlockState(colTop);
                boolean validGround = groundState.is(ModBlocks.END_MURK) || groundState.is(Blocks.END_STONE);
                if (!validGround) continue;

                int y = colTop.getY();
                int flatScore = 0;
                for (int nx = -2; nx <= 2; nx++) {
                    for (int nz = -2; nz <= 2; nz++) {
                        if (Math.abs(nx) + Math.abs(nz) > 3) continue;
                        BlockPos nTop = world.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, new BlockPos(x + nx, 0, z + nz)).below();
                        if (nTop.getY() == y) flatScore++;
                    }
                }

                int score = flatScore;
                if (world.isEmptyBlock(colTop.above())) score += 4;

                if (score > bestScore) {
                    bestScore = score;
                    bestSurface = colTop;
                }
            }
        }

        // no valid spot: leave world untouched, locator ignores starts without an altar
        if (bestSurface == null) {
            return;
        }

        BlockPos trunkBase = bestSurface.above();
        if (!world.isEmptyBlock(trunkBase)) {
            return;
        }

        // generate first (validates trunk clearance), only touch surface once placement succeeds
        boolean placed = ShadowClawTreeGenerator.generateForcedHollow(world, trunkBase, random);
        if (!placed) {
            return;
        }

        com.theendupdate.world.feature.ShadowClawScatterFeature.placeThicket(world, trunkBase, 20, 0.94f, random, box);

        if (world.getBlockState(bestSurface).is(Blocks.END_STONE)) {
            world.setBlock(bestSurface, ModBlocks.END_MURK.defaultBlockState(), 3);
        }
    }
}
