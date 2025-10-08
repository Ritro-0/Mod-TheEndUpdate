package com.theendupdate.world.structure.piece;

import com.theendupdate.registry.ModBlocks;
import com.theendupdate.world.ShadowClawTreeGenerator;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.StructureContext;
import net.minecraft.structure.StructurePiece;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.math.ChunkPos;

public class ShadowHollowTreePiece extends StructurePiece {
    private BlockPos pivot;

    public ShadowHollowTreePiece(BlockPos pivot) {
        super(com.theendupdate.registry.ModStructures.SHADOW_HOLLOW_TREE_PIECE, 0, new BlockBox(pivot.getX() - 16, pivot.getY(), pivot.getZ() - 16, pivot.getX() + 16, pivot.getY() + 64, pivot.getZ() + 16));
        this.pivot = pivot;
        this.setOrientation(Direction.NORTH);
    }

    public ShadowHollowTreePiece(StructureContext context, NbtCompound nbt) {
        super(com.theendupdate.registry.ModStructures.SHADOW_HOLLOW_TREE_PIECE, nbt);
        this.pivot = new BlockPos(
            nbt.getInt("px").orElse(0),
            nbt.getInt("py").orElse(0),
            nbt.getInt("pz").orElse(0)
        );
    }

    @Override
    protected void writeNbt(StructureContext context, NbtCompound nbt) {
        nbt.putInt("px", pivot.getX());
        nbt.putInt("py", pivot.getY());
        nbt.putInt("pz", pivot.getZ());
    }

    @Override
    public void generate(StructureWorldAccess world, StructureAccessor structureAccessor, ChunkGenerator chunkGenerator, Random random, BlockBox box, ChunkPos chunkPos, BlockPos pivotPos) {
        // Search a 9x9 area around the chunk center for a suitable anchor to prevent floating/ghost placements
        BlockPos bestSurface = null;
        int bestScore = Integer.MIN_VALUE;

        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                int x = pivot.getX() + dx;
                int z = pivot.getZ() + dz;
                BlockPos colTop = world.getTopPosition(net.minecraft.world.Heightmap.Type.WORLD_SURFACE_WG, new BlockPos(x, 0, z)).down();
                if (colTop.getY() <= world.getBottomY()) continue;

                // Require ground to be END_STONE or END_MURK
                var groundState = world.getBlockState(colTop);
                boolean validGround = groundState.isOf(ModBlocks.END_MURK) || groundState.isOf(net.minecraft.block.Blocks.END_STONE);
                if (!validGround) continue;

                // Compute local flatness score: count how many neighbors at same Y within 2-block radius
                int y = colTop.getY();
                int flatScore = 0;
                for (int nx = -2; nx <= 2; nx++) {
                    for (int nz = -2; nz <= 2; nz++) {
                        if (Math.abs(nx) + Math.abs(nz) > 3) continue; // diamond neighborhood
                        BlockPos nTop = world.getTopPosition(net.minecraft.world.Heightmap.Type.WORLD_SURFACE_WG, new BlockPos(x + nx, 0, z + nz)).down();
                        if (nTop.getY() == y) flatScore++;
                    }
                }

                // Prefer positions with air above for trunk base
                int score = flatScore;
                if (world.isAir(colTop.up())) score += 4;

                if (score > bestScore) {
                    bestScore = score;
                    bestSurface = colTop;
                }
            }
        }

        if (bestSurface == null) return; // No viable anchor in this chunk area

        // Normalize ground to End Murk at the exact surface spot to match biome visuals
        if (world.getBlockState(bestSurface).isOf(net.minecraft.block.Blocks.END_STONE)) {
            world.setBlockState(bestSurface, ModBlocks.END_MURK.getDefaultState(), 3);
        }

        BlockPos trunkBase = bestSurface.up();
        if (!world.isAir(trunkBase)) return; // avoid generating into obstructions

        ShadowClawTreeGenerator.generateForcedHollow(world, trunkBase, random);
    }
}


