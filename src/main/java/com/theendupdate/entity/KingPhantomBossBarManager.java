package com.theendupdate.entity;

import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.player.PlayerEntity;
import java.util.*;

/**
 * Manages the boss bar for the King Phantom.
 * Displays a deep blood red boss bar showing the King Phantom's health.
 */
public class KingPhantomBossBarManager {
    private static final Text BOSS_BAR_NAME = Text.translatable("entity.theendupdate.king_phantom");
    private static final int VIEW_DISTANCE = 64; // Distance within which players see the boss bar
    
    private final ServerBossBar bossBar;
    private final UUID entityUuid;
    private boolean isActive;
    
    public KingPhantomBossBarManager(UUID entityUuid) {
        this.entityUuid = entityUuid;
        // RED color for deep blood red appearance
        this.bossBar = new ServerBossBar(BOSS_BAR_NAME, BossBar.Color.RED, BossBar.Style.PROGRESS);
        this.isActive = false;
        
        // Configure boss bar visibility settings
        this.bossBar.setVisible(true);
        this.bossBar.setDarkenSky(false);
        this.bossBar.setDragonMusic(false);
        this.bossBar.setThickenFog(false);
    }
    
    /**
     * Starts displaying the boss bar
     */
    public void startBossFight(KingPhantomEntity entity) {
        this.isActive = true;
        this.bossBar.setPercent(1.0f);
        
        // Add nearby players to the boss bar
        if (entity.getEntityWorld() instanceof ServerWorld serverWorld) {
            this.updateNearbyPlayers(serverWorld, entity);
        }
    }
    
    /**
     * Updates the boss bar each tick
     */
    public void tick(ServerWorld world, KingPhantomEntity entity) {
        if (!this.isActive || world == null || entity == null) {
            return;
        }
        
        // Check if entity is dead or removed
        if (entity.isDead() || entity.isRemoved()) {
            this.endBossFight();
            return;
        }
        
        // Update health bar
        float healthPercent = entity.getHealth() / entity.getMaxHealth();
        this.bossBar.setPercent(Math.max(0.0f, Math.min(1.0f, healthPercent)));
        
        // Update nearby players regularly (every 20 ticks = 1 second)
        if (world.getTime() % 20 == 0) {
            this.updateNearbyPlayers(world, entity);
        }
    }
    
    /**
     * Updates which players can see the boss bar based on proximity
     */
    private void updateNearbyPlayers(ServerWorld world, KingPhantomEntity entity) {
        if (!this.isActive || entity == null) return;
        
        // Get all players within view distance
        Set<ServerPlayerEntity> nearbyPlayers = new HashSet<>();
        
        Box searchBox = Box.of(new Vec3d(entity.getX(), entity.getY(), entity.getZ()), VIEW_DISTANCE * 2, VIEW_DISTANCE * 2, VIEW_DISTANCE * 2);
        List<PlayerEntity> playersInRange = world.getEntitiesByClass(PlayerEntity.class, searchBox, 
            p -> new Vec3d(p.getX(), p.getY(), p.getZ()).distanceTo(new Vec3d(entity.getX(), entity.getY(), entity.getZ())) <= VIEW_DISTANCE);
        
        for (PlayerEntity player : playersInRange) {
            if (player instanceof ServerPlayerEntity serverPlayer) {
                nearbyPlayers.add(serverPlayer);
            }
        }
        
        // Add new players and remove distant players
        Set<ServerPlayerEntity> currentPlayers = new HashSet<>(this.bossBar.getPlayers());
        
        // Add new players
        for (ServerPlayerEntity player : nearbyPlayers) {
            if (!currentPlayers.contains(player)) {
                this.bossBar.addPlayer(player);
            }
        }
        
        // Remove players who are no longer nearby
        for (ServerPlayerEntity player : currentPlayers) {
            if (!nearbyPlayers.contains(player)) {
                this.bossBar.removePlayer(player);
            }
        }
    }
    
    /**
     * Ends the boss fight and cleans up
     */
    public void endBossFight() {
        this.isActive = false;
        this.bossBar.clearPlayers();
    }
    
    /**
     * Checks if this manager is currently active
     */
    public boolean isActive() {
        return this.isActive;
    }
}

