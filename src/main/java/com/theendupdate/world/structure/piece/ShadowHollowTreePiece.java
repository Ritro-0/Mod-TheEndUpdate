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
        BlockPos surface = world.getTopPosition(net.minecraft.world.Heightmap.Type.WORLD_SURFACE_WG, new BlockPos(pivot.getX(), 0, pivot.getZ())).down();
        if (surface.getY() <= world.getBottomY()) return;
        if (!(world.getBlockState(surface).isOf(ModBlocks.END_MURK) || world.getBlockState(surface).isOf(net.minecraft.block.Blocks.END_STONE))) return;
        if (world.getBlockState(surface).isOf(net.minecraft.block.Blocks.END_STONE)) {
            world.setBlockState(surface, ModBlocks.END_MURK.getDefaultState(), 3);
        }
        BlockPos trunkBase = surface.up();
        ShadowClawTreeGenerator.generateForcedHollow(world, trunkBase, random);
    }
}


