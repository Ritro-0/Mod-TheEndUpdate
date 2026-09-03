package com.theendupdate.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ClosedEnderChrysanthemumBlock extends VegetationBlock {
	public static final MapCodec<ClosedEnderChrysanthemumBlock> CODEC = simpleCodec(ClosedEnderChrysanthemumBlock::new);
	public static final Property<Direction> ATTACHMENT_FACE = BlockStateProperties.FACING;

	// Dimensions: 6x14 px (width x height)
	private static final double WIDTH = 6.0 / 16.0;      // 0.375
	private static final double HALF = WIDTH / 2.0;       // 0.1875
	private static final double HEIGHT = 14.0 / 16.0;     // 0.875
	private static final double MIN = 0.5 - HALF;         // 0.3125
	private static final double MAX = 0.5 + HALF;         // 0.6875

	private static final VoxelShape SHAPE_DOWN = Shapes.box(MIN, 0.0, MIN, MAX, HEIGHT, MAX);
	private static final VoxelShape SHAPE_UP = Shapes.box(MIN, 1.0 - HEIGHT, MIN, MAX, 1.0, MAX);
	private static final VoxelShape SHAPE_NORTH = Shapes.box(MIN, 0.5 - HALF, 0.0, MAX, 0.5 + HALF, HEIGHT);
	private static final VoxelShape SHAPE_SOUTH = Shapes.box(MIN, 0.5 - HALF, 1.0 - HEIGHT, MAX, 0.5 + HALF, 1.0);
	private static final VoxelShape SHAPE_WEST = Shapes.box(0.0, 0.5 - HALF, MIN, HEIGHT, 0.5 + HALF, MAX);
	private static final VoxelShape SHAPE_EAST = Shapes.box(1.0 - HEIGHT, 0.5 - HALF, MIN, 1.0, 0.5 + HALF, MAX);

	public ClosedEnderChrysanthemumBlock(Properties settings) {
		super(settings);
		this.registerDefaultState(this.stateDefinition.any()
			.setValue(ATTACHMENT_FACE, Direction.DOWN));
	}

	@Override
	public MapCodec<? extends VegetationBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(ATTACHMENT_FACE);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction clickedFace = context.getClickedFace();
		Direction attachmentDirection = clickedFace.getOpposite();
		
		BlockState candidate = this.defaultBlockState()
			.setValue(ATTACHMENT_FACE, attachmentDirection);
		return candidate.canSurvive(context.getLevel(), context.getClickedPos()) ? candidate : null;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		Direction face = state.getValue(ATTACHMENT_FACE);
		return switch (face) {
			case DOWN -> SHAPE_DOWN;
			case UP -> SHAPE_UP;
			case NORTH -> SHAPE_NORTH;
			case SOUTH -> SHAPE_SOUTH;
			case WEST -> SHAPE_WEST;
			case EAST -> SHAPE_EAST;
		};
	}

	@Override
	public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
		Direction attachmentDirection = state.getValue(ATTACHMENT_FACE);
		BlockPos supportPos = pos.relative(attachmentDirection);
		BlockState supportState = world.getBlockState(supportPos);
		return supportState.isFaceSturdy(world, supportPos, attachmentDirection.getOpposite());
	}
	
	@Override
	public void setPlacedBy(net.minecraft.world.level.Level world, BlockPos pos, BlockState state, net.minecraft.world.entity.LivingEntity placer, net.minecraft.world.item.ItemStack itemStack) {
		super.setPlacedBy(world, pos, state, placer, itemStack);
		if (!world.isClientSide()) {
			com.theendupdate.network.EnderChrysanthemumCloser.addClosedPositionManually((net.minecraft.server.level.ServerLevel) world, pos);
		}
	}

}

