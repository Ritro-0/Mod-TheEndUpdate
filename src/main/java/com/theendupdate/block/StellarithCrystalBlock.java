package com.theendupdate.block;

import net.minecraft.block.Block;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.StateManager;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
// removed unused world imports

// unused imports removed

public class StellarithCrystalBlock extends Block {
    public static final BooleanProperty NATURAL = BooleanProperty.of("natural");
    public static final BooleanProperty ORBS_SPAWNED = BooleanProperty.of("orbs_spawned");
    public StellarithCrystalBlock(AbstractBlock.Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(NATURAL, Boolean.FALSE).with(ORBS_SPAWNED, Boolean.FALSE));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(NATURAL, ORBS_SPAWNED);
    }

    // Mapping-safe: omit @Override for cross-version compatibility
    public PistonBehavior getPistonBehavior(BlockState state) {
        // Explicitly set to NORMAL for consistent push/pull behavior
        return PistonBehavior.NORMAL;
    }

    @Override
    public void afterBreak(net.minecraft.world.World world, PlayerEntity player, BlockPos pos, BlockState state, BlockEntity blockEntity, ItemStack tool) {
        if (!world.isClient()) {
            // Manual drop logic (mirror End Mire approach): Silk Touch -> drop self; else -> drop shard
            boolean hasSilk = false;
            try {
                ItemEnchantmentsComponent ench = tool.get(DataComponentTypes.ENCHANTMENTS);
                hasSilk = ench != null && ench.toString().contains("minecraft:silk_touch");
            } catch (Throwable ignore) {}

            if (hasSilk) {
                Block.dropStack(world, pos, new ItemStack(this.asItem()));
            } else {
                Block.dropStack(world, pos, new ItemStack(com.theendupdate.registry.ModItems.VOIDSTAR_SHARD));
            }
            ((ServerWorld) world).emitGameEvent(player, net.minecraft.world.event.GameEvent.BLOCK_DESTROY, pos);
        }
        // Do not call super to avoid default loot table path and prevent double drops
    }

    // Shulker spawning behavior moved to a chunk-load spawner; block no longer triggers scheduled spawns
}


