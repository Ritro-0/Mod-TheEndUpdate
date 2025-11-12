package com.theendupdate.block;

import com.mojang.serialization.MapCodec;
import com.theendupdate.registry.ModBlockEntities;
import com.theendupdate.registry.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class ShadowAltarBlock extends BlockWithEntity {
	public static final MapCodec<ShadowAltarBlock> CODEC = createCodec(ShadowAltarBlock::new);

	public ShadowAltarBlock(Settings settings) {
		super(settings);
	}

	@Override
	public MapCodec<ShadowAltarBlock> getCodec() {
		return CODEC;
	}

	@Override
	public BlockRenderType getRenderType(BlockState state) {
		return BlockRenderType.MODEL;
	}

	// Mapping-safe: omit @Override for cross-version compatibility
	public PistonBehavior getPistonBehavior(BlockState state) {
		// Block entities should be immovable to prevent corruption/duplication
		return PistonBehavior.BLOCK;
	}

	@Nullable
	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new ShadowAltarBlockEntity(pos, state);
	}

	// Mapping-safe overload used in some versions
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
		ItemStack held = player.getStackInHand(hand);
		boolean isIgniter = held.isOf(Items.FLINT_AND_STEEL) || held.isOf(Items.FIRE_CHARGE);
		if (!isIgniter) return ActionResult.PASS;
		if (world.isClient()) return ActionResult.SUCCESS;

		BlockEntity be = world.getBlockEntity(pos);
		if (be instanceof ShadowAltarBlockEntity altar) {
			if (!altar.canActivate()) {
				return ActionResult.CONSUME; // already active or cooling down
			}
			boolean started = altar.tryStart((ServerWorld) world);
			if (started) {
				if (!player.isCreative()) {
					if (held.isOf(Items.FLINT_AND_STEEL)) {
						held.damage(1, player, hand);
					} else if (held.isOf(Items.FIRE_CHARGE)) {
						held.decrement(1);
					}
				}
				return ActionResult.CONSUME;
			}
		}
		return ActionResult.PASS;
	}

	// 1.21.8 mapping variant (without Hand parameter)
	@Override
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		return onUse(state, world, pos, player, Hand.MAIN_HAND, hit);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
		if (world.isClient()) return null;
		return (type == ModBlockEntities.SHADOW_ALTAR)
			? (w, p, s, be) -> ShadowAltarBlockEntity.tick(w, p, s, (ShadowAltarBlockEntity) be)
			: null;
	}
	
	// Mapping-safe: omit @Override and use broader signature
	public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
		// Clean up the boss bar if altar is broken during charging
		if (!world.isClient() && !state.isOf(newState.getBlock())) {
			BlockEntity be = world.getBlockEntity(pos);
			if (be instanceof ShadowAltarBlockEntity altar) {
				altar.cleanup();
			}
		}
		if (!state.isOf(newState.getBlock()) && world instanceof ServerWorld sw) {
			super.onStateReplaced(state, sw, pos, moved);
		}
	}
	
	// 1.21.8 superclass override variant
	@Override
	public void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
		// Note: This variant doesn't receive newState, so we can't check if block is actually being replaced
		// Only clean up if the block entity is actually being removed (checked by getting it after state change)
		// We'll rely on the other variant for proper cleanup on break
		super.onStateReplaced(state, world, pos, moved);
	}
	
	@Override
	public void afterBreak(net.minecraft.world.World world, PlayerEntity player, BlockPos pos, BlockState state, BlockEntity blockEntity, ItemStack tool) {
		if (!world.isClient() && world instanceof ServerWorld serverWorld) {
			// Check if tool has silk touch enchantment
			boolean hasSilkTouch = false;
			try {
				ItemEnchantmentsComponent ench = tool.get(DataComponentTypes.ENCHANTMENTS);
				if (ench != null) {
					hasSilkTouch = ench.toString().contains("minecraft:silk_touch");
				}
			} catch (Throwable ignore) {}
			
			if (hasSilkTouch) {
				// Drop the block itself when using silk touch
				Block.dropStack(world, pos, new ItemStack(this.asItem()));
			} else {
				// Drop multiple items: 1-3 gunpowder, 0-1 blaze rods, 0-3 shadow cryptomycota
				int gunpowder = 1 + world.random.nextInt(3); // 1-3
				Block.dropStack(world, pos, new ItemStack(Items.GUNPOWDER, gunpowder));
				
				// 0-1 blaze rod (50% chance for 1)
				if (world.random.nextBoolean()) {
					Block.dropStack(world, pos, new ItemStack(Items.BLAZE_ROD));
				}
				
				// 0-3 shadow cryptomycota (0 to 3 inclusive)
				int cryptomycota = world.random.nextInt(4); // 0-3
				if (cryptomycota > 0) {
					Block.dropStack(world, pos, new ItemStack(ModBlocks.SHADOW_CRYPTOMYCOTA.asItem(), cryptomycota));
				}
				
				// Drop custom XP amount: 30-87 (2x spawner's 15-43)
				int xpAmount = world.random.nextInt(58) + 30; // Random range 30-87
				this.dropExperience(serverWorld, pos, xpAmount);
			}
			serverWorld.emitGameEvent(player, net.minecraft.world.event.GameEvent.BLOCK_DESTROY, pos);
		}
		// Do not call super to avoid default loot table path
	}
}


