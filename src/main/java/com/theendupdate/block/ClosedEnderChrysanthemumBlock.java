package com.theendupdate.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.PlantBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Property;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldView;

public class ClosedEnderChrysanthemumBlock extends PlantBlock {
	public static final MapCodec<ClosedEnderChrysanthemumBlock> CODEC = createCodec(ClosedEnderChrysanthemumBlock::new);
	public static final Property<Direction> ATTACHMENT_FACE = Properties.FACING;

	// Dimensions: 6x14 px (width x height)
	private static final double WIDTH = 6.0 / 16.0;      // 0.375
	private static final double HALF = WIDTH / 2.0;       // 0.1875
	private static final double HEIGHT = 14.0 / 16.0;     // 0.875
	private static final double MIN = 0.5 - HALF;         // 0.3125
	private static final double MAX = 0.5 + HALF;         // 0.6875

	private static final VoxelShape SHAPE_DOWN = VoxelShapes.cuboid(MIN, 0.0, MIN, MAX, HEIGHT, MAX);
	private static final VoxelShape SHAPE_UP = VoxelShapes.cuboid(MIN, 1.0 - HEIGHT, MIN, MAX, 1.0, MAX);
	private static final VoxelShape SHAPE_NORTH = VoxelShapes.cuboid(MIN, 0.5 - HALF, 0.0, MAX, 0.5 + HALF, HEIGHT);
	private static final VoxelShape SHAPE_SOUTH = VoxelShapes.cuboid(MIN, 0.5 - HALF, 1.0 - HEIGHT, MAX, 0.5 + HALF, 1.0);
	private static final VoxelShape SHAPE_WEST = VoxelShapes.cuboid(0.0, 0.5 - HALF, MIN, HEIGHT, 0.5 + HALF, MAX);
	private static final VoxelShape SHAPE_EAST = VoxelShapes.cuboid(1.0 - HEIGHT, 0.5 - HALF, MIN, 1.0, 0.5 + HALF, MAX);

	public ClosedEnderChrysanthemumBlock(Settings settings) {
		super(settings);
		this.setDefaultState(this.stateManager.getDefaultState()
			.with(ATTACHMENT_FACE, Direction.DOWN));
	}

	@Override
	public MapCodec<? extends PlantBlock> getCodec() {
		return CODEC;
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(ATTACHMENT_FACE);
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext context) {
		Direction clickedFace = context.getSide();
		Direction attachmentDirection = clickedFace.getOpposite();
		
		BlockState candidate = this.getDefaultState()
			.with(ATTACHMENT_FACE, attachmentDirection);
		return candidate.canPlaceAt(context.getWorld(), context.getBlockPos()) ? candidate : null;
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		Direction face = state.get(ATTACHMENT_FACE);
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
	public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
		Direction attachmentDirection = state.get(ATTACHMENT_FACE);
		BlockPos supportPos = pos.offset(attachmentDirection);
		BlockState supportState = world.getBlockState(supportPos);
		
		// Check if the supporting block can support the flower
		return supportState.isSideSolidFullSquare(world, supportPos, attachmentDirection.getOpposite());
	}
	
	@Override
	public void onPlaced(net.minecraft.world.World world, BlockPos pos, BlockState state, net.minecraft.entity.LivingEntity placer, net.minecraft.item.ItemStack itemStack) {
		super.onPlaced(world, pos, state, placer, itemStack);
		if (!world.isClient()) {
			com.theendupdate.network.EnderChrysanthemumCloser.addClosedPositionManually((net.minecraft.server.world.ServerWorld) world, pos);
		}
	}

}

