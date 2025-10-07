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
     * Gets the boss bar manager for a specific main entity UUID
     */
    public static ShadowCreakingBossBarManager getBossBar(UUID mainEntityUuid) {
        return activeBossBars.get(mainEntityUuid);
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
    
    /**
     * Ticks all active boss bars
     */
    public static void tickAll(ServerWorld world) {
        Iterator<Map.Entry<UUID, ShadowCreakingBossBarManager>> iterator = activeBossBars.entrySet().iterator();
        
        while (iterator.hasNext()) {
            Map.Entry<UUID, ShadowCreakingBossBarManager> entry = iterator.next();
            ShadowCreakingBossBarManager manager = entry.getValue();
            
            if (manager.isActive()) {
                manager.tick(world);
            } else {
                // Clean up inactive managers
                iterator.remove();
                manager.endBossFight();
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
