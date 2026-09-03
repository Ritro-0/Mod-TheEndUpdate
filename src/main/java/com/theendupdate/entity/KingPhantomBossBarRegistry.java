package com.theendupdate.entity;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerLevel;

public class KingPhantomBossBarRegistry {
    private static final Map<UUID, KingPhantomBossBarManager> activeBossBars = new ConcurrentHashMap<>();
    
    public static KingPhantomBossBarManager createBossBar(KingPhantomEntity entity) {
        UUID entityUuid = entity.getUUID();
        
        KingPhantomBossBarManager existing = activeBossBars.remove(entityUuid);
        if (existing != null) {
            existing.endBossFight();
        }
        
        KingPhantomBossBarManager manager = new KingPhantomBossBarManager(entityUuid);
        activeBossBars.put(entityUuid, manager);
        
        manager.startBossFight(entity);
        
        return manager;
    }
    
    public static KingPhantomBossBarManager getBossBar(UUID entityUuid) {
        return activeBossBars.get(entityUuid);
    }
    
    public static void removeBossBar(UUID entityUuid) {
        KingPhantomBossBarManager manager = activeBossBars.remove(entityUuid);
        if (manager != null) {
            manager.endBossFight();
        }
    }
    
    private static long lastTickTime = -1; // dedupes ticks when tickAll is called once per dimension in the same server tick
    
    public static void tickAll(ServerLevel world) {
        if (world == null) return;
        
        long currentTickTime = world.getGameTime();
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
    
    public static Collection<KingPhantomBossBarManager> getAllActiveBossBars() {
        return new ArrayList<>(activeBossBars.values());
    }
    
    public static void clearAll() {
        for (KingPhantomBossBarManager manager : activeBossBars.values()) {
            manager.endBossFight();
        }
        activeBossBars.clear();
    }
}
