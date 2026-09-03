package com.theendupdate.entity;

import java.util.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class KingPhantomBossBarManager {
    private static final Component BOSS_BAR_NAME = Component.translatable("entity.theendupdate.king_phantom");
    private static final int VIEW_DISTANCE = 64;
    
    private final ServerBossEvent bossBar;
    private final UUID entityUuid;
    private boolean isActive;
    
    public KingPhantomBossBarManager(UUID entityUuid) {
        this.entityUuid = entityUuid;
        this.bossBar = new ServerBossEvent(UUID.randomUUID(), BOSS_BAR_NAME, BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);
        this.isActive = false;
        
        this.bossBar.setVisible(true);
        this.bossBar.setDarkenScreen(false);
        this.bossBar.setPlayBossMusic(false);
        this.bossBar.setCreateWorldFog(false);
    }
    
    public void startBossFight(KingPhantomEntity entity) {
        this.isActive = true;
        
        this.bossBar.setProgress(1.0f);
        this.bossBar.setOverlay(BossEvent.BossBarOverlay.PROGRESS);
        
        if (entity.level() instanceof ServerLevel serverWorld) {
            this.updateNearbyPlayers(serverWorld);
        }
    }
    
    public void tick(ServerLevel world) {
        if (!this.isActive || world == null) {
            return;
        }
        
        try {
            net.minecraft.server.MinecraftServer server = world.getServer();
            if (server != null && server.getPlayerList().getPlayers().isEmpty()) {
                this.endBossFight();
                return;
            }
        } catch (Exception e) {
            // ignore, entity check below handles it
        }
        
        KingPhantomEntity entity = this.findEntityByUuid(world, this.entityUuid);
        
        if (entity == null || entity.isDeadOrDying() || entity.isRemoved()) {
            this.endBossFight();
            return;
        }
        
        this.updateBossBarHealth(entity);
        
        if (world.getGameTime() % 20 == 0) {
            this.updateNearbyPlayers(world);
        }
    }
    
    private void updateBossBarHealth(KingPhantomEntity entity) {
        if (entity == null || entity.isDeadOrDying()) {
            this.bossBar.setProgress(0.0f);
            return;
        }
        
        float currentHealth = entity.getHealth();
        float maxHealth = entity.getMaxHealth();
        float healthPercent = currentHealth / maxHealth;
        this.bossBar.setProgress(Math.max(0.0f, Math.min(1.0f, healthPercent)));
    }
    
    private KingPhantomEntity findEntityByUuid(ServerLevel world, UUID uuid) {
        net.minecraft.world.entity.Entity entity = world.getEntity(uuid);
        if (entity instanceof KingPhantomEntity kingPhantom) {
            return kingPhantom;
        }
        
        net.minecraft.server.MinecraftServer server = world.getServer();
        if (server != null) {
            for (ServerLevel serverWorld : server.getAllLevels()) {
                if (serverWorld != world) {
                    entity = serverWorld.getEntity(uuid);
                    if (entity instanceof KingPhantomEntity kingPhantom) {
                        return kingPhantom;
                    }
                }
            }
        }
        
        return null;
    }
    
    private void updateNearbyPlayers(ServerLevel world) {
        if (!this.isActive) return;
        
        Set<ServerPlayer> nearbyPlayers = new HashSet<>();
        
        KingPhantomEntity entity = this.findEntityByUuid(world, this.entityUuid);
        
        if (entity != null && entity.level() instanceof ServerLevel entityWorld) {
            AABB searchBox = AABB.ofSize(new Vec3(entity.getX(), entity.getY(), entity.getZ()), VIEW_DISTANCE * 2, VIEW_DISTANCE * 2, VIEW_DISTANCE * 2);
            List<Player> playersInRange = entityWorld.getEntitiesOfClass(Player.class, searchBox, 
                p -> new Vec3(p.getX(), p.getY(), p.getZ()).distanceTo(new Vec3(entity.getX(), entity.getY(), entity.getZ())) <= VIEW_DISTANCE);
            
            for (Player player : playersInRange) {
                if (player instanceof ServerPlayer serverPlayer) {
                    nearbyPlayers.add(serverPlayer);
                }
            }
        }
        
        Set<ServerPlayer> currentPlayers = new HashSet<>(this.bossBar.getPlayers());
        
        for (ServerPlayer player : nearbyPlayers) {
            if (!currentPlayers.contains(player)) {
                this.bossBar.addPlayer(player);
            }
        }
        
        for (ServerPlayer player : currentPlayers) {
            if (!nearbyPlayers.contains(player)) {
                this.bossBar.removePlayer(player);
            }
        }
    }
    
    public void endBossFight() {
        this.isActive = false;
        this.bossBar.removeAllPlayers();
    }
    
    public boolean isActive() {
        return this.isActive;
    }
    
    public java.util.Set<net.minecraft.server.level.ServerPlayer> getPlayers() {
        return new java.util.HashSet<>(this.bossBar.getPlayers());
    }
    
    public void addPlayer(net.minecraft.server.level.ServerPlayer player) {
        this.bossBar.addPlayer(player);
    }
    
    public void removePlayer(net.minecraft.server.level.ServerPlayer player) {
        this.bossBar.removePlayer(player);
    }
    
    public UUID getEntityUuid() {
        return this.entityUuid;
    }
}
