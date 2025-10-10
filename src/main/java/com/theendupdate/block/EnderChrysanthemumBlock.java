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
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldView;

public class EnderChrysanthemumBlock extends PlantBlock {
	public static final MapCodec<EnderChrysanthemumBlock> CODEC = createCodec(EnderChrysanthemumBlock::new);
	public static final Property<Direction> ATTACHMENT_FACE = Properties.FACING;

	public EnderChrysanthemumBlock(Settings settings) {
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
		return Block.createCuboidShape(6.0, 0.0, 6.0, 10.0, 6.0, 10.0);
	}

	@Override
	public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
		Direction attachmentDirection = state.get(ATTACHMENT_FACE);
		BlockPos supportPos = pos.offset(attachmentDirection);
		BlockState supportState = world.getBlockState(supportPos);
		
		// Check if the supporting block can support the flower
		return supportState.isSideSolidFullSquare(world, supportPos, attachmentDirection.getOpposite());
	}

}