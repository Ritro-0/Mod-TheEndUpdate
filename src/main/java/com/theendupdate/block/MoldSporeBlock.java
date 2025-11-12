package com.theendupdate.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.PlantBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.TallPlantBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.registry.tag.BlockTags;

/**
 * Simple small plant that behaves like warped roots: decorative, no growth.
 * Placement is allowed on any solid-top block to keep things flexible in 1.21.8.
 */
public class MoldSporeBlock extends PlantBlock {

    public static final MapCodec<MoldSporeBlock> CODEC = createCodec(MoldSporeBlock::new);

    // 8px wide (centered) x 12px tall
    private static final VoxelShape OUTLINE_SHAPE = VoxelShapes.cuboid(0.25, 0.0, 0.25, 0.75, 0.75, 0.75);

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

	@Override
	public BlockState getPlacementState(net.minecraft.item.ItemPlacementContext context) {
		// If targeting a flower or tall plant, do not replace it
		BlockState existing = context.getWorld().getBlockState(context.getBlockPos());
		if (existing.isIn(BlockTags.FLOWERS) || existing.getBlock() instanceof TallPlantBlock) {
			return null;
		}
		// Item-level logic handles adjacent-only placement. Do not add extra rejection here.
		return super.getPlacementState(context);
	}

	@Override
	public boolean canReplace(BlockState state, net.minecraft.item.ItemPlacementContext context) {
		// Prevent replacement of this plant by other placements; force adjacency attempts
		return false;
	}

}


