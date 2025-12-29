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
            // Manual drop logic: Silk Touch -> drop self; else -> drop 1-5 shards (weighted to 3-5) with large fortune bonuses
            boolean hasSilk = false;
            int fortuneLevel = 0;
            try {
                ItemEnchantmentsComponent ench = tool.get(DataComponentTypes.ENCHANTMENTS);
                if (ench != null) {
                    String s = ench.toString();
                    hasSilk = s.contains("minecraft:silk_touch");
                    // Detect fortune level - check from highest to lowest
                    for (int lvl = 5; lvl >= 1; lvl--) {
                        if (s.contains("minecraft:fortune") && s.contains(":" + lvl + "]")) {
                            fortuneLevel = lvl;
                            break;
                        }
                    }
                    if (fortuneLevel == 0 && s.contains("minecraft:fortune")) {
                        fortuneLevel = 1;
                    }
                }
            } catch (Throwable ignore) {}

            if (hasSilk) {
                Block.dropStack(world, pos, new ItemStack(this.asItem()));
            } else {
                // Weighted base drop: favors 3-5 shards
                // Weights: 1=5%, 2=10%, 3=30%, 4=30%, 5=25%
                int base;
                int roll = world.getRandom().nextInt(100);
                if (roll < 5) {
                    base = 1;
                } else if (roll < 15) {
                    base = 2;
                } else if (roll < 45) {
                    base = 3;
                } else if (roll < 75) {
                    base = 4;
                } else {
                    base = 5;
                }
                
                // Large fortune bonus: +3 to +8 per fortune level
                int fortuneBonus = 0;
                if (fortuneLevel > 0) {
                    fortuneBonus = world.getRandom().nextInt(6) + 3; // 3-8
                    fortuneBonus *= fortuneLevel; // Multiply by fortune level
                }
                
                int total = base + fortuneBonus;
                Block.dropStack(world, pos, new ItemStack(com.theendupdate.registry.ModItems.VOIDSTAR_SHARD, total));
            }
            ((ServerWorld) world).emitGameEvent(player, net.minecraft.world.event.GameEvent.BLOCK_DESTROY, pos);
        }
        // Do not call super to avoid default loot table path and prevent double drops
    }

    // Shulker spawning behavior moved to a chunk-load spawner; block no longer triggers scheduled spawns
}


