package com.theendupdate.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * BlockItem that prefers placing adjacent when the targeted block is a flower or tall plant,
 * instead of replacing it. Helps avoid odd replace-and-break interactions with double-high plants.
 */
public class AdjacentPlantBlockItem extends BlockItem {
	public AdjacentPlantBlockItem(Block block, Item.Properties settings) {
		super(block, settings);
	}

	@Override
	public BlockPlaceContext updatePlacementContext(BlockPlaceContext context) {
		var world = context.getLevel();
		BlockPos pos = context.getClickedPos();
		var state = world.getBlockState(pos);
		boolean isFlower = state.is(BlockTags.FLOWERS);
		boolean isTallPlant = state.getBlock() instanceof DoublePlantBlock;
		boolean isOtherPlant = state.getBlock() instanceof VegetationBlock;
		if (isFlower || isTallPlant || isOtherPlant) {
			Direction side = context.getClickedFace();
			BlockPos adj = pos.relative(side);
			var adjState = world.getBlockState(adj);

			// fail instead of skipping past if the neighbor already has any plant
			if (adjState.is(BlockTags.FLOWERS) || adjState.getBlock() instanceof DoublePlantBlock || adjState.getBlock() instanceof VegetationBlock) {
				return null;
			}

			BlockPlaceContext adjContextProbe;
			{
				BlockHitResult probeHit = new BlockHitResult(Vec3.atCenterOf(adj), side, adj, false);
				adjContextProbe = new BlockPlaceContext(world, context.getPlayer(), context.getHand(), context.getItemInHand(), probeHit);
			}
			if (!adjState.canBeReplaced(adjContextProbe)) {
				return null;
			}

			if (!this.getBlock().defaultBlockState().canSurvive(world, adj)) {
				return null;
			}

			BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(adj), side, adj, false);
			return new BlockPlaceContext(world, context.getPlayer(), context.getHand(), context.getItemInHand(), hit);
		}
		return super.updatePlacementContext(context);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		var world = context.getLevel();
		BlockPos pos = context.getClickedPos();
		var state = world.getBlockState(pos);
		boolean isFlower = state.is(BlockTags.FLOWERS);
		boolean isTallPlant = state.getBlock() instanceof DoublePlantBlock;
		boolean isOtherPlant = state.getBlock() instanceof VegetationBlock;
		if (isFlower || isTallPlant || isOtherPlant) {
			Direction side = context.getClickedFace();
			BlockPos adj = pos.relative(side);
			var adjState = world.getBlockState(adj);

			if (adjState.is(BlockTags.FLOWERS) || adjState.getBlock() instanceof DoublePlantBlock || adjState.getBlock() instanceof VegetationBlock) {
				return InteractionResult.FAIL;
			}

			BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(adj), side, adj, false);
			BlockPlaceContext adjCtx = new BlockPlaceContext(world, context.getPlayer(), context.getHand(), context.getItemInHand(), hit);

			if (!adjState.canBeReplaced(adjCtx)) {
				return InteractionResult.FAIL;
			}

			if (!this.getBlock().defaultBlockState().canSurvive(world, adj)) {
				return InteractionResult.FAIL;
			}

			return this.place(adjCtx);
		}
		return super.useOn(context);
	}
}


