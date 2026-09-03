package com.theendupdate.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class MoldSporeTuftBlock extends VegetationBlock {
    public static final MapCodec<MoldSporeTuftBlock> CODEC = simpleCodec(MoldSporeTuftBlock::new);

    // 14px wide (centered) x 15px tall
    private static final VoxelShape OUTLINE_SHAPE = Shapes.box(0.125, 0.0, 0.125, 0.875, 0.9375, 0.875);

    public MoldSporeTuftBlock(BlockBehaviour.Properties settings) {
        super(settings);
    }

    @Override
    public MapCodec<MoldSporeTuftBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean mayPlaceOn(BlockState floor, BlockGetter world, BlockPos pos) {
        return Block.isFaceFull(floor.getCollisionShape(world, pos), Direction.UP);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return OUTLINE_SHAPE;
    }

	@Override
	public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
		BlockState existing = context.getLevel().getBlockState(context.getClickedPos());
		if (existing.is(BlockTags.FLOWERS) || existing.getBlock() instanceof DoublePlantBlock) {
			return null;
		}
		// adjacent-only placement is handled at the item level, don't add extra rejection here
		return super.getStateForPlacement(context);
	}

	@Override
	public boolean canBeReplaced(BlockState state, net.minecraft.world.item.context.BlockPlaceContext context) {
		// keeps this from being replaced directly - callers fall back to placing adjacent instead
		return false;
	}

}


