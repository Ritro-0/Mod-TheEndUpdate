package com.theendupdate.network;

import com.theendupdate.TheEndUpdate;
import com.theendupdate.block.EnderChrysanthemumBlock;
import com.theendupdate.block.ClosedEnderChrysanthemumBlock;
import com.theendupdate.registry.ModBlocks;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class EnderChrysanthemumCloser {
    // in-memory only, no persistence, avoids save/load freezes
    private static final Map<ServerLevel, Map<BlockPos, Long>> worldToExpiry = new HashMap<>();
    private static final Map<ServerLevel, Set<BlockPos>> worldToClosedPositions = new HashMap<>();

    // incremented on START_FLASH, reset to 0 on FLASH_ENDED
    private static final Map<ServerLevel, Integer> worldToActiveFlashCount = new HashMap<>();

    private EnderChrysanthemumCloser() {}

    public static int getActiveFlashCount(ServerLevel world) {
        return worldToActiveFlashCount.getOrDefault(world, 0);
    }

    private static void incrementActiveFlashCount(ServerLevel world) {
        int current = worldToActiveFlashCount.getOrDefault(world, 0);
        worldToActiveFlashCount.put(world, current + 1);
    }

    private static void resetActiveFlashCount(ServerLevel world) {
        worldToActiveFlashCount.put(world, 0);
    }

    // returns true if newly added, false if already tracked
    private static boolean addClosedPosition(ServerLevel world, BlockPos pos) {
        Set<BlockPos> closedSet = worldToClosedPositions.computeIfAbsent(world, w -> new HashSet<>());
        BlockPos immutablePos = pos.immutable();
        boolean wasNew = !closedSet.contains(immutablePos);
        closedSet.add(immutablePos);
        return wasNew;
    }

    private static void removeClosedPosition(ServerLevel world, BlockPos pos) {
        Set<BlockPos> closedSet = worldToClosedPositions.get(world);
        if (closedSet != null) {
            closedSet.remove(pos);
        }
    }
    
    // for manual placement (e.g. block placed while a flash is active); gives it a
    // long default expiry so it's picked up by forceReopenAll instead of expiring early
    public static void addClosedPositionManually(ServerLevel world, BlockPos pos) {
        boolean wasAdded = addClosedPosition(world, pos);

        int activeFlashes = getActiveFlashCount(world);
        if (activeFlashes > 0) {
            Map<BlockPos, Long> expiryMap = worldToExpiry.computeIfAbsent(world, w -> new HashMap<>());
            long currentTime = world.getGameTime();
            long defaultExpiry = currentTime + 1000; // 1000 ticks = 50s, outlasts any flash
            expiryMap.put(pos.immutable(), defaultExpiry);
        }
    }

    // disabled by default, full chunk scans caused load freezes
    private static final boolean ENABLE_CHUNK_SCANNING = Boolean.parseBoolean(
        System.getProperty("theendupdate.enableChrysanthemumChunkScan", "false")
    );

    private static final Map<ServerLevel, Set<net.minecraft.world.level.ChunkPos>> scannedChunks = new HashMap<>();
    private static final Set<ServerLevel> scannedWorlds = new HashSet<>();

    private static boolean shouldScanChunk(ServerLevel world, net.minecraft.world.level.ChunkPos chunkPos) {
        if (!ENABLE_CHUNK_SCANNING) return false;

        Set<net.minecraft.world.level.ChunkPos> scanned = scannedChunks.computeIfAbsent(world, w -> new HashSet<>());
        if (scanned.contains(chunkPos)) return false;

        int scanRadius = 8; // view distance
        java.util.List<net.minecraft.server.level.ServerPlayer> players = world.players();
        for (var player : players) {
            net.minecraft.world.level.ChunkPos playerChunk = new net.minecraft.world.level.ChunkPos(player.blockPosition().getX() >> 4, player.blockPosition().getZ() >> 4);
            int dx = Math.abs(chunkPos.x() - playerChunk.x());
            int dz = Math.abs(chunkPos.z() - playerChunk.z());
            if (dx <= scanRadius && dz <= scanRadius) {
                return true;
            }
        }
        
        if (chunkPos.x() == 0 && chunkPos.z() == 0) { // also scan spawn chunk on initial load
            return true;
        }

        return false;
    }

    private static int scanChunkInternal(ServerLevel world, net.minecraft.world.level.chunk.ChunkAccess chunk) {
        long startTime = System.nanoTime();
        int closedFound = 0;
        net.minecraft.world.level.ChunkPos chunkPos = chunk.getPos();

        // Y range capped to 0-256 for performance
        int minY = Math.max(0, world.getMinY());
        int maxY = Math.min(256, world.getMinY() + world.dimensionType().height() - 1);
        int minX = chunkPos.getMinBlockX();
        int maxX = chunkPos.getMaxBlockX();
        int minZ = chunkPos.getMinBlockZ();
        int maxZ = chunkPos.getMaxBlockZ();

        // world block state access is safe from this async server thread
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState blockState = world.getBlockState(pos);
                    if (blockState.is(ModBlocks.CLOSED_ENDER_CHRYSANTHEMUM) || blockState.is(ModBlocks.POTTED_CLOSED_ENDER_CHRYSANTHEMUM)) {
                        addClosedPosition(world, pos.immutable());
                        closedFound++;
                    }
                }
            }
        }

        Set<net.minecraft.world.level.ChunkPos> scanned = scannedChunks.computeIfAbsent(world, w -> new HashSet<>());
        scanned.add(chunkPos);
        
        return closedFound;
    }

    public static void scanChunkForClosed(ServerLevel world, net.minecraft.world.level.chunk.ChunkAccess chunk) {
        if (world.dimension() != net.minecraft.world.level.Level.END) return;

        net.minecraft.world.level.ChunkPos chunkPos = chunk.getPos();

        if (!shouldScanChunk(world, chunkPos)) {
            return;
        }

        // async via server executor to avoid blocking the main thread
        world.getServer().execute(() -> {
            try {
                scanChunkInternal(world, chunk);
            } catch (Throwable t) {
                com.theendupdate.TheEndUpdate.LOGGER.warn("[EndUpdate] Error scanning chunk: ", t);
            }
        });
    }
    
    // scans all loaded chunks near players, once per world, on first flash trigger
    public static void scanLoadedChunksForFirstFlash(ServerLevel world) {
        if (world.dimension() != net.minecraft.world.level.Level.END) return;
        if (!ENABLE_CHUNK_SCANNING) return;

        synchronized (scannedWorlds) {
            if (scannedWorlds.contains(world)) return;
            scannedWorlds.add(world);
        }

        world.getServer().execute(() -> {
            try {
                int closedFound = 0;

                int viewDistance = 8;
                java.util.List<net.minecraft.server.level.ServerPlayer> players = world.players();
                
                for (var player : players) {
                    net.minecraft.world.level.ChunkPos center = new net.minecraft.world.level.ChunkPos(player.blockPosition().getX() >> 4, player.blockPosition().getZ() >> 4);
                    for (int dx = -viewDistance; dx <= viewDistance; dx++) {
                        for (int dz = -viewDistance; dz <= viewDistance; dz++) {
                            net.minecraft.world.level.ChunkPos cp = new net.minecraft.world.level.ChunkPos(center.x() + dx, center.z() + dz);
                            
                            if (world.hasChunk(cp.x(), cp.z())) {
                                net.minecraft.world.level.chunk.ChunkAccess chunk = world.getChunk(cp.x(), cp.z());
                                if (chunk != null) {
                                    closedFound += scanChunkInternal(world, chunk);
                                }
                            }
                        }
                    }
                }
            } catch (Throwable t) {
                com.theendupdate.TheEndUpdate.LOGGER.warn("[EndUpdate] Error in lazy chunk scan: ", t);
            }
        });
    }

    public static void closeNearby(ServerLevel world, BlockPos center, int radius, long durationTicks) {
        if (world.dimension() != net.minecraft.world.level.Level.END) {
            TheEndUpdate.LOGGER.warn("[EndUpdate] closeNearby called outside The End dimension (current: {}), ignoring", 
                world.dimension().identifier());
            return;
        }
        
        TheEndUpdate.LOGGER.info("[EndUpdate] ========== CLOSE NEARBY FLOWERS ==========");
        TheEndUpdate.LOGGER.info("[EndUpdate] Center: {}, Radius: {}, Duration: {} ticks", center, radius, durationTicks);

        incrementActiveFlashCount(world);

        Map<BlockPos, Long> expiryMap = worldToExpiry.computeIfAbsent(world, w -> new HashMap<>());

        int r = Math.max(1, radius);
        int minX = center.getX() - r;
        int maxX = center.getX() + r;
        int minY = Math.max(world.getMinY(), center.getY() - r);
        int worldTop = world.getMinY() + world.dimensionType().height() - 1;
        int maxY = Math.min(worldTop, center.getY() + r);
        int minZ = center.getZ() - r;
        int maxZ = center.getZ() + r;

        long currentTime = world.getGameTime();
        // safety net: cap at 300 ticks (15s), longer than a typical flash + grace period,
        // so flowers reopen even if the FLASH_ENDED packet is lost
        long maxExpireAt = currentTime + 300;
        long expireAt = Math.min(currentTime + durationTicks, maxExpireAt);
        int replacedCount = 0;
        int extendedCount = 0;
        int foundCount = 0;
        int trackingAddedCount = 0;
        
        TheEndUpdate.LOGGER.info("[EndUpdate] Scanning area: X=[{}, {}], Y=[{}, {}], Z=[{}, {}]", 
            minX, maxX, minY, maxY, minZ, maxZ);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockPos immutablePos = pos.immutable();
                    BlockState blockState = world.getBlockState(pos);

                    // closedPositions is the authoritative tracking list; expiryMap only times reopening.
                    // both are updated in every branch below so forceReopenAll/tick stay consistent.
                    if (blockState.is(ModBlocks.ENDER_CHRYSANTHEMUM) || blockState.is(ModBlocks.CLOSED_ENDER_CHRYSANTHEMUM)) {
                        foundCount++;

                        if (blockState.is(ModBlocks.ENDER_CHRYSANTHEMUM)) {
                            BlockState closedState = ModBlocks.CLOSED_ENDER_CHRYSANTHEMUM.defaultBlockState()
                                .setValue(ClosedEnderChrysanthemumBlock.ATTACHMENT_FACE, blockState.getValue(EnderChrysanthemumBlock.ATTACHMENT_FACE));
                            world.setBlock(pos, closedState, Block.UPDATE_ALL);

                            Long existingExpiry = expiryMap.get(immutablePos);
                            if (existingExpiry != null && existingExpiry > currentTime) {
                                long newExpiry = Math.min(Math.max(expireAt, existingExpiry), maxExpireAt);
                                expiryMap.put(immutablePos, newExpiry);
                                extendedCount++;
                            } else {
                                expiryMap.put(immutablePos, expireAt);
                                replacedCount++;
                            }

                            boolean wasAdded = addClosedPosition(world, immutablePos);
                            if (wasAdded) trackingAddedCount++;
                        } else if (blockState.is(ModBlocks.CLOSED_ENDER_CHRYSANTHEMUM)) {
                            // already closed, just extend duration (multi-flash overlap)
                            Long existingExpiry = expiryMap.get(immutablePos);
                            if (existingExpiry != null && existingExpiry > currentTime) {
                                long newExpiry = Math.min(Math.max(expireAt, existingExpiry), maxExpireAt);
                                expiryMap.put(immutablePos, newExpiry);
                                extendedCount++;
                            } else {
                                expiryMap.put(immutablePos, expireAt);
                                extendedCount++;
                            }
                            boolean wasAdded = addClosedPosition(world, immutablePos);
                            if (wasAdded) {
                                trackingAddedCount++;
                            }
                        }
                    } else if (blockState.is(ModBlocks.POTTED_ENDER_CHRYSANTHEMUM) || blockState.is(ModBlocks.POTTED_CLOSED_ENDER_CHRYSANTHEMUM)) {
                        if (blockState.is(ModBlocks.POTTED_ENDER_CHRYSANTHEMUM)) {
                            world.setBlock(pos, ModBlocks.POTTED_CLOSED_ENDER_CHRYSANTHEMUM.defaultBlockState(), Block.UPDATE_ALL);

                            Long existingExpiry = expiryMap.get(immutablePos);
                            if (existingExpiry != null && existingExpiry > currentTime) {
                                long newExpiry = Math.min(Math.max(expireAt, existingExpiry), maxExpireAt);
                                expiryMap.put(immutablePos, newExpiry);
                                extendedCount++;
                            } else {
                                expiryMap.put(immutablePos, expireAt);
                                replacedCount++;
                            }

                            boolean wasAdded = addClosedPosition(world, immutablePos);
                            if (wasAdded) trackingAddedCount++;
                        } else if (blockState.is(ModBlocks.POTTED_CLOSED_ENDER_CHRYSANTHEMUM)) {
                            Long existingExpiry = expiryMap.get(immutablePos);
                            if (existingExpiry != null && existingExpiry > currentTime) {
                                long newExpiry = Math.min(Math.max(expireAt, existingExpiry), maxExpireAt);
                                expiryMap.put(immutablePos, newExpiry);
                                extendedCount++;
                            } else {
                                expiryMap.put(immutablePos, expireAt);
                                extendedCount++;
                            }
                            boolean wasAdded = addClosedPosition(world, immutablePos);
                            if (wasAdded) {
                                trackingAddedCount++;
                            }
                        }
                    }
                }
            }
        }

        Set<BlockPos> closedSet = worldToClosedPositions.get(world);
        int finalTrackingCount = closedSet != null ? closedSet.size() : 0;
        int finalExpiryCount = expiryMap.size();
        
        TheEndUpdate.LOGGER.info("[EndUpdate] Scan complete: found {} total chrysanthemums", foundCount);
        if (replacedCount > 0 || extendedCount > 0) {
            TheEndUpdate.LOGGER.info("[EndUpdate] Closed {} flowers (newly closed: {}, duration extended: {})", 
                replacedCount + extendedCount, replacedCount, extendedCount);
            TheEndUpdate.LOGGER.info("[EndUpdate] Tracking: {} NEW positions added to closedPositions this flash", trackingAddedCount);
            TheEndUpdate.LOGGER.info("[EndUpdate] Tracking totals: {} total in closedPositions set, {} in expiryMap", 
                finalTrackingCount, finalExpiryCount);
            // Note: finalTrackingCount may be higher than (replacedCount + extendedCount) if there are
            // flowers from previous flashes that haven't been reopened yet - this is normal for overlapping flashes
        } else if (foundCount > 0) {
            TheEndUpdate.LOGGER.info("[EndUpdate] Found {} chrysanthemums but none needed closing (already closed or invalid)", foundCount);
        } else {
            TheEndUpdate.LOGGER.info("[EndUpdate] No chrysanthemums found in scanned area");
        }
        TheEndUpdate.LOGGER.info("[EndUpdate] ==========================================");
    }

    public static void closeLoadedAroundPlayers(ServerLevel world, long durationTicks) {
        if (world.dimension() != net.minecraft.world.level.Level.END) {
            TheEndUpdate.LOGGER.warn("[EndUpdate] closeLoadedAroundPlayers called outside The End dimension (current: {}), ignoring",
                world.dimension().identifier());
            return;
        }

        TheEndUpdate.LOGGER.info("[EndUpdate] ========== CLOSE LOADED FLOWERS ==========");
        TheEndUpdate.LOGGER.info("[EndUpdate] Duration: {} ticks", durationTicks);

        incrementActiveFlashCount(world);
        Map<BlockPos, Long> expiryMap = worldToExpiry.computeIfAbsent(world, w -> new HashMap<>());

        long currentTime = world.getGameTime();
        long maxExpireAt = currentTime + 400; // safety net cap at 20s in case FLASH_ENDED is lost
        long expireAt = Math.min(currentTime + durationTicks, maxExpireAt);

        int minY = world.getMinY();
        int worldTop = world.getMinY() + world.dimensionType().height() - 1;
        int maxY = Math.min(worldTop, 255);

        // scan loaded chunks around players rather than a fixed world-space radius
        int chunkRadius = 12;
        java.util.Set<net.minecraft.world.level.ChunkPos> chunksToScan = new java.util.HashSet<>();
        for (var player : world.players()) {
            net.minecraft.world.level.ChunkPos playerChunk = new net.minecraft.world.level.ChunkPos(player.blockPosition().getX() >> 4, player.blockPosition().getZ() >> 4);
            for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
                for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                    int cx = playerChunk.x() + dx;
                    int cz = playerChunk.z() + dz;
                    if (world.hasChunk(cx, cz)) {
                        chunksToScan.add(new net.minecraft.world.level.ChunkPos(cx, cz));
                    }
                }
            }
        }

        int replacedCount = 0;
        int extendedCount = 0;
        int foundCount = 0;
        int trackingAddedCount = 0;

        TheEndUpdate.LOGGER.info("[EndUpdate] Scanning {} loaded chunks around players", chunksToScan.size());

        for (net.minecraft.world.level.ChunkPos cp : chunksToScan) {
            int minX = cp.getMinBlockX();
            int maxX = cp.getMaxBlockX();
            int minZ = cp.getMinBlockZ();
            int maxZ = cp.getMaxBlockZ();

            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        BlockPos immutablePos = pos.immutable();
                        BlockState blockState = world.getBlockState(pos);

                        if (blockState.is(ModBlocks.ENDER_CHRYSANTHEMUM) || blockState.is(ModBlocks.CLOSED_ENDER_CHRYSANTHEMUM)) {
                            foundCount++;

                            if (blockState.is(ModBlocks.ENDER_CHRYSANTHEMUM)) {
                                BlockState closedState = ModBlocks.CLOSED_ENDER_CHRYSANTHEMUM.defaultBlockState()
                                    .setValue(ClosedEnderChrysanthemumBlock.ATTACHMENT_FACE, blockState.getValue(EnderChrysanthemumBlock.ATTACHMENT_FACE));
                                world.setBlock(pos, closedState, Block.UPDATE_ALL);

                                Long existingExpiry = expiryMap.get(immutablePos);
                                if (existingExpiry != null && existingExpiry > currentTime) {
                                    long newExpiry = Math.min(Math.max(expireAt, existingExpiry), maxExpireAt);
                                    expiryMap.put(immutablePos, newExpiry);
                                    extendedCount++;
                                } else {
                                    expiryMap.put(immutablePos, expireAt);
                                    replacedCount++;
                                }
                                boolean wasAdded = addClosedPosition(world, immutablePos);
                                if (wasAdded) trackingAddedCount++;
                            } else {
                                Long existingExpiry = expiryMap.get(immutablePos);
                                if (existingExpiry != null && existingExpiry > currentTime) {
                                    long newExpiry = Math.min(Math.max(expireAt, existingExpiry), maxExpireAt);
                                    expiryMap.put(immutablePos, newExpiry);
                                    extendedCount++;
                                } else {
                                    expiryMap.put(immutablePos, expireAt);
                                    extendedCount++;
                                }
                                boolean wasAdded = addClosedPosition(world, immutablePos);
                                if (wasAdded) trackingAddedCount++;
                            }
                        } else if (blockState.is(ModBlocks.POTTED_ENDER_CHRYSANTHEMUM) || blockState.is(ModBlocks.POTTED_CLOSED_ENDER_CHRYSANTHEMUM)) {
                            if (blockState.is(ModBlocks.POTTED_ENDER_CHRYSANTHEMUM)) {
                                world.setBlock(pos, ModBlocks.POTTED_CLOSED_ENDER_CHRYSANTHEMUM.defaultBlockState(), Block.UPDATE_ALL);

                                Long existingExpiry = expiryMap.get(immutablePos);
                                if (existingExpiry != null && existingExpiry > currentTime) {
                                    long newExpiry = Math.min(Math.max(expireAt, existingExpiry), maxExpireAt);
                                    expiryMap.put(immutablePos, newExpiry);
                                    extendedCount++;
                                } else {
                                    expiryMap.put(immutablePos, expireAt);
                                    replacedCount++;
                                }
                                boolean wasAdded = addClosedPosition(world, immutablePos);
                                if (wasAdded) trackingAddedCount++;
                            } else {
                                Long existingExpiry = expiryMap.get(immutablePos);
                                if (existingExpiry != null && existingExpiry > currentTime) {
                                    long newExpiry = Math.min(Math.max(expireAt, existingExpiry), maxExpireAt);
                                    expiryMap.put(immutablePos, newExpiry);
                                    extendedCount++;
                                } else {
                                    expiryMap.put(immutablePos, expireAt);
                                    extendedCount++;
                                }
                                boolean wasAdded = addClosedPosition(world, immutablePos);
                                if (wasAdded) trackingAddedCount++;
                            }
                        }
                    }
                }
            }
        }

        Set<BlockPos> closedSet = worldToClosedPositions.get(world);
        int finalTrackingCount = closedSet != null ? closedSet.size() : 0;
        int finalExpiryCount = expiryMap.size();
        TheEndUpdate.LOGGER.info("[EndUpdate] Loaded-scan complete: found {} total chrysanthemums", foundCount);
        TheEndUpdate.LOGGER.info("[EndUpdate] Closed {} flowers (newly closed: {}, duration extended: {})",
            replacedCount + extendedCount, replacedCount, extendedCount);
        TheEndUpdate.LOGGER.info("[EndUpdate] Tracking: {} NEW positions added this flash", trackingAddedCount);
        TheEndUpdate.LOGGER.info("[EndUpdate] Tracking totals: {} total in closedPositions set, {} in expiryMap",
            finalTrackingCount, finalExpiryCount);
        TheEndUpdate.LOGGER.info("[EndUpdate] ==========================================");
    }

    public static void forceReopenAll(ServerLevel world) {
        long currentTick = world.getGameTime();
        Map<BlockPos, Long> expiryMap = worldToExpiry.get(world);
        Set<BlockPos> closedPositions = worldToClosedPositions.get(world);
        
        TheEndUpdate.LOGGER.info("[EndUpdate] ========== FORCE REOPEN ALL FLOWERS ==========");
        TheEndUpdate.LOGGER.info("[EndUpdate] World: {} (dimension: {})", 
            world.dimension().identifier(), world.dimension());
        TheEndUpdate.LOGGER.info("[EndUpdate] Current tick: {}", currentTick);
        TheEndUpdate.LOGGER.info("[EndUpdate] Tracking maps: expiryMap={} (size: {}), closedPositions={} (size: {})", 
            expiryMap != null, expiryMap != null ? expiryMap.size() : 0,
            closedPositions != null, closedPositions != null ? closedPositions.size() : 0);

        resetActiveFlashCount(world);

        if (expiryMap == null && closedPositions == null) {
            TheEndUpdate.LOGGER.info("[EndUpdate] No closed flowers to reopen");
            TheEndUpdate.LOGGER.info("[EndUpdate] ===========================================");
            return;
        }
        
        int reopenedCount = 0;
        int fromExpiryMapCount = 0;
        int fromClosedPositionsCount = 0;
        int alreadyOpenCount = 0;
        int notFoundCount = 0;

        // closedPositions is authoritative (complete list); expiryMap is just for timing
        java.util.Set<BlockPos> processedPositions = new java.util.HashSet<>();

        if (closedPositions != null && !closedPositions.isEmpty()) {
            TheEndUpdate.LOGGER.info("[EndUpdate] Processing {} positions from closedPositions set...", closedPositions.size());
            Iterator<BlockPos> closedIt = closedPositions.iterator();
            while (closedIt.hasNext()) {
                BlockPos pos = closedIt.next();
                BlockPos immutablePos = pos.immutable();
                
                if (processedPositions.contains(immutablePos)) {
                    closedIt.remove();
                    continue;
                }
                
                processedPositions.add(immutablePos);
                BlockState blockState = world.getBlockState(pos);
                
                if (blockState.is(ModBlocks.CLOSED_ENDER_CHRYSANTHEMUM)) {
                    BlockState regularState = ModBlocks.ENDER_CHRYSANTHEMUM.defaultBlockState()
                        .setValue(EnderChrysanthemumBlock.ATTACHMENT_FACE, blockState.getValue(ClosedEnderChrysanthemumBlock.ATTACHMENT_FACE));
                    world.setBlock(pos, regularState, Block.UPDATE_ALL);
                    reopenedCount++;
                    fromClosedPositionsCount++;
                } else if (blockState.is(ModBlocks.POTTED_CLOSED_ENDER_CHRYSANTHEMUM)) {
                    world.setBlock(pos, ModBlocks.POTTED_ENDER_CHRYSANTHEMUM.defaultBlockState(), Block.UPDATE_ALL);
                    reopenedCount++;
                    fromClosedPositionsCount++;
                } else if (blockState.is(ModBlocks.ENDER_CHRYSANTHEMUM) || blockState.is(ModBlocks.POTTED_ENDER_CHRYSANTHEMUM)) {
                    alreadyOpenCount++;
                } else {
                    notFoundCount++; // block changed to something else
                }

                closedIt.remove();
            }
            closedPositions.clear();
        }

        if (expiryMap != null && !expiryMap.isEmpty()) {
            int expiryMapSize = expiryMap.size();
            expiryMap.clear();
            TheEndUpdate.LOGGER.info("[EndUpdate] Cleared {} entries from expiryMap", expiryMapSize);
        }
        
        TheEndUpdate.LOGGER.info("[EndUpdate] Reopen complete: {} total flowers reopened", reopenedCount);
        if (reopenedCount > 0 || alreadyOpenCount > 0 || notFoundCount > 0) {
            TheEndUpdate.LOGGER.info("[EndUpdate] Breakdown: {} from closedPositions set, {} already open, {} not found (block changed)", 
                fromClosedPositionsCount, alreadyOpenCount, notFoundCount);
        }
        if (reopenedCount == 0 && (closedPositions == null || closedPositions.isEmpty()) && (expiryMap == null || expiryMap.isEmpty())) {
            TheEndUpdate.LOGGER.warn("[EndUpdate] WARNING: No flowers were reopened, but tracking was empty. This may indicate a tracking issue.");
        }
        TheEndUpdate.LOGGER.info("[EndUpdate] ==============================================");
    }

    // safety net fallback, reopens expired flowers if FLASH_ENDED was ever lost
    public static void tick(ServerLevel world) {
        if (world.dimension() != net.minecraft.world.level.Level.END) return;

        Map<BlockPos, Long> expiryMap = worldToExpiry.get(world);
        if (expiryMap == null || expiryMap.isEmpty()) {
            return;
        }
        
        long now = world.getGameTime();
        Iterator<Map.Entry<BlockPos, Long>> it = expiryMap.entrySet().iterator();
        int expiredCount = 0;
        
        while (it.hasNext()) {
            Map.Entry<BlockPos, Long> entry = it.next();
            BlockPos pos = entry.getKey();
            long expiryTime = entry.getValue();

            if (now >= expiryTime) {
                expiredCount++;
                BlockState blockState = world.getBlockState(pos);
                if (blockState.is(ModBlocks.CLOSED_ENDER_CHRYSANTHEMUM)) {
                    BlockState regularState = ModBlocks.ENDER_CHRYSANTHEMUM.defaultBlockState()
                        .setValue(EnderChrysanthemumBlock.ATTACHMENT_FACE, blockState.getValue(ClosedEnderChrysanthemumBlock.ATTACHMENT_FACE));
                    world.setBlock(pos, regularState, Block.UPDATE_ALL);
                    removeClosedPosition(world, pos);
                } else if (blockState.is(ModBlocks.POTTED_CLOSED_ENDER_CHRYSANTHEMUM)) {
                    world.setBlock(pos, ModBlocks.POTTED_ENDER_CHRYSANTHEMUM.defaultBlockState(), Block.UPDATE_ALL);
                    removeClosedPosition(world, pos);
                } else {
                    // already open or changed to something else, either way stop tracking it
                    removeClosedPosition(world, pos);
                }
                it.remove();
            }
        }
        
        if (expiredCount > 0) {
            TheEndUpdate.LOGGER.info("[EndUpdate] Safety net: Reopened {} expired flowers (current time: {})", expiredCount, now);
        }
    }
}


