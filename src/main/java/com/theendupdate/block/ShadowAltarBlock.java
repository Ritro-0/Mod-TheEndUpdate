package com.theendupdate.block;

import com.mojang.serialization.MapCodec;
import com.theendupdate.registry.ModBlockEntities;
import com.theendupdate.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class ShadowAltarBlock extends BaseEntityBlock {
	public static final MapCodec<ShadowAltarBlock> CODEC = simpleCodec(ShadowAltarBlock::new);

	public ShadowAltarBlock(Properties settings) {
		super(settings);
	}

	@Override
	public MapCodec<ShadowAltarBlock> codec() {
		return CODEC;
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	// Mapping-safe: omit @Override for cross-version compatibility
	public PushReaction getPistonBehavior(BlockState state) {
		// immovable - prevents corruption/duplication of the block entity
		return PushReaction.BLOCK;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new ShadowAltarBlockEntity(pos, state);
	}

	// Mapping-safe overload used in some versions
	protected InteractionResult onUse(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		ItemStack held = player.getItemInHand(hand);
		boolean isIgniter = held.is(Items.FLINT_AND_STEEL) || held.is(Items.FIRE_CHARGE);
		if (!isIgniter) return InteractionResult.PASS;
		if (world.isClientSide()) return InteractionResult.SUCCESS;

		BlockEntity be = world.getBlockEntity(pos);
		if (be instanceof ShadowAltarBlockEntity altar) {
			if (!altar.canActivate()) {
				return InteractionResult.CONSUME; // already active or cooling down
			}
			boolean started = altar.tryStart((ServerLevel) world);
			if (started) {
				if (!player.isCreative()) {
					if (held.is(Items.FLINT_AND_STEEL)) {
						held.hurtAndBreak(1, player, hand);
					} else if (held.is(Items.FIRE_CHARGE)) {
						held.shrink(1);
					}
				}
				return InteractionResult.CONSUME;
			}
		}
		return InteractionResult.PASS;
	}

	// 1.21.8 mapping variant (without Hand parameter)
	@Override
	public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
		return onUse(state, world, pos, player, InteractionHand.MAIN_HAND, hit);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
		if (world.isClientSide()) return null;
		return (type == ModBlockEntities.SHADOW_ALTAR)
			? (w, p, s, be) -> ShadowAltarBlockEntity.tick(w, p, s, (ShadowAltarBlockEntity) be)
			: null;
	}
	
	// Mapping-safe: omit @Override and use broader signature
	public void onStateReplaced(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moved) {
		// clean up boss bar if altar is broken mid-charge
		if (!world.isClientSide() && !state.is(newState.getBlock())) {
			BlockEntity be = world.getBlockEntity(pos);
			if (be instanceof ShadowAltarBlockEntity altar) {
				altar.cleanup();
			}
		}
		if (!state.is(newState.getBlock()) && world instanceof ServerLevel sw) {
			super.affectNeighborsAfterRemoval(state, sw, pos, moved);
		}
	}
	
	// 1.21.8 superclass override variant
	@Override
	public void affectNeighborsAfterRemoval(BlockState state, ServerLevel world, BlockPos pos, boolean moved) {
		// no newState here so we can't tell if the block is actually being replaced - cleanup is handled by the other variant
		super.affectNeighborsAfterRemoval(state, world, pos, moved);
	}
	
	@Override
	public void playerDestroy(net.minecraft.world.level.Level world, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity, ItemStack tool) {
		if (!world.isClientSide() && world instanceof ServerLevel serverWorld) {
			boolean hasSilkTouch = false;
			try {
				ItemEnchantments ench = tool.get(DataComponents.ENCHANTMENTS);
				if (ench != null) {
					hasSilkTouch = ench.toString().contains("minecraft:silk_touch");
				}
			} catch (Throwable ignore) {}
			
			if (hasSilkTouch) {
				Block.popResource(world, pos, new ItemStack(this.asItem()));
			} else {
				int gunpowder = 1 + world.getRandom().nextInt(3); // 1-3
				Block.popResource(world, pos, new ItemStack(Items.GUNPOWDER, gunpowder));
				
				// 50% chance for 1 blaze rod
				if (world.getRandom().nextBoolean()) {
					Block.popResource(world, pos, new ItemStack(Items.BLAZE_ROD));
				}
				
				int cryptomycota = world.getRandom().nextInt(4); // 0-3
				if (cryptomycota > 0) {
					Block.popResource(world, pos, new ItemStack(ModBlocks.SHADOW_CRYPTOMYCOTA.asItem(), cryptomycota));
				}
				
				// custom XP, 30-87 (2x the vanilla spawner's 15-43)
				int xpAmount = world.getRandom().nextInt(58) + 30;
				this.popExperience(serverWorld, pos, xpAmount);
			}
			serverWorld.gameEvent(player, net.minecraft.world.level.gameevent.GameEvent.BLOCK_DESTROY, pos);
		}
		// Do not call super to avoid default loot table path
	}
}


