package com.theendupdate.world;

import com.theendupdate.block.StellarithCrystalBlock;
import com.theendupdate.entity.EtherealOrbEntity;
import com.theendupdate.registry.ModBlocks;
import com.theendupdate.registry.ModEntities;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
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
        // tick-budgeted scanning near players avoids heavy work during chunk load
        ServerTickEvents.END_LEVEL_TICK.register(EtherealOrbOnCrystalsSpawner::onWorldTick);
        // registered but intentionally a no-op, doing work here caused freezes
        ServerChunkEvents.CHUNK_LOAD.register((level, chunk, generated) -> {});
    }

    private static void onWorldTick(ServerLevel world) {
        if (!world.dimension().equals(Level.END)) return;
        try {
            List<net.minecraft.server.level.ServerPlayer> players = world.players();
            for (var player : players) {
                ChunkPos center = new ChunkPos(player.blockPosition().getX() >> 4, player.blockPosition().getZ() >> 4);
                for (int dx = -2; dx <= 2; dx++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        ChunkPos cp = new ChunkPos(center.x() + dx, center.z() + dz);
                        long key = chunkKey(cp);
                        if (processedChunks.contains(key)) continue;
                        queue.add(key);
                    }
                }
            }

            // budget: at most 2 chunks per tick, avoids frame stalls
            int budget = 2;
            java.util.Iterator<Long> it = queue.iterator();
            while (budget-- > 0 && it.hasNext()) {
                long key = it.next();
                it.remove();
                int cx = ChunkPos.getX(key);
                int cz = ChunkPos.getZ(key);
                ChunkPos cp = new ChunkPos(cx, cz);
                if (!world.hasChunk(cp.x(), cp.z())) {
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

    private static long chunkKey(ChunkPos pos) {
        return ChunkPos.pack(pos.x(), pos.z());
    }

    // top-most crystal block per x,z column, likely part of a natural spike
    private static List<BlockPos> findNaturalCrystalTopsNearSurface(ServerLevel world, ChunkPos chunkPos) {
        List<BlockPos> tops = new ArrayList<>();
        int minY = world.getMinY();
        int worldTop = minY + world.getHeight() - 1;
        int startX = chunkPos.getMinBlockX();
        int startZ = chunkPos.getMinBlockZ();
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = startX + dx;
                int z = startZ + dz;
                // scan around terrain surface only, keeps cost down
                int surfaceY = world.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG, new BlockPos(x, 0, z)).getY();
                int yStart = Math.min(worldTop, surfaceY + 64);
                int yEnd = Math.max(minY, surfaceY - 96);
                for (int y = yStart; y >= yEnd; y--) {
                    BlockPos p = new BlockPos(x, y, z);
                    BlockState s = world.getBlockState(p);
                    if (!s.is(ModBlocks.STELLARITH_CRYSTAL)) continue;
                    if (!s.getOptionalValue(StellarithCrystalBlock.NATURAL).orElse(Boolean.FALSE)) continue;
                    tops.add(p.immutable());
                    break;
                }
            }
        }
        return tops;
    }

    private static void trySpawnOrbsForTips(ServerLevel world, List<BlockPos> topCrystalBlocks, RandomSource random) {
        // one spawn per connected cluster, constrained to this chunk to avoid cascading loads
        HashSet<BlockPos> visited = new HashSet<>();
        for (BlockPos seed : topCrystalBlocks) {
            if (visited.contains(seed)) continue;
            List<BlockPos> cluster = collectClusterAcrossLoaded(world, seed, 2048);
            if (cluster.isEmpty()) continue;
            visited.addAll(cluster);

            // pick a tip with outward exposure bias
            Tip tipData = chooseTipFromCluster(world, cluster);
            if (tipData == null) continue;
            BlockPos tip = tipData.pos();
            net.minecraft.world.phys.Vec3 outward = tipData.outward();
            if (outward.lengthSqr() < 1.0e-6) outward = new Vec3(0, 1, 0);

			// Mirelands only, disabled in Shadowlands
            var biomeKey = world.getBiome(tip).unwrapKey().orElse(null);
            if (biomeKey == null) continue;
            String path = biomeKey.identifier().getPath();
			if (!("mirelands_highlands".equals(path) || "mirelands_midlands".equals(path) || "mirelands_barrens".equals(path))) continue;

            // one-time per tip per session
            long tipKey = tip.asLong();
            if (spawnedTips.contains(tipKey)) continue;

            int count = 2 + random.nextInt(2); // 2-3, temporarily tightened
            Vec3 n = outward.normalize();
            // tangent basis for a jittered cloud around the tip, biased outward
            Vec3 t1;
            if (Math.abs(n.y) < 0.99) {
                t1 = n.cross(new Vec3(0, 1, 0)).normalize();
            } else {
                t1 = n.cross(new Vec3(1, 0, 0)).normalize();
            }
            Vec3 t2 = n.cross(t1).normalize();
            Vec3 center = Vec3.atCenterOf(tip).add(n.scale(1.4));
            int spawned = 0;
            for (int i = 0; i < count; i++) {
                // disc jitter perpendicular to outward normal, slight forward spread
                double r = 0.6 + random.nextDouble() * 1.2;
                double ang = random.nextDouble() * Math.PI * 2.0;
                double fwd = 0.2 + random.nextDouble() * 0.6;
                Vec3 offset = t1.scale(Math.cos(ang) * r).add(t2.scale(Math.sin(ang) * r)).add(n.scale(fwd));
                Vec3 p = center.add(offset);
                // step outward/up if obstructed, to land in air
                Vec3 safe = findSafeAir(world, p, n);
                if (safe == null) continue;
                EtherealOrbEntity e = new EtherealOrbEntity(ModEntities.ETHEREAL_ORB, world);
                e.setPersistenceRequired();
                e.snapTo(safe.x, safe.y, safe.z, random.nextFloat() * 360f, 0.0f);
                if (world.addFreshEntity(e)) {
                    spawned++;
                }
            }

            if (spawned > 0) {
                spawnedTips.add(tipKey);
                // clear NATURAL, set ORBS_SPAWNED across the whole cluster so it never retriggers
                for (BlockPos p : cluster) {
                    BlockState s = world.getBlockState(p);
                    if (s.is(ModBlocks.STELLARITH_CRYSTAL)) {
                        world.setBlock(p, s.setValue(StellarithCrystalBlock.NATURAL, Boolean.FALSE).setValue(StellarithCrystalBlock.ORBS_SPAWNED, Boolean.TRUE), 2);
                    }
                }
            }
        }
    }

    // all NATURAL crystal blocks connected to start, across loaded chunks (bounded)
    private static List<BlockPos> collectClusterAcrossLoaded(ServerLevel world, BlockPos start, int max) {
        ArrayDeque<BlockPos> q = new ArrayDeque<>();
        HashSet<BlockPos> seen = new HashSet<>();
        List<BlockPos> out = new ArrayList<>();
        q.add(start);
        seen.add(start);
        while (!q.isEmpty() && out.size() < max) {
            BlockPos cur = q.poll();
            ChunkPos ch = new ChunkPos(cur.getX() >> 4, cur.getZ() >> 4);
            if (!world.hasChunk(ch.x(), ch.z())) continue;
            BlockState s = world.getBlockState(cur);
            if (!s.is(ModBlocks.STELLARITH_CRYSTAL)) continue;
            if (!s.getOptionalValue(StellarithCrystalBlock.NATURAL).orElse(Boolean.FALSE)) continue;
            if (s.getOptionalValue(StellarithCrystalBlock.ORBS_SPAWNED).orElse(Boolean.FALSE)) continue;
            out.add(cur);
            for (Direction d : Direction.values()) {
                BlockPos nxt = cur.relative(d);
                if (seen.add(nxt)) q.add(nxt);
            }
        }
        return out;
    }

    // tip = the position with fewest crystal neighbors
    private static BlockPos findSpikeTip(ServerLevel world, BlockPos seed, int maxSteps) {
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
            ChunkPos ch = new ChunkPos(cur.getX() >> 4, cur.getZ() >> 4);
            if (!world.hasChunk(ch.x(), ch.z())) continue;
            int neighbors = countCrystalNeighbors(world, cur);
            if (neighbors < bestNeighbors || (neighbors == bestNeighbors && cur.getY() > best.getY())) {
                best = cur;
                bestNeighbors = neighbors;
            }
            for (Direction d : Direction.values()) {
                BlockPos nxt = cur.relative(d);
                if (seen.add(nxt) && world.getBlockState(nxt).is(ModBlocks.STELLARITH_CRYSTAL)) q.add(nxt);
            }
        }
        return best;
    }

    private static int countCrystalNeighbors(ServerLevel world, BlockPos pos) {
        int c = 0;
        for (Direction d : Direction.values()) {
            BlockPos n = pos.relative(d);
            ChunkPos ch = new ChunkPos(n.getX() >> 4, n.getZ() >> 4);
            if (world.hasChunk(ch.x(), ch.z()) && world.getBlockState(n).is(ModBlocks.STELLARITH_CRYSTAL)) c++;
        }
        return c;
    }

    private record Tip(BlockPos pos, Vec3 outward) {}

    private static Tip chooseTipFromCluster(ServerLevel world, List<BlockPos> cluster) {
        if (cluster.isEmpty()) return null;
        // centroid, used to bias scoring toward endpoints
        double cx = 0, cy = 0, cz = 0;
        for (BlockPos p : cluster) { cx += p.getX(); cy += p.getY(); cz += p.getZ(); }
        cx /= cluster.size(); cy /= cluster.size(); cz /= cluster.size();

        Tip best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (BlockPos p : cluster) {
            int neighbors = 0;
            int airSides = 0;
            Vec3 outward = Vec3.ZERO;
            for (Direction d : Direction.values()) {
                BlockPos q = p.relative(d);
                boolean isCrystal = world.getBlockState(q).is(ModBlocks.STELLARITH_CRYSTAL);
                if (isCrystal) neighbors++;
                else airSides++;
                if (!isCrystal) outward = outward.add(new Vec3(d.getStepX(), d.getStepY(), d.getStepZ()));
            }
            // skip downward-facing under-island tips
            int surfaceY = world.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG, new BlockPos(p.getX(), 0, p.getZ())).getY();
            if (outward.y < -0.3 && (surfaceY - p.getY()) > 8) continue;

            double dx = p.getX() - cx, dy = p.getY() - cy, dz = p.getZ() - cz;
            double dist2 = dx*dx + dy*dy + dz*dz;
            double score = airSides * 5.0 - neighbors * 2.0 + Math.sqrt(dist2) * 0.75;
            // slightly prefer more outward-aimed points
            score += outward.length() * 0.25;
            if (score > bestScore) {
                bestScore = score;
                best = new Tip(p, outward);
            }
        }
        if (best == null) {
            // fallback to simple search
            BlockPos fallback = findSpikeTip(world, cluster.get(0), 256);
            return fallback == null ? null : new Tip(fallback, new Vec3(0, 1, 0));
        }
        return best;
    }

    private static Vec3 findSafeAir(ServerLevel world, Vec3 start, Vec3 outwardNormal) {
        // step outward along normal first
        Vec3 p = start;
        for (int i = 0; i < 10; i++) {
            BlockPos bp = BlockPos.containing(p);
            if (world.getBlockState(bp).isAir()) return p;
            p = p.add(outwardNormal.scale(0.4));
        }
        // still blocked, try small upward steps
        p = start;
        for (int i = 0; i < 10; i++) {
            BlockPos bp = BlockPos.containing(p);
            if (world.getBlockState(bp).isAir()) return p;
            p = p.add(0, 0.4, 0);
        }
        // final fallback: place at surface
        BlockPos base = BlockPos.containing(start);
        int surfaceY = world.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG, new BlockPos(base.getX(), 0, base.getZ())).getY();
        Vec3 surf = new Vec3(base.getX() + 0.5, surfaceY + 1.2, base.getZ() + 0.5);
        if (world.getBlockState(BlockPos.containing(surf)).isAir()) return surf;
        return null;
    }
}


