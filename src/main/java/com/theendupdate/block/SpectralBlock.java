package com.theendupdate.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class SpectralBlock extends Block {
    private static final int HALO_RANGE = 2;
    private static final int HALO_LEVEL = 2; // low-level helper light to extend effective reach

    public SpectralBlock(Settings settings) {
        super(settings);
    }

    // Mapping-safe: omit @Override for cross-version compatibility
    public PistonBehavior getPistonBehavior(BlockState state) {
        // Explicitly set to NORMAL for consistent push/pull behavior (like glowstone)
        return PistonBehavior.NORMAL;
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, net.minecraft.entity.LivingEntity placer, net.minecraft.item.ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        if (!world.isClient()) {
            placeHalo((net.minecraft.server.world.ServerWorld) world, pos);
        }
    }

    // Mapping-safe: omit @Override for the World signature variant used by some mappings
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (state.getBlock() != newState.getBlock()) {
            if (!world.isClient()) {
                clearHalo((net.minecraft.server.world.ServerWorld) world, pos);
            }
        }
        // Intentionally do not call super here; see ServerWorld overload below
    }

    // 1.21.8 superclass override variant
    public void onStateReplaced(BlockState state, net.minecraft.server.world.ServerWorld world, BlockPos pos, boolean moved) {
        super.onStateReplaced(state, world, pos, moved);
    }

    @Override
    public void afterBreak(net.minecraft.world.World world, PlayerEntity player, BlockPos pos, BlockState state, BlockEntity blockEntity, ItemStack tool) {
        if (!world.isClient()) {
            boolean hasSilk = false;
            int fortuneLevel = 0;
            try {
                ItemEnchantmentsComponent ench = tool.get(DataComponentTypes.ENCHANTMENTS);
                if (ench != null) {
                    String s = ench.toString();
                    hasSilk = s.contains("minecraft:silk_touch");
                    // crude but mapping-safe way to detect fortune level tokens like 'minecraft:fortune:1'
                    // prefer exact formula: base (2..4) + random [0..fortune]
                    for (int lvl = 5; lvl >= 1; lvl--) {
                        if (s.contains("minecraft:fortune") && s.contains(":" + lvl + "]")) { fortuneLevel = lvl; break; }
                    }
                    if (fortuneLevel == 0 && s.contains("minecraft:fortune")) fortuneLevel = 1;
                }
            } catch (Throwable ignore) {}

            if (hasSilk) {
                Block.dropStack(world, pos, new ItemStack(this.asItem()));
            } else {
                int base = 2 + world.getRandom().nextInt(3); // 2..4
                int bonus = fortuneLevel > 0 ? world.getRandom().nextInt(fortuneLevel + 1) : 0; // 0..fortune
                int total = Math.max(1, base + bonus);
                Block.dropStack(world, pos, new ItemStack(com.theendupdate.registry.ModItems.SPECTRAL_CLUSTER, total));
            }
            ((ServerWorld) world).emitGameEvent(player, net.minecraft.world.event.GameEvent.BLOCK_DESTROY, pos);
        }
        // Do not call super to avoid default loot table path
    }

    private void placeHalo(net.minecraft.server.world.ServerWorld world, BlockPos center) {
        iterateHalo(center, (target) -> {
            if (!world.isChunkLoaded(target.getX() >> 4, target.getZ() >> 4)) return;
            BlockState current = world.getBlockState(target);
            if (current.isAir() || (current.isOf(Blocks.LIGHT) && current.get(Properties.LEVEL_15) < HALO_LEVEL)) {
                BlockState halo = Blocks.LIGHT.getDefaultState().with(Properties.LEVEL_15, HALO_LEVEL);
                world.setBlockState(target, halo, Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
            }
        });
    }

    private void clearHalo(net.minecraft.server.world.ServerWorld world, BlockPos center) {
        iterateHalo(center, (target) -> {
            if (!world.isChunkLoaded(target.getX() >> 4, target.getZ() >> 4)) return;
            BlockState current = world.getBlockState(target);
            if (current.isOf(Blocks.LIGHT) && current.get(Properties.LEVEL_15) == HALO_LEVEL) {
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
                        consumer.accept(center.add(dx, dy, dz));
                    }
                }
            }
        }
    }
}


