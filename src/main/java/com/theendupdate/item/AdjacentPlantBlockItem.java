package com.theendupdate.item;

import net.minecraft.block.Block;
import net.minecraft.block.PlantBlock;
import net.minecraft.block.TallPlantBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
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
		// If the targeted block is a flower or a tall plant, try to place on the adjacent block instead
		var world = context.getWorld();
		BlockPos pos = context.getBlockPos();
		var state = world.getBlockState(pos);
		boolean isFlower = state.isIn(BlockTags.FLOWERS);
		boolean isTallPlant = state.getBlock() instanceof TallPlantBlock;
		boolean isOtherPlant = state.getBlock() instanceof PlantBlock;
		if (isFlower || isTallPlant || isOtherPlant) {
			Direction side = context.getSide();
			BlockPos adj = pos.offset(side);
			// Build a new placement context targeting the adjacent block
			BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(adj), side, adj, false);
			return new ItemPlacementContext(world, context.getPlayer(), context.getHand(), context.getStack(), hit);
		}
		return super.getPlacementContext(context);
	}
}


