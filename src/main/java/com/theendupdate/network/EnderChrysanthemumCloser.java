package com.theendupdate.network;

import com.theendupdate.TemplateMod;
import com.theendupdate.block.EnderChrysanthemumBlock;
import com.theendupdate.block.ClosedEnderChrysanthemumBlock;
import com.theendupdate.registry.ModBlocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.BlockState;
import net.minecraft.block.Block;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public final class EnderChrysanthemumCloser {
    // Simple in-memory tracking (no persistence to avoid save/load freezes)
    private static final Map<ServerWorld, Map<BlockPos, Long>> worldToExpiry = new HashMap<>();
    private static final Map<ServerWorld, Set<BlockPos>> worldToClosedPositions = new HashMap<>();
    
    // Track active flash count per world (incremented on START_FLASH, reset to 0 on FLASH_ENDED)
    private static final Map<ServerWorld, Integer> worldToActiveFlashCount = new HashMap<>();

    private EnderChrysanthemumCloser() {}
    
    // Get active flash count for a world
    public static int getActiveFlashCount(ServerWorld world) {
        return worldToActiveFlashCount.getOrDefault(world, 0);
    }
    
    // Increment active flash count
    private static void incrementActiveFlashCount(ServerWorld world) {
        int current = worldToActiveFlashCount.getOrDefault(world, 0);
        worldToActiveFlashCount.put(world, current + 1);
    }
    
    // Reset active flash count to 0
    private static void resetActiveFlashCount(ServerWorld world) {
        worldToActiveFlashCount.put(world, 0);
    }
    
    // Add a closed position to global tracking
    // Returns true if the position was newly added, false if it was already tracked
    private static boolean addClosedPosition(ServerWorld world, BlockPos pos) {
        Set<BlockPos> closedSet = worldToClosedPositions.computeIfAbsent(world, w -> new HashSet<>());
        BlockPos immutablePos = pos.toImmutable();
        boolean wasNew = !closedSet.contains(immutablePos);
        closedSet.add(immutablePos);
        return wasNew;
    }
    
    // Remove a closed position from global tracking
    private static void removeClosedPosition(ServerWorld world, BlockPos pos) {
        Set<BlockPos> closedSet = worldToClosedPositions.get(world);
        if (closedSet != null) {
            closedSet.remove(pos);
        }
    }
    
    // Public method for adding closed positions manually (e.g., from block placement)
    // If there's an active flash, also add to expiryMap with a long default expiry
    public static void addClosedPositionManually(ServerWorld world, BlockPos pos) {
        boolean wasAdded = addClosedPosition(world, pos);
        
        // If there's an active flash, also add to expiryMap so it gets reopened on FLASH_ENDED
        int activeFlashes = getActiveFlashCount(world);
        if (activeFlashes > 0) {
            Map<BlockPos, Long> expiryMap = worldToExpiry.computeIfAbsent(world, w -> new HashMap<>());
            long currentTime = world.getTime();
            // Use a long default expiry (1000 ticks = 50 seconds) so it won't expire before flash ends
            // This ensures it's included in the force reopen
            long defaultExpiry = currentTime + 1000;
            expiryMap.put(pos.toImmutable(), defaultExpiry);
        }
    }
    
    // Config flag for chunk scanning (disabled by default to prevent load freezes)
    private static final boolean ENABLE_CHUNK_SCANNING = false;
    
    // Track which chunks have been scanned
    private static final Map<ServerWorld, Set<net.minecraft.util.math.ChunkPos>> scannedChunks = new HashMap<>();
    
    // Track which worlds have had their first flash lazy scan
    private static final Set<ServerWorld> scannedWorlds = new HashSet<>();
    
    // Check if chunk should be scanned (near players/spawn, within view distance)
    private static boolean shouldScanChunk(ServerWorld world, net.minecraft.util.math.ChunkPos chunkPos) {
        if (!ENABLE_CHUNK_SCANNING) return false;
        
        // Check if already scanned
        Set<net.minecraft.util.math.ChunkPos> scanned = scannedChunks.computeIfAbsent(world, w -> new HashSet<>());
        if (scanned.contains(chunkPos)) return false;
        
        // Check if chunk is near players (within 8 chunks = view distance)
        int scanRadius = 8;
        java.util.List<net.minecraft.server.network.ServerPlayerEntity> players = world.getPlayers();
        for (var player : players) {
            net.minecraft.util.math.ChunkPos playerChunk = new net.minecraft.util.math.ChunkPos(player.getBlockPos());
            int dx = Math.abs(chunkPos.x - playerChunk.x);
            int dz = Math.abs(chunkPos.z - playerChunk.z);
            if (dx <= scanRadius && dz <= scanRadius) {
                return true;
            }
        }
        
        // Also scan spawn chunks (0,0) on initial load
        if (chunkPos.x == 0 && chunkPos.z == 0) {
            return true;
        }
        
        return false;
    }
    
    // Optimized chunk scan with performance monitoring
    private static int scanChunkInternal(ServerWorld world, net.minecraft.world.chunk.Chunk chunk) {
        long startTime = System.nanoTime();
        int closedFound = 0;
        net.minecraft.util.math.ChunkPos chunkPos = chunk.getPos();
        
        // Limit Y range to 0-256 for performance
        int minY = Math.max(0, world.getBottomY());
        int maxY = Math.min(256, world.getBottomY() + world.getDimension().height() - 1);
        int minX = chunkPos.getStartX();
        int maxX = chunkPos.getEndX();
        int minZ = chunkPos.getStartZ();
        int maxZ = chunkPos.getEndZ();
        
        // Use world block state access (safe in async server thread)
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState blockState = world.getBlockState(pos);
                    if (blockState.isOf(ModBlocks.CLOSED_ENDER_CHRYSANTHEMUM) || blockState.isOf(ModBlocks.POTTED_CLOSED_ENDER_CHRYSANTHEMUM)) {
                        addClosedPosition(world, pos.toImmutable());
                        closedFound++;
                    }
                }
            }
        }
        
        // Mark as scanned
        Set<net.minecraft.util.math.ChunkPos> scanned = scannedChunks.computeIfAbsent(world, w -> new HashSet<>());
        scanned.add(chunkPos);
        
        return closedFound;
    }
    
    // Async chunk scanning (called from chunk load event)
    public static void scanChunkForClosed(ServerWorld world, net.minecraft.world.chunk.Chunk chunk) {
        // End dimension check
        if (world.getRegistryKey() != net.minecraft.world.World.END) return;
        
        net.minecraft.util.math.ChunkPos chunkPos = chunk.getPos();
        
        // Check if should scan (near players or spawn)
        if (!shouldScanChunk(world, chunkPos)) {
            return;
        }
        
        // Async scan using server executor to avoid blocking main thread
        world.getServer().execute(() -> {
            try {
                scanChunkInternal(world, chunk);
            } catch (Throwable t) {
                com.theendupdate.TemplateMod.LOGGER.warn("[EndUpdate] Error scanning chunk: ", t);
            }
        });
    }
    
    // Lazy scan: scan all loaded chunks on first flash trigger
    public static void scanLoadedChunksForFirstFlash(ServerWorld world) {
        if (world.getRegistryKey() != net.minecraft.world.World.END) return;
        if (!ENABLE_CHUNK_SCANNING) return;
        
        // Only scan once per world
        synchronized (scannedWorlds) {
            if (scannedWorlds.contains(world)) return;
            scannedWorlds.add(world);
        }
        
        // Async scan all loaded chunks near players
        world.getServer().execute(() -> {
            try {
                int closedFound = 0;
                
                // Get view distance (default 8 chunks)
                int viewDistance = 8;
                java.util.List<net.minecraft.server.network.ServerPlayerEntity> players = world.getPlayers();
                
                for (var player : players) {
                    net.minecraft.util.math.ChunkPos center = new net.minecraft.util.math.ChunkPos(player.getBlockPos());
                    for (int dx = -viewDistance; dx <= viewDistance; dx++) {
                        for (int dz = -viewDistance; dz <= viewDistance; dz++) {
                            net.minecraft.util.math.ChunkPos cp = new net.minecraft.util.math.ChunkPos(center.x + dx, center.z + dz);
                            
                            if (world.isChunkLoaded(cp.x, cp.z)) {
                                net.minecraft.world.chunk.Chunk chunk = world.getChunk(cp.x, cp.z);
                                if (chunk != null) {
                                    closedFound += scanChunkInternal(world, chunk);
                                }
                            }
                        }
                    }
                }
            } catch (Throwable t) {
                com.theendupdate.TemplateMod.LOGGER.warn("[EndUpdate] Error in lazy chunk scan: ", t);
            }
        });
    }

    public static void closeNearby(ServerWorld world, BlockPos center, int radius, long durationTicks) {
        // Only process in The End dimension
        if (world.getRegistryKey() != net.minecraft.world.World.END) {
            TemplateMod.LOGGER.warn("[EndUpdate] closeNearby called outside The End dimension (current: {}), ignoring", 
                world.getRegistryKey().getValue());
            return;
        }
        
        TemplateMod.LOGGER.info("[EndUpdate] ========== CLOSE NEARBY FLOWERS ==========");
        TemplateMod.LOGGER.info("[EndUpdate] Center: {}, Radius: {}, Duration: {} ticks", center, radius, durationTicks);
        
        // Increment active flash count
        incrementActiveFlashCount(world);
        
        Map<BlockPos, Long> expiryMap = worldToExpiry.computeIfAbsent(world, w -> new HashMap<>());
        
        int r = Math.max(1, radius);
        int minX = center.getX() - r;
        int maxX = center.getX() + r;
        int minY = Math.max(world.getBottomY(), center.getY() - r);
        int worldTop = world.getBottomY() + world.getDimension().height() - 1;
        int maxY = Math.min(worldTop, center.getY() + r);
        int minZ = center.getZ() - r;
        int maxZ = center.getZ() + r;

        long currentTime = world.getTime();
        // Safety net: cap expiry at 300 ticks (15 seconds) - longer than typical flash + grace period
        // This ensures flowers will reopen even if FLASH_ENDED packet is lost
        long maxExpireAt = currentTime + 300;
        long expireAt = Math.min(currentTime + durationTicks, maxExpireAt);
        int replacedCount = 0;
        int extendedCount = 0;
        int foundCount = 0;
        int trackingAddedCount = 0;
        
        TemplateMod.LOGGER.info("[EndUpdate] Scanning area: X=[{}, {}], Y=[{}, {}], Z=[{}, {}]", 
            minX, maxX, minY, maxY, minZ, maxZ);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockPos immutablePos = pos.toImmutable();
                    BlockState blockState = world.getBlockState(pos);
                    
                    if (blockState.isOf(ModBlocks.ENDER_CHRYSANTHEMUM) || blockState.isOf(ModBlocks.CLOSED_ENDER_CHRYSANTHEMUM)) {
                        foundCount++;
                        
                        if (blockState.isOf(ModBlocks.ENDER_CHRYSANTHEMUM)) {
                            // Replace with closed version, preserving facing direction
                            // Use NOTIFY_ALL to ensure all clients are notified of state changes
                            BlockState closedState = ModBlocks.CLOSED_ENDER_CHRYSANTHEMUM.getDefaultState()
                                .with(ClosedEnderChrysanthemumBlock.ATTACHMENT_FACE, blockState.get(EnderChrysanthemumBlock.ATTACHMENT_FACE));
                            world.setBlockState(pos, closedState, Block.NOTIFY_ALL);
                            
                            // If already scheduled, extend the duration (multi-flash handling)
                            Long existingExpiry = expiryMap.get(immutablePos);
                            if (existingExpiry != null && existingExpiry > currentTime) {
                                // Already closed - extend duration but cap at maxExpireAt
                                long newExpiry = Math.min(Math.max(expireAt, existingExpiry), maxExpireAt);
                                expiryMap.put(immutablePos, newExpiry);
                                extendedCount++;
                            } else {
                                expiryMap.put(immutablePos, expireAt);
                                replacedCount++;
                            }
                            
                            // ALWAYS add to closedPositions tracking - this is the authoritative list
                            boolean wasAdded = addClosedPosition(world, immutablePos);
                            if (wasAdded) trackingAddedCount++;
                        } else if (blockState.isOf(ModBlocks.CLOSED_ENDER_CHRYSANTHEMUM)) {
                            // Already closed - extend duration if needed
                            // CRITICAL: Always update expiryMap AND closedPositions for extended flowers
                            Long existingExpiry = expiryMap.get(immutablePos);
                            if (existingExpiry != null && existingExpiry > currentTime) {
                                // Extend to the later of the two, but cap at maxExpireAt
                                long newExpiry = Math.min(Math.max(expireAt, existingExpiry), maxExpireAt);
                                expiryMap.put(immutablePos, newExpiry);
                                extendedCount++;
                            } else {
                                // Not in map or expired - add it with new duration
                                expiryMap.put(immutablePos, expireAt);
                                extendedCount++;
                            }
                            // ALWAYS add to closedPositions tracking - this is the authoritative list
                            // HashSet automatically handles duplicates, but we want to ensure it's tracked
                            boolean wasAdded = addClosedPosition(world, immutablePos);
                            if (wasAdded) {
                                trackingAddedCount++;
                            }
                        }
                    } else if (blockState.isOf(ModBlocks.POTTED_ENDER_CHRYSANTHEMUM) || blockState.isOf(ModBlocks.POTTED_CLOSED_ENDER_CHRYSANTHEMUM)) {
                        // Handle potted variants
                        if (blockState.isOf(ModBlocks.POTTED_ENDER_CHRYSANTHEMUM)) {
                            // Replace with potted closed version
                            // Use NOTIFY_ALL to ensure all clients are notified of state changes
                            world.setBlockState(pos, ModBlocks.POTTED_CLOSED_ENDER_CHRYSANTHEMUM.getDefaultState(), Block.NOTIFY_ALL);
                            
                            // If already scheduled, extend the duration (multi-flash handling)
                            Long existingExpiry = expiryMap.get(immutablePos);
                            if (existingExpiry != null && existingExpiry > currentTime) {
                                // Already closed - extend duration but cap at maxExpireAt
                                long newExpiry = Math.min(Math.max(expireAt, existingExpiry), maxExpireAt);
                                expiryMap.put(immutablePos, newExpiry);
                                extendedCount++;
                            } else {
                                expiryMap.put(immutablePos, expireAt);
                                replacedCount++;
                            }
                            
                            // ALWAYS add to closedPositions tracking - this is the authoritative list
                            boolean wasAdded = addClosedPosition(world, immutablePos);
                            if (wasAdded) trackingAddedCount++;
                        } else if (blockState.isOf(ModBlocks.POTTED_CLOSED_ENDER_CHRYSANTHEMUM)) {
                            // Already closed - extend duration if needed
                            // CRITICAL: Always update expiryMap AND closedPositions for extended flowers
                            Long existingExpiry = expiryMap.get(immutablePos);
                            if (existingExpiry != null && existingExpiry > currentTime) {
                                // Extend to the later of the two, but cap at maxExpireAt
                                long newExpiry = Math.min(Math.max(expireAt, existingExpiry), maxExpireAt);
                                expiryMap.put(immutablePos, newExpiry);
                                extendedCount++;
                            } else {
                                // Not in map or expired - add it with new duration
                                expiryMap.put(immutablePos, expireAt);
                                extendedCount++;
                            }
                            // ALWAYS add to closedPositions tracking - this is the authoritative list
                            // HashSet automatically handles duplicates, but we want to ensure it's tracked
                            boolean wasAdded = addClosedPosition(world, immutablePos);
                            if (wasAdded) {
                                trackingAddedCount++;
                            }
                        }
                    }
                }
            }
        }
        
        // Get final tracking counts for verification
        Set<BlockPos> closedSet = worldToClosedPositions.get(world);
        int finalTrackingCount = closedSet != null ? closedSet.size() : 0;
        int finalExpiryCount = expiryMap.size();
        
        TemplateMod.LOGGER.info("[EndUpdate] Scan complete: found {} total chrysanthemums", foundCount);
        if (replacedCount > 0 || extendedCount > 0) {
            TemplateMod.LOGGER.info("[EndUpdate] Closed {} flowers (newly closed: {}, duration extended: {})", 
                replacedCount + extendedCount, replacedCount, extendedCount);
            TemplateMod.LOGGER.info("[EndUpdate] Tracking: {} NEW positions added to closedPositions this flash", trackingAddedCount);
            TemplateMod.LOGGER.info("[EndUpdate] Tracking totals: {} total in closedPositions set, {} in expiryMap", 
                finalTrackingCount, finalExpiryCount);
            // Note: finalTrackingCount may be higher than (replacedCount + extendedCount) if there are
            // flowers from previous flashes that haven't been reopened yet - this is normal for overlapping flashes
        } else if (foundCount > 0) {
            TemplateMod.LOGGER.info("[EndUpdate] Found {} chrysanthemums but none needed closing (already closed or invalid)", foundCount);
        } else {
            TemplateMod.LOGGER.info("[EndUpdate] No chrysanthemums found in scanned area");
        }
        TemplateMod.LOGGER.info("[EndUpdate] ==========================================");
    }

    public static void forceReopenAll(ServerWorld world) {
        long currentTick = world.getTime();
        Map<BlockPos, Long> expiryMap = worldToExpiry.get(world);
        Set<BlockPos> closedPositions = worldToClosedPositions.get(world);
        
        TemplateMod.LOGGER.info("[EndUpdate] ========== FORCE REOPEN ALL FLOWERS ==========");
        TemplateMod.LOGGER.info("[EndUpdate] World: {} (dimension: {})", 
            world.getRegistryKey().getValue(), world.getRegistryKey());
        TemplateMod.LOGGER.info("[EndUpdate] Current tick: {}", currentTick);
        TemplateMod.LOGGER.info("[EndUpdate] Tracking maps: expiryMap={} (size: {}), closedPositions={} (size: {})", 
            expiryMap != null, expiryMap != null ? expiryMap.size() : 0,
            closedPositions != null, closedPositions != null ? closedPositions.size() : 0);
        
        // Reset active flash count to 0
        resetActiveFlashCount(world);
        
        if (expiryMap == null && closedPositions == null) {
            TemplateMod.LOGGER.info("[EndUpdate] No closed flowers to reopen");
            TemplateMod.LOGGER.info("[EndUpdate] ===========================================");
            return;
        }
        
        int reopenedCount = 0;
        int fromExpiryMapCount = 0;
        int fromClosedPositionsCount = 0;
        int alreadyOpenCount = 0;
        int notFoundCount = 0;
        
        // Use closedPositions as the authoritative source - it should contain ALL closed flowers
        // expiryMap is just for timing, but closedPositions is the complete list
        java.util.Set<BlockPos> processedPositions = new java.util.HashSet<>();
        
        // First, process all positions from closedPositions (authoritative list)
        if (closedPositions != null && !closedPositions.isEmpty()) {
            TemplateMod.LOGGER.info("[EndUpdate] Processing {} positions from closedPositions set...", closedPositions.size());
            Iterator<BlockPos> closedIt = closedPositions.iterator();
            while (closedIt.hasNext()) {
                BlockPos pos = closedIt.next();
                BlockPos immutablePos = pos.toImmutable();
                
                if (processedPositions.contains(immutablePos)) {
                    closedIt.remove();
                    continue;
                }
                
                processedPositions.add(immutablePos);
                BlockState blockState = world.getBlockState(pos);
                
                if (blockState.isOf(ModBlocks.CLOSED_ENDER_CHRYSANTHEMUM)) {
                    BlockState regularState = ModBlocks.ENDER_CHRYSANTHEMUM.getDefaultState()
                        .with(EnderChrysanthemumBlock.ATTACHMENT_FACE, blockState.get(ClosedEnderChrysanthemumBlock.ATTACHMENT_FACE));
                    world.setBlockState(pos, regularState, Block.NOTIFY_ALL);
                    reopenedCount++;
                    fromClosedPositionsCount++;
                } else if (blockState.isOf(ModBlocks.POTTED_CLOSED_ENDER_CHRYSANTHEMUM)) {
                    world.setBlockState(pos, ModBlocks.POTTED_ENDER_CHRYSANTHEMUM.getDefaultState(), Block.NOTIFY_ALL);
                    reopenedCount++;
                    fromClosedPositionsCount++;
                } else if (blockState.isOf(ModBlocks.ENDER_CHRYSANTHEMUM) || blockState.isOf(ModBlocks.POTTED_ENDER_CHRYSANTHEMUM)) {
                    // Already open - just remove from tracking
                    alreadyOpenCount++;
                } else {
                    // Block changed to something else
                    notFoundCount++;
                }
                
                closedIt.remove();
            }
            closedPositions.clear();
        }
        
        // Clear expiry map as well (it's just for timing, not authoritative)
        if (expiryMap != null && !expiryMap.isEmpty()) {
            int expiryMapSize = expiryMap.size();
            expiryMap.clear();
            TemplateMod.LOGGER.info("[EndUpdate] Cleared {} entries from expiryMap", expiryMapSize);
        }
        
        TemplateMod.LOGGER.info("[EndUpdate] Reopen complete: {} total flowers reopened", reopenedCount);
        if (reopenedCount > 0 || alreadyOpenCount > 0 || notFoundCount > 0) {
            TemplateMod.LOGGER.info("[EndUpdate] Breakdown: {} from closedPositions set, {} already open, {} not found (block changed)", 
                fromClosedPositionsCount, alreadyOpenCount, notFoundCount);
        }
        if (reopenedCount == 0 && (closedPositions == null || closedPositions.isEmpty()) && (expiryMap == null || expiryMap.isEmpty())) {
            TemplateMod.LOGGER.warn("[EndUpdate] WARNING: No flowers were reopened, but tracking was empty. This may indicate a tracking issue.");
        }
        TemplateMod.LOGGER.info("[EndUpdate] ==============================================");
    }
    
    // Periodic tick handler for safety net - reopens expired flowers as fallback
    // This ensures flowers reopen even if FLASH_ENDED packet is lost
    public static void tick(ServerWorld world) {
        // Only process in The End dimension
        if (world.getRegistryKey() != net.minecraft.world.World.END) return;
        
        Map<BlockPos, Long> expiryMap = worldToExpiry.get(world);
        if (expiryMap == null || expiryMap.isEmpty()) {
            return;
        }
        
        long now = world.getTime();
        Iterator<Map.Entry<BlockPos, Long>> it = expiryMap.entrySet().iterator();
        int expiredCount = 0;
        
        while (it.hasNext()) {
            Map.Entry<BlockPos, Long> entry = it.next();
            BlockPos pos = entry.getKey();
            long expiryTime = entry.getValue();
            
            // Only reopen if time has expired (fallback safety)
            if (now >= expiryTime) {
                expiredCount++;
                BlockState blockState = world.getBlockState(pos);
                if (blockState.isOf(ModBlocks.CLOSED_ENDER_CHRYSANTHEMUM)) {
                    // Replace back with regular version, preserving facing direction
                    BlockState regularState = ModBlocks.ENDER_CHRYSANTHEMUM.getDefaultState()
                        .with(EnderChrysanthemumBlock.ATTACHMENT_FACE, blockState.get(ClosedEnderChrysanthemumBlock.ATTACHMENT_FACE));
                    world.setBlockState(pos, regularState, Block.NOTIFY_ALL);
                    removeClosedPosition(world, pos);
                } else if (blockState.isOf(ModBlocks.POTTED_CLOSED_ENDER_CHRYSANTHEMUM)) {
                    // Replace potted closed with potted open
                    world.setBlockState(pos, ModBlocks.POTTED_ENDER_CHRYSANTHEMUM.getDefaultState(), Block.NOTIFY_ALL);
                    removeClosedPosition(world, pos);
                } else if (!blockState.isOf(ModBlocks.ENDER_CHRYSANTHEMUM) && !blockState.isOf(ModBlocks.POTTED_ENDER_CHRYSANTHEMUM)) {
                    // Block was changed to something else - remove from tracking
                    removeClosedPosition(world, pos);
                } else {
                    // Already open - remove from tracking
                    removeClosedPosition(world, pos);
                }
                it.remove();
            }
            // Note: We removed the "re-close if prematurely opened" logic because:
            // 1. FLASH_ENDED should handle all reopenings via forceReopenAll()
            // 2. If a flower expires naturally, it should reopen (safety net)
            // 3. Re-closing would interfere with the normal reopen flow
        }
        
        if (expiredCount > 0) {
            TemplateMod.LOGGER.info("[EndUpdate] Safety net: Reopened {} expired flowers (current time: {})", expiredCount, now);
        }
    }
}


