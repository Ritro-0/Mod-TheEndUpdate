package com.theendupdate.world;

import com.theendupdate.registry.ModBlocks;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.ShulkerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class EndChunkCrystalSpawner {
    private static final Set<Long> handled = new HashSet<>();

    public static void init() {
        ServerChunkEvents.CHUNK_LOAD.register((world, chunk) -> onChunkLoad(world, chunk));
    }

    private static void onChunkLoad(ServerWorld world, WorldChunk chunk) {
        if (!world.getRegistryKey().equals(World.END)) return;
        ChunkPos cp = chunk.getPos();
        long key = (((long) world.getRegistryKey().getValue().hashCode()) << 32) ^ (((long) cp.x) << 16) ^ (cp.z & 0xFFFFL);
        if (handled.contains(key)) return;

        List<BlockPos> crystals = findNaturalCrystals(world, cp);
        if (crystals.isEmpty()) return;

        spawnShulkers(world, crystals, world.getRandom());
        handled.add(key);
    }

    private static List<BlockPos> findNaturalCrystals(ServerWorld world, ChunkPos chunkPos) {
        List<BlockPos> out = new ArrayList<>();
        int minY = world.getBottomY();
        int maxY = minY + world.getHeight();
        int startX = chunkPos.getStartX();
        int startZ = chunkPos.getStartZ();
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = startX + dx;
                int z = startZ + dz;
                for (int y = maxY - 1; y >= minY; y--) {
                    BlockPos p = new BlockPos(x, y, z);
                    BlockState s = world.getBlockState(p);
                    if (s.isOf(ModBlocks.STELLARITH_CRYSTAL)) {
                        boolean natural = s.getOrEmpty(com.theendupdate.block.StellarithCrystalBlock.NATURAL).orElse(Boolean.FALSE);
                        if (natural || hasAstralNeighbor(world, p) || estimateCluster(world, p, 24) >= 12) {
                            out.add(p.toImmutable());
                            break;
                        }
                    }
                }
            }
        }
        return out;
    }

    private static boolean hasAstralNeighbor(ServerWorld world, BlockPos pos) {
        for (Direction d : Direction.values()) {
            if (world.getBlockState(pos.offset(d)).isOf(ModBlocks.ASTRAL_REMNANT)) return true;
        }
        return false;
    }

    private static int estimateCluster(ServerWorld world, BlockPos start, int max) {
        ArrayDeque<BlockPos> q = new ArrayDeque<>();
        HashSet<BlockPos> seen = new HashSet<>();
        q.add(start);
        seen.add(start);
        int size = 0;
        while (!q.isEmpty() && size < max) {
            BlockPos cur = q.poll();
            if (!world.getBlockState(cur).isOf(ModBlocks.STELLARITH_CRYSTAL)) continue;
            size++;
            for (Direction d : Direction.values()) {
                BlockPos nxt = cur.offset(d);
                if (seen.add(nxt) && world.getBlockState(nxt).isOf(ModBlocks.STELLARITH_CRYSTAL)) q.add(nxt);
            }
        }
        return size;
    }

    private static void spawnShulkers(ServerWorld world, List<BlockPos> crystalBlocks, Random random) {
        if (random.nextFloat() >= 0.75f) return;
        int spawnCount = 1;
        if (random.nextFloat() < 0.50f) {
            spawnCount = 2;
            if (random.nextFloat() < 0.50f) {
                spawnCount = 3;
                if (random.nextFloat() < 0.12f) spawnCount = 12;
            }
        }
        for (int i = 0; i < spawnCount; i++) {
            BlockPos support = crystalBlocks.get(random.nextInt(crystalBlocks.size()));
            if (!trySpawn(world, support, random)) {
                trySpawnAround(world, support, random, 4);
            }
        }
    }

    private static boolean trySpawn(ServerWorld world, BlockPos support, Random random) {
        Direction[] dirs = Direction.values();
        for (int i = 0; i < dirs.length; i++) {
            int j = random.nextInt(dirs.length);
            Direction tmp = dirs[i];
            dirs[i] = dirs[j];
            dirs[j] = tmp;
        }
        for (Direction dir : dirs) {
            BlockPos air = support.offset(dir);
            if (!world.getBlockState(air).isAir()) continue;
            if (air.getY() <= world.getBottomY() + 1) continue;
            ShulkerEntity e = EntityType.SHULKER.spawn(world, air, SpawnReason.STRUCTURE);
            if (e == null) continue;
            e.setPersistent();
            e.refreshPositionAndAngles(air, 0.0F, 0.0F);
            return true;
        }
        return false;
    }

    private static boolean trySpawnAround(ServerWorld world, BlockPos center, Random random, int radius) {
        BlockPos.Mutable cursor = new BlockPos.Mutable();
        for (int tries = 0; tries < 96; tries++) {
            int dx = random.nextBetween(-radius, radius);
            int dy = random.nextBetween(-radius, radius);
            int dz = random.nextBetween(-radius, radius);
            cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
            BlockState s = world.getBlockState(cursor);
            if (!(s.isOf(ModBlocks.END_MIRE) || s.isOf(ModBlocks.MOLD_BLOCK) || s.isOf(ModBlocks.ASTRAL_REMNANT)
                || s.isOf(ModBlocks.STELLARITH_CRYSTAL) || s.isOf(net.minecraft.block.Blocks.END_STONE))) continue;
            if (trySpawn(world, cursor, random)) return true;
        }
        return false;
    }

    private EndChunkCrystalSpawner() {}
}


