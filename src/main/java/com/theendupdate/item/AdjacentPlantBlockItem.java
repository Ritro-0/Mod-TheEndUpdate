package com.theendupdate.item;

import net.minecraft.block.Block;
import net.minecraft.block.PlantBlock;
import net.minecraft.block.TallPlantBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.registry.tag.BlockTags;

/**
 * BlockItem that prefers placing adjacent when the targeted block is a flower or tall plant,
 * instead of replacing it. Helps avoid odd replace-and-break interactions with double-high plants.
 */
public class AdjacentPlantBlockItem extends BlockItem {
	public AdjacentPlantBlockItem(Block block, Item.Settings settings) {
		super(block, settings);
	}

	@Override
	public ItemPlacementContext getPlacementContext(ItemPlacementContext context) {
		// If the targeted block is a flower or a plant, try the immediate neighboring block.
		var world = context.getWorld();
		BlockPos pos = context.getBlockPos();
		var state = world.getBlockState(pos);
		boolean isFlower = state.isIn(BlockTags.FLOWERS);
		boolean isTallPlant = state.getBlock() instanceof TallPlantBlock;
		boolean isOtherPlant = state.getBlock() instanceof PlantBlock;
		if (isFlower || isTallPlant || isOtherPlant) {
			Direction side = context.getSide();
			BlockPos adj = pos.offset(side);

			// Validate the immediate neighbor explicitly. If it's not valid, fail placement.
			var adjState = world.getBlockState(adj);

			// Do not place if the neighbor already has any plant (prevents replacing/skipping behavior)
			if (adjState.isIn(BlockTags.FLOWERS) || adjState.getBlock() instanceof TallPlantBlock || adjState.getBlock() instanceof PlantBlock) {
				return null; // fail instead of skipping past
			}

			// Must be replaceable (air, snow layer, fluids, etc.)
			ItemPlacementContext adjContextProbe;
			{
				BlockHitResult probeHit = new BlockHitResult(Vec3d.ofCenter(adj), side, adj, false);
				adjContextProbe = new ItemPlacementContext(world, context.getPlayer(), context.getHand(), context.getStack(), probeHit);
			}
			if (!adjState.canReplace(adjContextProbe)) {
				return null; // neighbor is not replaceable; do not offset further
			}

			// Must be a valid position for this block (e.g., has solid top below, etc.)
			if (!this.getBlock().getDefaultState().canPlaceAt(world, adj)) {
				return null;
			}

			// All good: build a placement context targeting ONLY the immediate neighbor
			BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(adj), side, adj, false);
			return new ItemPlacementContext(world, context.getPlayer(), context.getHand(), context.getStack(), hit);
		}
		return super.getPlacementContext(context);
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		var world = context.getWorld();
		BlockPos pos = context.getBlockPos();
		var state = world.getBlockState(pos);
		boolean isFlower = state.isIn(BlockTags.FLOWERS);
		boolean isTallPlant = state.getBlock() instanceof TallPlantBlock;
		boolean isOtherPlant = state.getBlock() instanceof PlantBlock;
		if (isFlower || isTallPlant || isOtherPlant) {
			Direction side = context.getSide();
			BlockPos adj = pos.offset(side);
			var adjState = world.getBlockState(adj);

			// If the immediate neighbor is any plant, fail instead of skipping/replacing
			if (adjState.isIn(BlockTags.FLOWERS) || adjState.getBlock() instanceof TallPlantBlock || adjState.getBlock() instanceof PlantBlock) {
				return ActionResult.FAIL;
			}

			// Build a placement context for the immediate neighbor
			BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(adj), side, adj, false);
			ItemPlacementContext adjCtx = new ItemPlacementContext(world, context.getPlayer(), context.getHand(), context.getStack(), hit);

			// Require neighbor to be replaceable
			if (!adjState.canReplace(adjCtx)) {
				return ActionResult.FAIL;
			}

			// Require neighbor position to be valid for this block
			if (!this.getBlock().getDefaultState().canPlaceAt(world, adj)) {
				return ActionResult.FAIL;
			}

			// Place only at the immediate neighbor
			return this.place(adjCtx);
		}
		return super.useOnBlock(context);
	}
}


