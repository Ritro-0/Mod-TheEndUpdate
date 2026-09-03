package com.theendupdate.entity;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerLevel;

/** Tracks all active Shadow Creaking boss bar managers so multiple fights can run at once. */
public class ShadowCreakingBossBarRegistry {
    private static final Map<UUID, ShadowCreakingBossBarManager> activeBossBars = new ConcurrentHashMap<>();
    
    public static ShadowCreakingBossBarManager createBossBar(ShadowCreakingEntity mainEntity, boolean isEmergingFromAltar) {
        UUID mainEntityUuid = mainEntity.getUUID();
        
        ShadowCreakingBossBarManager existing = activeBossBars.remove(mainEntityUuid);
        if (existing != null) {
            existing.endBossFight();
        }
        
        ShadowCreakingBossBarManager manager = new ShadowCreakingBossBarManager(mainEntityUuid);
        activeBossBars.put(mainEntityUuid, manager);
        
        manager.startBossFight(mainEntity, isEmergingFromAltar);
        
        return manager;
    }
    
    /** Called when the altar is lit, before the entity actually exists. */
    public static ShadowCreakingBossBarManager createChargingBossBar(UUID entityUuid, ServerLevel world, net.minecraft.core.BlockPos altarPos) {
        ShadowCreakingBossBarManager existing = activeBossBars.remove(entityUuid);
        if (existing != null) {
            existing.endBossFight();
        }
        
        ShadowCreakingBossBarManager manager = new ShadowCreakingBossBarManager(entityUuid);
        activeBossBars.put(entityUuid, manager);
        
        manager.startChargingFromAltar(world, altarPos);
        
        return manager;
    }
    
    public static ShadowCreakingBossBarManager getBossBar(UUID mainEntityUuid) {
        return activeBossBars.get(mainEntityUuid);
    }
    
    /** Used when the entity actually spawns after the charging phase (which tracked it under a placeholder UUID). */
    public static void transferBossBar(UUID oldUuid, UUID newUuid) {
        ShadowCreakingBossBarManager manager = activeBossBars.remove(oldUuid);
        if (manager != null) {
            activeBossBars.put(newUuid, manager);
        }
    }
    
    public static void removeBossBar(UUID mainEntityUuid) {
        ShadowCreakingBossBarManager manager = activeBossBars.remove(mainEntityUuid);
        if (manager != null) {
            manager.endBossFight();
        }
    }
    
    // dimensions each call tickAll once per world tick, so dedupe on game time to avoid double-ticking
    private static long lastTickTime = -1;
    
    public static void tickAll(ServerLevel world) {
        if (world == null) return;
        
        long currentTickTime = world.getGameTime();
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
                    iterator.remove();
                    if (manager != null) {
                        manager.endBossFight();
                    }
                }
            } catch (Exception e) {
                com.theendupdate.TheEndUpdate.LOGGER.error("Error ticking Shadow Creaking boss bar", e);
                iterator.remove();
            }
        }
    }
    
    public static Collection<ShadowCreakingBossBarManager> getAllActiveBossBars() {
        return new ArrayList<>(activeBossBars.values());
    }
    
    public static void clearAll() {
        for (ShadowCreakingBossBarManager manager : activeBossBars.values()) {
            manager.endBossFight();
        }
        activeBossBars.clear();
    }
}
