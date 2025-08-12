package com.theendupdate.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.PlantBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

/**
 * Simple small plant that behaves like warped roots: decorative, no growth.
 * Placement is allowed on any solid-top block to keep things flexible in 1.21.8.
 */
public class MoldSporeBlock extends PlantBlock {

    public static final MapCodec<MoldSporeBlock> CODEC = createCodec(MoldSporeBlock::new);

    // Use a small, low profile shape similar to roots
    private static final VoxelShape OUTLINE_SHAPE = VoxelShapes.cuboid(0.125, 0.0, 0.125, 0.875, 0.375, 0.875);

    public MoldSporeBlock(AbstractBlock.Settings settings) {
        super(settings);
    }

    @Override
    public MapCodec<MoldSporeBlock> getCodec() {
        return CODEC;
    }

    @Override
    protected boolean canPlantOnTop(BlockState floor, BlockView world, BlockPos pos) {
        return Block.isFaceFullSquare(floor.getCollisionShape(world, pos), Direction.UP);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return OUTLINE_SHAPE;
    }
}


