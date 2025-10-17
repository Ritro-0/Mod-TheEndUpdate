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
 * EXACT COPY of ShadowCreakingBossBarManager structure, simplified for single-entity boss.
 */
public class KingPhantomBossBarManager {
    private static final Text BOSS_BAR_NAME = Text.translatable("entity.theendupdate.king_phantom");
    private static final int VIEW_DISTANCE = 64; // Distance within which players see the boss bar
    
    private final ServerBossBar bossBar;
    private final UUID entityUuid;
    private boolean isActive;
    
    public KingPhantomBossBarManager(UUID entityUuid) {
        this.entityUuid = entityUuid;
        this.bossBar = new ServerBossBar(BOSS_BAR_NAME, BossBar.Color.RED, BossBar.Style.PROGRESS);
        this.isActive = false;
        
        // Configure boss bar visibility settings
        this.bossBar.setVisible(true);
        this.bossBar.setDarkenSky(false);
        this.bossBar.setDragonMusic(false);
        this.bossBar.setThickenFog(false);
    }
    
    /**
     * Starts the boss fight
     */
    public void startBossFight(KingPhantomEntity entity) {
        this.isActive = true;
        
        // Start at full health
        this.bossBar.setPercent(1.0f);
        this.bossBar.setStyle(BossBar.Style.PROGRESS);
        
        // Add nearby players to the boss bar
        if (entity.getEntityWorld() instanceof ServerWorld serverWorld) {
            this.updateNearbyPlayers(serverWorld);
        }
    }
    
    /**
     * Updates the boss bar each tick
     */
    public void tick(ServerWorld world) {
        if (!this.isActive || world == null) {
            return;
        }
        
        // Check if all players have logged out - end boss fight
        try {
            net.minecraft.server.MinecraftServer server = world.getServer();
            if (server != null && server.getPlayerManager().getPlayerList().isEmpty()) {
                this.endBossFight();
                return;
            }
        } catch (Exception e) {
            // Silent fail
        }
        
        // Find the entity across all dimensions
        KingPhantomEntity entity = this.findEntityByUuid(world, this.entityUuid);
        
        // Check if entity is gone (dead, removed, or despawned due to peaceful mode)
        if (entity == null || entity.isDead() || entity.isRemoved()) {
            this.endBossFight();
            return;
        }
        
        // Update health based on current entity
        this.updateBossBarHealth(entity);
        
        // Update nearby players regularly (every 20 ticks = 1 second)
        if (world.getTime() % 20 == 0) {
            this.updateNearbyPlayers(world);
        }
    }
    
    /**
     * Updates the boss bar health based on entity's current health
     */
    private void updateBossBarHealth(KingPhantomEntity entity) {
        if (entity == null || entity.isDead()) {
            this.bossBar.setPercent(0.0f);
            return;
        }
        
        float currentHealth = entity.getHealth();
        float maxHealth = entity.getMaxHealth();
        float healthPercent = currentHealth / maxHealth;
        this.bossBar.setPercent(Math.max(0.0f, Math.min(1.0f, healthPercent)));
    }
    
    /**
     * Finds a KingPhantomEntity by UUID across all dimensions
     */
    private KingPhantomEntity findEntityByUuid(ServerWorld world, UUID uuid) {
        // First try the passed world for performance
        net.minecraft.entity.Entity entity = world.getEntity(uuid);
        if (entity instanceof KingPhantomEntity kingPhantom) {
            return kingPhantom;
        }
        
        // If not found in this world, search all dimensions
        net.minecraft.server.MinecraftServer server = world.getServer();
        if (server != null) {
            for (ServerWorld serverWorld : server.getWorlds()) {
                if (serverWorld != world) { // Already checked this one
                    entity = serverWorld.getEntity(uuid);
                    if (entity instanceof KingPhantomEntity kingPhantom) {
                        return kingPhantom;
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * Updates which players can see the boss bar based on proximity
     */
    private void updateNearbyPlayers(ServerWorld world) {
        if (!this.isActive) return;
        
        // Get all players within view distance of the entity
        Set<ServerPlayerEntity> nearbyPlayers = new HashSet<>();
        
        // Find the entity across all dimensions
        KingPhantomEntity entity = this.findEntityByUuid(world, this.entityUuid);
        
        if (entity != null && entity.getEntityWorld() instanceof ServerWorld entityWorld) {
            Box searchBox = Box.of(new Vec3d(entity.getX(), entity.getY(), entity.getZ()), VIEW_DISTANCE * 2, VIEW_DISTANCE * 2, VIEW_DISTANCE * 2);
            // Search for players in the entity's dimension, not the passed world
            List<PlayerEntity> playersInRange = entityWorld.getEntitiesByClass(PlayerEntity.class, searchBox, 
                p -> new Vec3d(p.getX(), p.getY(), p.getZ()).distanceTo(new Vec3d(entity.getX(), entity.getY(), entity.getZ())) <= VIEW_DISTANCE);
            
            for (PlayerEntity player : playersInRange) {
                if (player instanceof ServerPlayerEntity serverPlayer) {
                    nearbyPlayers.add(serverPlayer);
                }
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
    
    /**
     * Gets all players currently watching this boss bar
     */
    public java.util.Set<net.minecraft.server.network.ServerPlayerEntity> getPlayers() {
        return new java.util.HashSet<>(this.bossBar.getPlayers());
    }
    
    /**
     * Adds a player to this boss bar
     */
    public void addPlayer(net.minecraft.server.network.ServerPlayerEntity player) {
        this.bossBar.addPlayer(player);
    }
    
    /**
     * Removes a player from this boss bar
     */
    public void removePlayer(net.minecraft.server.network.ServerPlayerEntity player) {
        this.bossBar.removePlayer(player);
    }
    
    /**
     * Gets the UUID of the entity
     */
    public UUID getEntityUuid() {
        return this.entityUuid;
    }
}
