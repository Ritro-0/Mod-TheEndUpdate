package com.theendupdate.entity;

import net.minecraft.server.world.ServerWorld;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for managing all active King Phantom boss bar managers.
 * EXACT COPY of ShadowCreakingBossBarRegistry structure, simplified for single-entity boss.
 */
public class KingPhantomBossBarRegistry {
    private static final Map<UUID, KingPhantomBossBarManager> activeBossBars = new ConcurrentHashMap<>();
    
    /**
     * Creates a new boss bar manager for a King Phantom entity
     */
    public static KingPhantomBossBarManager createBossBar(KingPhantomEntity entity) {
        UUID entityUuid = entity.getUuid();
        
        // Remove any existing boss bar for this entity (shouldn't happen, but safety check)
        KingPhantomBossBarManager existing = activeBossBars.remove(entityUuid);
        if (existing != null) {
            existing.endBossFight();
        }
        
        // Create new boss bar manager
        KingPhantomBossBarManager manager = new KingPhantomBossBarManager(entityUuid);
        activeBossBars.put(entityUuid, manager);
        
        // Start the boss fight
        manager.startBossFight(entity);
        
        return manager;
    }
    
    /**
     * Gets the boss bar manager for a specific entity UUID
     */
    public static KingPhantomBossBarManager getBossBar(UUID entityUuid) {
        return activeBossBars.get(entityUuid);
    }
    
    /**
     * Removes a boss bar manager and cleans it up
     */
    public static void removeBossBar(UUID entityUuid) {
        KingPhantomBossBarManager manager = activeBossBars.remove(entityUuid);
        if (manager != null) {
            manager.endBossFight();
        }
    }
    
    // Track which server tick we last updated on to avoid duplicate ticks per server tick
    private static long lastTickTime = -1;
    
    /**
     * Ticks all active boss bars. Call this once per server tick.
     * If called multiple times in the same server tick (e.g., once per world),
     * only the first call will actually tick the managers.
     */
    public static void tickAll(ServerWorld world) {
        // Safety check: ensure world is valid
        if (world == null) return;
        
        // Get current server tick time to avoid duplicate ticks
        long currentTickTime = world.getTime();
        
        // Skip if we already ticked this server tick (prevents ticking once per dimension)
        if (currentTickTime == lastTickTime) {
            return;
        }
        lastTickTime = currentTickTime;
        
        Iterator<Map.Entry<UUID, KingPhantomBossBarManager>> iterator = activeBossBars.entrySet().iterator();
        
        while (iterator.hasNext()) {
            try {
                Map.Entry<UUID, KingPhantomBossBarManager> entry = iterator.next();
                KingPhantomBossBarManager manager = entry.getValue();
                
                if (manager != null && manager.isActive()) {
                    manager.tick(world);
                } else {
                    // Clean up inactive managers
                    iterator.remove();
                    if (manager != null) {
                        manager.endBossFight();
                    }
                }
            } catch (Exception e) {
                iterator.remove();
            }
        }
    }
    
    /**
     * Gets all active boss bar managers (for debugging or other purposes)
     */
    public static Collection<KingPhantomBossBarManager> getAllActiveBossBars() {
        return new ArrayList<>(activeBossBars.values());
    }
    
    /**
     * Cleans up all boss bars (called when server shuts down)
     */
    public static void clearAll() {
        for (KingPhantomBossBarManager manager : activeBossBars.values()) {
            manager.endBossFight();
        }
        activeBossBars.clear();
    }
}
