package com.theendupdate.world;

import com.theendupdate.registry.ModBlocks;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.ShulkerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Spawns shulkers on/near naturally generated crystal spikes after worldgen, with configured weights.
 * Runs opportunistically near players in The End to avoid scanning the entire world at once.
 */
public final class ShulkerOnCrystalsSpawner {
    private static final Set<Long> processedChunks = new HashSet<>();
    private static final java.util.Map<Long, Integer> attempts = new java.util.HashMap<>();

    public static void init() {
        ServerTickEvents.END_WORLD_TICK.register(ShulkerOnCrystalsSpawner::onEndWorldTick);
    }

    private static void onEndWorldTick(ServerWorld world) {
        if (!world.getRegistryKey().equals(World.END)) {
            return;
        }
        List<ServerPlayerEntity> players = world.getPlayers();
        if (players.isEmpty()) {
            return;
        }
        for (ServerPlayerEntity player : players) {
            // Scan a 7x7 around the player for broader coverage while flying
            ChunkPos center = new ChunkPos(player.getBlockPos());
            for (int cx = -3; cx <= 3; cx++) {
                for (int cz = -3; cz <= 3; cz++) {
                    ChunkPos playerChunk = new ChunkPos(center.x + cx, center.z + cz);
                    long chunkKey = chunkKey(world.getRegistryKey(), playerChunk);
                    if (processedChunks.contains(chunkKey)) {
                        continue;
                    }

                    List<BlockPos> crystalPositions = collectTopCrystalPositions(world, playerChunk);
            if (!crystalPositions.isEmpty()) {
                spawnShulkersForCluster(world, crystalPositions, world.getRandom());
                processedChunks.add(chunkKey);
                attempts.remove(chunkKey);
            } else {
                // Retry up to N ticks to allow late decoration
                int count = attempts.getOrDefault(chunkKey, 0) + 1;
                attempts.put(chunkKey, count);
                if (count > 120) { // ~6 seconds at 20 tps
                    processedChunks.add(chunkKey);
                    attempts.remove(chunkKey);
                }
            }
                }
            }
        }
    }

    private static List<BlockPos> collectTopCrystalPositions(ServerWorld world, ChunkPos chunkPos) {
        List<BlockPos> positions = new ArrayList<>();
        int startX = chunkPos.getStartX();
        int startZ = chunkPos.getStartZ();
        int minY = world.getBottomY();
        int maxY = minY + world.getHeight();
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = startX + dx;
                int z = startZ + dz;
                for (int y = maxY - 1; y >= minY; y--) {
                    BlockPos p = new BlockPos(x, y, z);
                    BlockState s = world.getBlockState(p);
                    if (isProbableNaturalCrystal(world, p, s)) {
                        positions.add(p.toImmutable());
                        break; // next column
                    }
                }
            }
        }
        return positions;
    }

    private static boolean isProbableNaturalCrystal(ServerWorld world, BlockPos pos, BlockState state) {
        if (!state.isOf(ModBlocks.STELLARITH_CRYSTAL)) return false;
        // If flagged NATURAL, accept immediately
        if (state.getOrEmpty(com.theendupdate.block.StellarithCrystalBlock.NATURAL).orElse(Boolean.FALSE)) return true;
        // Heuristic for existing worlds: clustered crystal or adjacent astral remnant implies natural spike
        if (hasAstralNeighbor(world, pos)) return true;
        return estimateCrystalClusterSize(world, pos, 24) >= 12;
    }

    private static boolean hasAstralNeighbor(ServerWorld world, BlockPos pos) {
        for (Direction d : Direction.values()) {
            if (world.getBlockState(pos.offset(d)).isOf(ModBlocks.ASTRAL_REMNANT)) return true;
        }
        return false;
    }

    private static int estimateCrystalClusterSize(ServerWorld world, BlockPos start, int max) {
        java.util.ArrayDeque<BlockPos> q = new java.util.ArrayDeque<>();
        java.util.HashSet<BlockPos> seen = new java.util.HashSet<>();
        q.add(start);
        seen.add(start);
        int size = 0;
        while (!q.isEmpty() && size < max) {
            BlockPos cur = q.poll();
            if (!world.getBlockState(cur).isOf(ModBlocks.STELLARITH_CRYSTAL)) continue;
            size++;
            for (Direction d : Direction.values()) {
                BlockPos nxt = cur.offset(d);
                if (seen.add(nxt) && world.getBlockState(nxt).isOf(ModBlocks.STELLARITH_CRYSTAL)) {
                    q.add(nxt);
                }
            }
        }
        return size;
    }

    private static void spawnShulkersForCluster(ServerWorld world, List<BlockPos> crystalBlocks, Random random) {
        // 75% chance to spawn on this cluster
        if (random.nextFloat() >= 0.75f) {
            return;
        }

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
            if (crystalBlocks.isEmpty()) break;
            BlockPos support = crystalBlocks.get(random.nextInt(crystalBlocks.size()));
            if (!trySpawnShulkerOnSupport(world, support, random)) {
                // Try nearby island base
                trySpawnShulkerAround(world, support, random, 4);
            }
        }
    }

    private static boolean trySpawnShulkerOnSupport(ServerWorld world, BlockPos support, Random random) {
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

            ShulkerEntity shulker = new ShulkerEntity(EntityType.SHULKER, world);
            shulker.setPersistent();
            shulker.refreshPositionAndAngles(air, 0.0F, 0.0F);
            if (world.spawnEntity(shulker)) return true;
        }
        return false;
    }

    private static boolean trySpawnShulkerAround(ServerWorld world, BlockPos center, Random random, int radius) {
        BlockPos.Mutable cursor = new BlockPos.Mutable();
        for (int tries = 0; tries < 64; tries++) {
            int dx = random.nextBetween(-radius, radius);
            int dy = random.nextBetween(-radius, radius);
            int dz = random.nextBetween(-radius, radius);
            cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
            BlockState s = world.getBlockState(cursor);
            if (!(s.isOf(Blocks.END_STONE) || s.isOf(ModBlocks.END_MIRE) || s.isOf(ModBlocks.MOLD_BLOCK)
                || s.isOf(ModBlocks.ASTRAL_REMNANT) || s.isOf(ModBlocks.STELLARITH_CRYSTAL))) {
                continue;
            }
            if (trySpawnShulkerOnSupport(world, cursor, random)) {
                return true;
            }
        }
        return false;
    }

    private static long chunkKey(RegistryKey<World> worldKey, ChunkPos chunkPos) {
        // Combine world and chunk coords into a single long key
        return (((long) worldKey.getValue().hashCode()) << 32) ^ (((long) chunkPos.x) << 16) ^ (chunkPos.z & 0xFFFFL);
    }

    private ShulkerOnCrystalsSpawner() {}
}


