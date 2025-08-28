package com.theendupdate.world;

import com.theendupdate.block.StellarithCrystalBlock;
import com.theendupdate.entity.EtherealOrbEntity;
import com.theendupdate.registry.ModBlocks;
import com.theendupdate.registry.ModEntities;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Spawns Ethereal Orbs once per naturally generated Stellarith crystal spike tip
 * in Mirelands biomes, after worldgen during chunk load. Uses OrbSpawnState to
 * ensure one-time spawns.
 */
public final class EtherealOrbOnCrystalsSpawner {
    private static final Set<Long> processedChunks = new HashSet<>();
    private static final java.util.LinkedHashSet<Long> queue = new java.util.LinkedHashSet<>();
    private static final java.util.HashSet<Long> spawnedTips = new java.util.HashSet<>();

    public static void init() {
        // Prefer tick-budgeted scanning near players to avoid heavy work during chunk load
        ServerTickEvents.END_WORLD_TICK.register(EtherealOrbOnCrystalsSpawner::onWorldTick);
        // Keep CHUNK_LOAD registered but do nothing to avoid freezes
        ServerChunkEvents.CHUNK_LOAD.register((world, chunk) -> {});
    }

    private static void onWorldTick(ServerWorld world) {
        if (!world.getRegistryKey().equals(World.END)) return;
        try {
            // Enqueue nearby chunks around players
            List<net.minecraft.server.network.ServerPlayerEntity> players = world.getPlayers();
            for (var player : players) {
                ChunkPos center = new ChunkPos(player.getBlockPos());
                for (int dx = -2; dx <= 2; dx++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        ChunkPos cp = new ChunkPos(center.x + dx, center.z + dz);
                        long key = chunkKey(world.getRegistryKey(), cp);
                        if (processedChunks.contains(key)) continue;
                        queue.add(key);
                    }
                }
            }

            // Budget: process at most 2 chunks per tick to avoid frame stalls
            int budget = 2;
            java.util.Iterator<Long> it = queue.iterator();
            while (budget-- > 0 && it.hasNext()) {
                long key = it.next();
                it.remove();
                int cx = (int) ((key >> 16) & 0xFFFF);
                if ((cx & 0x8000) != 0) cx |= 0xFFFF0000; // sign-extend
                int cz = (int) (key & 0xFFFF);
                if ((cz & 0x8000) != 0) cz |= 0xFFFF0000;
                ChunkPos cp = new ChunkPos(cx, cz);
                if (!world.isChunkLoaded(cp.x, cp.z)) {
                    // keep it processed to avoid requeue loops; it will be re-enqueued by player proximity when loaded
                    processedChunks.add(key);
                    continue;
                }
                List<BlockPos> candidates = findNaturalCrystalTopsNearSurface(world, cp);
                if (!candidates.isEmpty()) {
                    trySpawnOrbsForTips(world, candidates, world.getRandom());
                }
                processedChunks.add(key);
            }
        } catch (Throwable ignored) {}
    }

    private static long chunkKey(RegistryKey<World> dim, ChunkPos pos) {
        return (((long) dim.getValue().hashCode()) << 32) ^ (((long) pos.x) << 16) ^ (pos.z & 0xFFFFL);
    }

    // Collect the top-most crystal blocks per x,z column that are likely part of natural spikes
    private static List<BlockPos> findNaturalCrystalTopsNearSurface(ServerWorld world, ChunkPos chunkPos) {
        List<BlockPos> tops = new ArrayList<>();
        int minY = world.getBottomY();
        int worldTop = minY + world.getHeight() - 1;
        int startX = chunkPos.getStartX();
        int startZ = chunkPos.getStartZ();
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = startX + dx;
                int z = startZ + dz;
                // Focus scan around terrain surface to reduce work
                int surfaceY = world.getTopPosition(net.minecraft.world.Heightmap.Type.WORLD_SURFACE_WG, new BlockPos(x, 0, z)).getY();
                int yStart = Math.min(worldTop, surfaceY + 64);
                int yEnd = Math.max(minY, surfaceY - 96);
                for (int y = yStart; y >= yEnd; y--) {
                    BlockPos p = new BlockPos(x, y, z);
                    BlockState s = world.getBlockState(p);
                    if (!s.isOf(ModBlocks.STELLARITH_CRYSTAL)) continue;
                    if (!s.getOrEmpty(StellarithCrystalBlock.NATURAL).orElse(Boolean.FALSE)) continue;
                    tops.add(p.toImmutable());
                    break;
                }
            }
        }
        return tops;
    }

    private static void trySpawnOrbsForTips(ServerWorld world, List<BlockPos> topCrystalBlocks, Random random) {
        // Process at most one spawn per connected cluster, constrained to the current chunk to avoid cascading loads
        HashSet<BlockPos> visited = new HashSet<>();
        for (BlockPos seed : topCrystalBlocks) {
            if (visited.contains(seed)) continue;
            // Collect this cluster across loaded chunks (bounded)
            List<BlockPos> cluster = collectClusterAcrossLoaded(world, seed, 2048);
            if (cluster.isEmpty()) continue;
            visited.addAll(cluster);

            // Choose a tip within this chunk with outward exposure bias
            Tip tipData = chooseTipFromCluster(world, cluster);
            if (tipData == null) continue;
            BlockPos tip = tipData.pos();
            net.minecraft.util.math.Vec3d outward = tipData.outward();
            if (outward.lengthSquared() < 1.0e-6) outward = new Vec3d(0, 1, 0);

            // Check Mirelands biome
            var biomeKey = world.getBiome(tip).getKey().orElse(null);
            if (biomeKey == null) continue;
            String path = biomeKey.getValue().getPath();
            if (!("mirelands_highlands".equals(path) || "mirelands_midlands".equals(path) || "mirelands_barrens".equals(path))) continue;

            // One-time per tip per session
            long tipKey = tip.asLong();
            if (spawnedTips.contains(tipKey)) continue;

            int count = 2 + random.nextInt(2); // 2-3 (temporary tightening)
            Vec3d n = outward.normalize();
            // Build tangent basis for jittered cloud around the tip, biased outward
            Vec3d t1;
            if (Math.abs(n.y) < 0.99) {
                t1 = n.crossProduct(new Vec3d(0, 1, 0)).normalize();
            } else {
                t1 = n.crossProduct(new Vec3d(1, 0, 0)).normalize();
            }
            Vec3d t2 = n.crossProduct(t1).normalize();
            Vec3d center = Vec3d.ofCenter(tip).add(n.multiply(1.4));
            int spawned = 0;
            for (int i = 0; i < count; i++) {
                // Disc jitter in plane perpendicular to outward normal, with slight forward spread
                double r = 0.6 + random.nextDouble() * 1.2;
                double ang = random.nextDouble() * Math.PI * 2.0;
                double fwd = 0.2 + random.nextDouble() * 0.6;
                Vec3d offset = t1.multiply(Math.cos(ang) * r).add(t2.multiply(Math.sin(ang) * r)).add(n.multiply(fwd));
                Vec3d p = center.add(offset);
                // Ensure we place into air; step outward/up if obstructed
                Vec3d safe = findSafeAir(world, p, n);
                if (safe == null) continue;
                EtherealOrbEntity e = new EtherealOrbEntity(ModEntities.ETHEREAL_ORB, world);
                e.setPersistent();
                e.refreshPositionAndAngles(safe.x, safe.y, safe.z, random.nextFloat() * 360f, 0.0f);
                if (world.spawnEntity(e)) {
                    spawned++;
                }
            }

            if (spawned > 0) {
                spawnedTips.add(tipKey);
                // Clear NATURAL and set ORBS_SPAWNED on the entire connected cluster so it won't trigger again anywhere
                for (BlockPos p : cluster) {
                    BlockState s = world.getBlockState(p);
                    if (s.isOf(ModBlocks.STELLARITH_CRYSTAL)) {
                        world.setBlockState(p, s.with(StellarithCrystalBlock.NATURAL, Boolean.FALSE).with(StellarithCrystalBlock.ORBS_SPAWNED, Boolean.TRUE), 2);
                    }
                }
            }
        }
    }

    // Collect all NATURAL crystal blocks connected to start across loaded chunks (bounded)
    private static List<BlockPos> collectClusterAcrossLoaded(ServerWorld world, BlockPos start, int max) {
        ArrayDeque<BlockPos> q = new ArrayDeque<>();
        HashSet<BlockPos> seen = new HashSet<>();
        List<BlockPos> out = new ArrayList<>();
        q.add(start);
        seen.add(start);
        while (!q.isEmpty() && out.size() < max) {
            BlockPos cur = q.poll();
            ChunkPos ch = new ChunkPos(cur);
            if (!world.isChunkLoaded(ch.x, ch.z)) continue;
            BlockState s = world.getBlockState(cur);
            if (!s.isOf(ModBlocks.STELLARITH_CRYSTAL)) continue;
            if (!s.getOrEmpty(StellarithCrystalBlock.NATURAL).orElse(Boolean.FALSE)) continue;
            if (s.getOrEmpty(StellarithCrystalBlock.ORBS_SPAWNED).orElse(Boolean.FALSE)) continue;
            out.add(cur);
            for (Direction d : Direction.values()) {
                BlockPos nxt = cur.offset(d);
                if (seen.add(nxt)) q.add(nxt);
            }
        }
        return out;
    }

    // Attempt to find a tip by picking position with fewest crystal neighbors
    private static BlockPos findSpikeTip(ServerWorld world, BlockPos seed, int maxSteps) {
        ArrayDeque<BlockPos> q = new ArrayDeque<>();
        HashSet<BlockPos> seen = new HashSet<>();
        q.add(seed);
        seen.add(seed);
        BlockPos best = seed;
        int bestNeighbors = Integer.MAX_VALUE;
        int steps = 0;
        while (!q.isEmpty() && steps < maxSteps) {
            BlockPos cur = q.poll();
            steps++;
            ChunkPos ch = new ChunkPos(cur);
            if (!world.isChunkLoaded(ch.x, ch.z)) continue;
            int neighbors = countCrystalNeighbors(world, cur);
            if (neighbors < bestNeighbors || (neighbors == bestNeighbors && cur.getY() > best.getY())) {
                best = cur;
                bestNeighbors = neighbors;
            }
            for (Direction d : Direction.values()) {
                BlockPos nxt = cur.offset(d);
                if (seen.add(nxt) && world.getBlockState(nxt).isOf(ModBlocks.STELLARITH_CRYSTAL)) q.add(nxt);
            }
        }
        return best;
    }

    private static int countCrystalNeighbors(ServerWorld world, BlockPos pos) {
        int c = 0;
        for (Direction d : Direction.values()) {
            BlockPos n = pos.offset(d);
            ChunkPos ch = new ChunkPos(n);
            if (world.isChunkLoaded(ch.x, ch.z) && world.getBlockState(n).isOf(ModBlocks.STELLARITH_CRYSTAL)) c++;
        }
        return c;
    }

    private record Tip(BlockPos pos, Vec3d outward) {}

    private static Tip chooseTipFromCluster(ServerWorld world, List<BlockPos> cluster) {
        if (cluster.isEmpty()) return null;
        // Compute centroid for tie-breaking towards endpoints
        double cx = 0, cy = 0, cz = 0;
        for (BlockPos p : cluster) { cx += p.getX(); cy += p.getY(); cz += p.getZ(); }
        cx /= cluster.size(); cy /= cluster.size(); cz /= cluster.size();

        Tip best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (BlockPos p : cluster) {
            int neighbors = 0;
            int airSides = 0;
            Vec3d outward = Vec3d.ZERO;
            for (Direction d : Direction.values()) {
                BlockPos q = p.offset(d);
                boolean isCrystal = world.getBlockState(q).isOf(ModBlocks.STELLARITH_CRYSTAL);
                if (isCrystal) neighbors++;
                else airSides++;
                if (!isCrystal) outward = outward.add(new Vec3d(d.getOffsetX(), d.getOffsetY(), d.getOffsetZ()));
            }
            // Skip downward-facing under-island tips
            int surfaceY = world.getTopPosition(net.minecraft.world.Heightmap.Type.WORLD_SURFACE_WG, new BlockPos(p.getX(), 0, p.getZ())).getY();
            if (outward.y < -0.3 && (surfaceY - p.getY()) > 8) continue;

            double dx = p.getX() - cx, dy = p.getY() - cy, dz = p.getZ() - cz;
            double dist2 = dx*dx + dy*dy + dz*dz;
            double score = airSides * 5.0 - neighbors * 2.0 + Math.sqrt(dist2) * 0.75;
            // Prefer more outward-aimed points slightly
            score += outward.length() * 0.25;
            if (score > bestScore) {
                bestScore = score;
                best = new Tip(p, outward);
            }
        }
        if (best == null) {
            // Fallback to simple search
            BlockPos fallback = findSpikeTip(world, cluster.get(0), 256);
            return fallback == null ? null : new Tip(fallback, new Vec3d(0, 1, 0));
        }
        return best;
    }

    private static Vec3d findSafeAir(ServerWorld world, Vec3d start, Vec3d outwardNormal) {
        // Try stepping outward along normal, then upward a bit
        Vec3d p = start;
        for (int i = 0; i < 10; i++) {
            BlockPos bp = BlockPos.ofFloored(p);
            if (world.getBlockState(bp).isAir()) return p;
            p = p.add(outwardNormal.multiply(0.4));
        }
        // Try small upward steps if still blocked
        p = start;
        for (int i = 0; i < 10; i++) {
            BlockPos bp = BlockPos.ofFloored(p);
            if (world.getBlockState(bp).isAir()) return p;
            p = p.add(0, 0.4, 0);
        }
        // Final fallback: place at surface
        BlockPos base = BlockPos.ofFloored(start);
        int surfaceY = world.getTopPosition(net.minecraft.world.Heightmap.Type.WORLD_SURFACE_WG, new BlockPos(base.getX(), 0, base.getZ())).getY();
        Vec3d surf = new Vec3d(base.getX() + 0.5, surfaceY + 1.2, base.getZ() + 0.5);
        if (world.getBlockState(BlockPos.ofFloored(surf)).isAir()) return surf;
        return null;
    }
}


