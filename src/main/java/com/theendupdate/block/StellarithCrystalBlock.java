package com.theendupdate.block;

import net.minecraft.block.Block;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.StateManager;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public class StellarithCrystalBlock extends Block {
    public static final BooleanProperty NATURAL = BooleanProperty.of("natural");
    public StellarithCrystalBlock(AbstractBlock.Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(NATURAL, Boolean.FALSE));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(NATURAL);
    }

    @Override
    public void afterBreak(net.minecraft.world.World world, PlayerEntity player, BlockPos pos, BlockState state, BlockEntity blockEntity, ItemStack tool) {
        if (!world.isClient) {
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

    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        super.onBlockAdded(state, world, pos, oldState, notify);
        if (world.isClient) return;
        if (oldState.isOf(this)) return;
        if (!state.getOrEmpty(NATURAL).orElse(Boolean.FALSE)) return; // only natural crystals schedule spawn
        // Schedule a near-immediate tick to attempt shulker spawning for this spike cluster
        world.scheduleBlockTick(pos, this, 1);
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, net.minecraft.util.math.random.Random random) {
        if (!world.getRegistryKey().equals(net.minecraft.world.World.END)) return;
        if (Processed.remember(world, pos)) return; // already handled around here

        // Collect connected crystal cluster around this block (bounded)
        List<BlockPos> cluster = collectCluster(world, pos, 196);
        if (cluster.isEmpty()) return;

        // 75% gate
        if (random.nextFloat() >= 0.75f) return;

        int spawnCount = 1;
        if (random.nextFloat() < 0.50f) {
            spawnCount = 2;
            if (random.nextFloat() < 0.50f) {
                spawnCount = 3;
                if (random.nextFloat() < 0.12f) {
                    spawnCount = 12;
                }
            }
        }

        for (int i = 0; i < spawnCount; i++) {
            // 80% on crystal, 20% on surrounding
            boolean onCrystal = random.nextFloat() < 0.80f;
            boolean placed = false;
            if (onCrystal && !cluster.isEmpty()) {
                for (int tries = 0; tries < 24 && !placed; tries++) {
                    BlockPos support = cluster.get(random.nextInt(cluster.size()));
                    placed = trySpawnShulkerOnSupport(world, support, random);
                }
            }
            if (!placed) {
                placed = trySpawnShulkerAround(world, pos, random, 4);
            }
        }
    }

    private static List<BlockPos> collectCluster(ServerWorld world, BlockPos start, int max) {
        List<BlockPos> result = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> q = new ArrayDeque<>();
        q.add(start);
        visited.add(start);
        while (!q.isEmpty() && result.size() < max) {
            BlockPos cur = q.poll();
            if (!world.getBlockState(cur).isOf(com.theendupdate.registry.ModBlocks.STELLARITH_CRYSTAL)) continue;
            result.add(cur);
            for (Direction d : Direction.values()) {
                BlockPos nxt = cur.offset(d);
                if (!visited.contains(nxt) && world.getBlockState(nxt).isOf(com.theendupdate.registry.ModBlocks.STELLARITH_CRYSTAL)) {
                    visited.add(nxt);
                    q.add(nxt);
                }
            }
        }
        return result;
    }

    private static boolean trySpawnShulkerOnSupport(ServerWorld world, BlockPos support, net.minecraft.util.math.random.Random random) {
        Direction[] dirs = Direction.values();
        for (int i = 0; i < dirs.length; i++) {
            int j = random.nextInt(dirs.length);
            Direction t = dirs[i];
            dirs[i] = dirs[j];
            dirs[j] = t;
        }
        for (Direction dir : dirs) {
            BlockPos air = support.offset(dir);
            if (!world.getBlockState(air).isAir()) continue;
            if (air.getY() <= world.getBottomY() + 1) continue;
            net.minecraft.entity.mob.ShulkerEntity shulker = new net.minecraft.entity.mob.ShulkerEntity(net.minecraft.entity.EntityType.SHULKER, world);
            shulker.setPersistent();
            shulker.refreshPositionAndAngles(air, 0.0F, 0.0F);
            if (world.spawnEntity(shulker)) return true;
        }
        return false;
    }

    private static boolean trySpawnShulkerAround(ServerWorld world, BlockPos center, net.minecraft.util.math.random.Random random, int radius) {
        BlockPos.Mutable cursor = new BlockPos.Mutable();
        for (int tries = 0; tries < 96; tries++) {
            int dx = random.nextBetween(-radius, radius);
            int dy = random.nextBetween(-radius, radius);
            int dz = random.nextBetween(-radius, radius);
            cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
            net.minecraft.block.BlockState s = world.getBlockState(cursor);
            if (!(s.isOf(com.theendupdate.registry.ModBlocks.END_MIRE)
                || s.isOf(com.theendupdate.registry.ModBlocks.MOLD_BLOCK)
                || s.isOf(net.minecraft.block.Blocks.END_STONE)
                || s.isOf(com.theendupdate.registry.ModBlocks.ASTRAL_REMNANT)
                || s.isOf(com.theendupdate.registry.ModBlocks.STELLARITH_CRYSTAL))) continue;
            if (trySpawnShulkerOnSupport(world, cursor, random)) return true;
        }
        return false;
    }

    private static final class Processed {
        private static final Set<Long> SEEN = new HashSet<>();
        private static boolean remember(ServerWorld world, BlockPos pos) {
            long key = (((long) world.getRegistryKey().getValue().hashCode()) << 32) ^ pos.asLong();
            if (SEEN.contains(key)) return true;
            SEEN.add(key);
            return false;
        }
    }
}


