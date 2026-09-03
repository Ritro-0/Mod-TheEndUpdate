package com.theendupdate.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.PushReaction;

public class SpectralBlock extends Block {
    private static final int HALO_RANGE = 2;
    private static final int HALO_LEVEL = 2; // low-level helper light to extend effective reach

    public SpectralBlock(Properties settings) {
        super(settings);
    }

    // Mapping-safe: omit @Override for cross-version compatibility
    public PushReaction getPistonBehavior(BlockState state) {
        return PushReaction.NORMAL; // pushable/pullable, like glowstone
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, net.minecraft.world.entity.LivingEntity placer, net.minecraft.world.item.ItemStack itemStack) {
        super.setPlacedBy(world, pos, state, placer, itemStack);
        if (!world.isClientSide()) {
            placeHalo((net.minecraft.server.level.ServerLevel) world, pos);
        }
    }

    // Mapping-safe: omit @Override for the World signature variant used by some mappings
    public void onStateReplaced(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moved) {
        if (state.getBlock() != newState.getBlock()) {
            if (!world.isClientSide()) {
                clearHalo((net.minecraft.server.level.ServerLevel) world, pos);
            }
        }
        // Intentionally do not call super here; see ServerWorld overload below
    }

    // 1.21.8 superclass override variant
    public void affectNeighborsAfterRemoval(BlockState state, net.minecraft.server.level.ServerLevel world, BlockPos pos, boolean moved) {
        super.affectNeighborsAfterRemoval(state, world, pos, moved);
    }

    @Override
    public void playerDestroy(net.minecraft.world.level.Level world, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity, ItemStack tool) {
        if (!world.isClientSide()) {
            boolean hasSilk = false;
            int fortuneLevel = 0;
            try {
                ItemEnchantments ench = tool.get(DataComponents.ENCHANTMENTS);
                if (ench != null) {
                    String s = ench.toString();
                    hasSilk = s.contains("minecraft:silk_touch");
                    // crude but mapping-safe: scan for tokens like 'minecraft:fortune:1' instead of resolving the enchantment
                    for (int lvl = 5; lvl >= 1; lvl--) {
                        if (s.contains("minecraft:fortune") && s.contains(":" + lvl + "]")) { fortuneLevel = lvl; break; }
                    }
                    if (fortuneLevel == 0 && s.contains("minecraft:fortune")) fortuneLevel = 1;
                }
            } catch (Throwable ignore) {}

            if (hasSilk) {
                Block.popResource(world, pos, new ItemStack(this.asItem()));
            } else {
                int base = 2 + world.getRandom().nextInt(3); // 2..4
                int bonus = fortuneLevel > 0 ? world.getRandom().nextInt(fortuneLevel + 1) : 0; // 0..fortune
                int total = Math.max(1, base + bonus);
                Block.popResource(world, pos, new ItemStack(com.theendupdate.registry.ModItems.SPECTRAL_CLUSTER, total));
            }
            ((ServerLevel) world).gameEvent(player, net.minecraft.world.level.gameevent.GameEvent.BLOCK_DESTROY, pos);
        }
        // no super call - avoids the default loot table path
    }

    private void placeHalo(net.minecraft.server.level.ServerLevel world, BlockPos center) {
        iterateHalo(center, (target) -> {
            if (!world.hasChunk(target.getX() >> 4, target.getZ() >> 4)) return;
            BlockState current = world.getBlockState(target);
            if (current.isAir() || (current.is(Blocks.LIGHT) && current.getValue(BlockStateProperties.LEVEL) < HALO_LEVEL)) {
                BlockState halo = Blocks.LIGHT.defaultBlockState().setValue(BlockStateProperties.LEVEL, HALO_LEVEL);
                world.setBlock(target, halo, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
        });
    }

    private void clearHalo(net.minecraft.server.level.ServerLevel world, BlockPos center) {
        iterateHalo(center, (target) -> {
            if (!world.hasChunk(target.getX() >> 4, target.getZ() >> 4)) return;
            BlockState current = world.getBlockState(target);
            if (current.is(Blocks.LIGHT) && current.getValue(BlockStateProperties.LEVEL) == HALO_LEVEL) {
                world.removeBlock(target, false);
            }
        });
    }

    private void iterateHalo(BlockPos center, java.util.function.Consumer<BlockPos> consumer) {
        for (int dx = -HALO_RANGE; dx <= HALO_RANGE; dx++) {
            for (int dy = -HALO_RANGE; dy <= HALO_RANGE; dy++) {
                for (int dz = -HALO_RANGE; dz <= HALO_RANGE; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    int manhattan = Math.abs(dx) + Math.abs(dy) + Math.abs(dz);
                    if (manhattan == HALO_RANGE) {
                        consumer.accept(center.offset(dx, dy, dz));
                    }
                }
            }
        }
    }
}


