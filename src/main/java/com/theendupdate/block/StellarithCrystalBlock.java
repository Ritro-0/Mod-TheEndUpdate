package com.theendupdate.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.PushReaction;

public class StellarithCrystalBlock extends Block {
    public static final BooleanProperty NATURAL = BooleanProperty.create("natural");
    public static final BooleanProperty ORBS_SPAWNED = BooleanProperty.create("orbs_spawned");
    public StellarithCrystalBlock(BlockBehaviour.Properties settings) {
        super(settings);
        this.registerDefaultState(this.defaultBlockState().setValue(NATURAL, Boolean.FALSE).setValue(ORBS_SPAWNED, Boolean.FALSE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NATURAL, ORBS_SPAWNED);
    }

    // Mapping-safe: omit @Override for cross-version compatibility
    public PushReaction getPistonBehavior(BlockState state) {
        return PushReaction.NORMAL; // pushable/pullable
    }

    @Override
    public void playerDestroy(net.minecraft.world.level.Level world, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity, ItemStack tool) {
        if (!world.isClientSide()) {
            // manual drops: silk touch -> block itself, else 1-5 shards (weighted to 3-5) with fortune bonus
            boolean hasSilk = false;
            int fortuneLevel = 0;
            try {
                ItemEnchantments ench = tool.get(DataComponents.ENCHANTMENTS);
                if (ench != null) {
                    String s = ench.toString();
                    hasSilk = s.contains("minecraft:silk_touch");
                    // check highest level first so we match the actual enchant level, not just "contains fortune"
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
                Block.popResource(world, pos, new ItemStack(this.asItem()));
            } else {
                // weights: 1=5%, 2=10%, 3=30%, 4=30%, 5=25%
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
                
                int fortuneBonus = 0;
                if (fortuneLevel > 0) {
                    fortuneBonus = world.getRandom().nextInt(6) + 3; // 3-8, per fortune level
                    fortuneBonus *= fortuneLevel;
                }
                
                int total = base + fortuneBonus;
                Block.popResource(world, pos, new ItemStack(com.theendupdate.registry.ModItems.VOIDSTAR_SHARD, total));
            }
            ((ServerLevel) world).gameEvent(player, net.minecraft.world.level.gameevent.GameEvent.BLOCK_DESTROY, pos);
        }
        // no super call - avoids the default loot table and double drops
    }

    // shulker spawning is handled by a chunk-load spawner now, not this block
}


