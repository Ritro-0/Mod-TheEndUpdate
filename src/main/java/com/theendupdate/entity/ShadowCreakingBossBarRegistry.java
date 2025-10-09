package com.theendupdate.entity;

import net.minecraft.server.world.ServerWorld;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for managing all active Shadow Creaking boss bar managers.
 * This ensures we can track multiple boss fights simultaneously.
 */
public class ShadowCreakingBossBarRegistry {
    private static final Map<UUID, ShadowCreakingBossBarManager> activeBossBars = new ConcurrentHashMap<>();
    
    /**
     * Creates a new boss bar manager for a main Shadow Creaking entity
     */
    public static ShadowCreakingBossBarManager createBossBar(ShadowCreakingEntity mainEntity, boolean isEmergingFromAltar) {
        UUID mainEntityUuid = mainEntity.getUuid();
        
        // Remove any existing boss bar for this entity (shouldn't happen, but safety check)
        ShadowCreakingBossBarManager existing = activeBossBars.remove(mainEntityUuid);
        if (existing != null) {
            existing.endBossFight();
        }
        
        // Create new boss bar manager
        ShadowCreakingBossBarManager manager = new ShadowCreakingBossBarManager(mainEntityUuid);
        activeBossBars.put(mainEntityUuid, manager);
        
        // Start the boss fight
        manager.startBossFight(mainEntity, isEmergingFromAltar);
        
        return manager;
    }
    
    /**
     * Creates a new boss bar manager that starts charging when the altar is lit.
     * This is called before the entity spawns.
     */
    public static ShadowCreakingBossBarManager createChargingBossBar(UUID entityUuid, ServerWorld world, net.minecraft.util.math.BlockPos altarPos) {
        // Remove any existing boss bar for this entity (shouldn't happen, but safety check)
        ShadowCreakingBossBarManager existing = activeBossBars.remove(entityUuid);
        if (existing != null) {
            existing.endBossFight();
        }
        
        // Create new boss bar manager
        ShadowCreakingBossBarManager manager = new ShadowCreakingBossBarManager(entityUuid);
        activeBossBars.put(entityUuid, manager);
        
        // Start the charging phase
        manager.startChargingFromAltar(world, altarPos);
        
        return manager;
    }
    
    /**
     * Gets the boss bar manager for a specific main entity UUID
     */
    public static ShadowCreakingBossBarManager getBossBar(UUID mainEntityUuid) {
        return activeBossBars.get(mainEntityUuid);
    }
    
    /**
     * Transfers a boss bar from one UUID to another (used when entity spawns after charging)
     */
    public static void transferBossBar(UUID oldUuid, UUID newUuid) {
        ShadowCreakingBossBarManager manager = activeBossBars.remove(oldUuid);
        if (manager != null) {
            activeBossBars.put(newUuid, manager);
            com.theendupdate.TemplateMod.LOGGER.info("Transferred boss bar from UUID {} to {}", oldUuid, newUuid);
        }
    }
    
    /**
     * Removes a boss bar manager and cleans it up
     */
    public static void removeBossBar(UUID mainEntityUuid) {
        ShadowCreakingBossBarManager manager = activeBossBars.remove(mainEntityUuid);
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
        
        Iterator<Map.Entry<UUID, ShadowCreakingBossBarManager>> iterator = activeBossBars.entrySet().iterator();
        
        while (iterator.hasNext()) {
            try {
                Map.Entry<UUID, ShadowCreakingBossBarManager> entry = iterator.next();
                ShadowCreakingBossBarManager manager = entry.getValue();
                
                if (manager != null && manager.isActive()) {
                    manager.tick(world);
                } else {
                    // Clean up inactive managers
                    com.theendupdate.TemplateMod.LOGGER.info("Cleaning up inactive boss bar manager");
                    iterator.remove();
                    if (manager != null) {
                        manager.endBossFight();
                    }
                }
            } catch (Exception e) {
                // Log error and remove the problematic manager
                com.theendupdate.TemplateMod.LOGGER.error("Error ticking Shadow Creaking boss bar", e);
                iterator.remove();
            }
        }
    }
    
    /**
     * Gets all active boss bar managers (for debugging or other purposes)
     */
    public static Collection<ShadowCreakingBossBarManager> getAllActiveBossBars() {
        return new ArrayList<>(activeBossBars.values());
    }
    
    /**
     * Cleans up all boss bars (called when server shuts down)
     */
    public static void clearAll() {
        for (ShadowCreakingBossBarManager manager : activeBossBars.values()) {
            manager.endBossFight();
        }
        activeBossBars.clear();
    }
}
