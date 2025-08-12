package com.theendupdate.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.TallPlantBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

public class MoldSporeSproutBlock extends TallPlantBlock {
    public static final MapCodec<MoldSporeSproutBlock> CODEC = createCodec(MoldSporeSproutBlock::new);

    private static final VoxelShape OUTLINE_SHAPE = VoxelShapes.cuboid(0.125, 0.0, 0.125, 0.875, 1.0, 0.875);

    public MoldSporeSproutBlock(AbstractBlock.Settings settings) {
        super(settings);
    }

    @Override
    public MapCodec<MoldSporeSproutBlock> getCodec() {
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


