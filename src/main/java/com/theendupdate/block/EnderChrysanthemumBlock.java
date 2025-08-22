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
import net.minecraft.world.WorldAccess;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldView;

public class EnderChrysanthemumBlock extends PlantBlock {
	public static final MapCodec<EnderChrysanthemumBlock> CODEC = createCodec(EnderChrysanthemumBlock::new);
	// Direction of the supporting face (direction from the flower toward the supporting block)
	public static final Property<Direction> ATTACHMENT_FACE = Properties.FACING;

	// Dimensions: 10x14 px (width x height)
	private static final double WIDTH = 10.0 / 16.0;      // 0.625
	private static final double HALF = WIDTH / 2.0;       // 0.3125
	private static final double HEIGHT = 14.0 / 16.0;     // 0.875
	private static final double MIN = 0.5 - HALF;         // 0.1875
	private static final double MAX = 0.5 + HALF;         // 0.8125

	private static final VoxelShape SHAPE_DOWN = VoxelShapes.cuboid(MIN, 0.0, MIN, MAX, HEIGHT, MAX);
	private static final VoxelShape SHAPE_UP = VoxelShapes.cuboid(MIN, 1.0 - HEIGHT, MIN, MAX, 1.0, MAX);
	private static final VoxelShape SHAPE_NORTH = VoxelShapes.cuboid(MIN, 0.5 - HALF, 0.0, MAX, 0.5 + HALF, HEIGHT);
	private static final VoxelShape SHAPE_SOUTH = VoxelShapes.cuboid(MIN, 0.5 - HALF, 1.0 - HEIGHT, MAX, 0.5 + HALF, 1.0);
	private static final VoxelShape SHAPE_WEST = VoxelShapes.cuboid(0.0, 0.5 - HALF, MIN, HEIGHT, 0.5 + HALF, MAX);
	private static final VoxelShape SHAPE_EAST = VoxelShapes.cuboid(1.0 - HEIGHT, 0.5 - HALF, MIN, 1.0, 0.5 + HALF, MAX);

	public EnderChrysanthemumBlock(Settings settings) {
		super(settings);
		this.setDefaultState(this.stateManager.getDefaultState().with(ATTACHMENT_FACE, Direction.DOWN));
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
		BlockState candidate = this.getDefaultState().with(ATTACHMENT_FACE, attachmentDirection);
		return candidate.canPlaceAt(context.getWorld(), context.getBlockPos()) ? candidate : null;
	}

	@Override
	public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
		Direction attachmentFace = state.get(ATTACHMENT_FACE);
		BlockPos attachedPos = pos.offset(attachmentFace);
		// Check the opposite face of the adjacent block is full square (solid face)
		return world.getBlockState(attachedPos).isSideSolidFullSquare(world, attachedPos, attachmentFace.getOpposite());
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
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
	public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		// No collision like vanilla flowers
		return VoxelShapes.empty();
	}

	public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
		// If the supporting block was changed and no longer supports us, break
		if (!this.canPlaceAt(state, world, pos)) {
			return net.minecraft.block.Blocks.AIR.getDefaultState();
		}
		return state;
	}
}


