package com.theendupdate.entity;

import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.entity.player.PlayerEntity;
import java.util.*;

/**
 * Manages the unified boss bar for the Shadow Creaking boss fight.
 * Tracks all phases: main entity, two mini entities, and four tiny entities.
 * The health bar only reaches zero when ALL entities are dead.
 */
public class ShadowCreakingBossBarManager {
    private static final Text BOSS_BAR_NAME = Text.translatable("entity.theendupdate.shadow_creaking");
    private static final int VIEW_DISTANCE = 64; // Distance within which players see the boss bar
    
    private final ServerBossBar bossBar;
    private final UUID mainEntityUuid;
    private final Map<UUID, EntityPhase> trackedEntities;
    private boolean isActive;
    private boolean isEmerging;
    private int emergingTicks;
    public boolean isCharging; // New: true when charging before entity spawns
    public int chargingTicks; // New: tracks charging progress before entity spawns
    private static final int EMERGE_DURATION_TICKS = 134; // Match ShadowCreakingEntity.EMERGE_DURATION_TICKS
    private static final int LEVITATE_DURATION_TICKS = 140; // Match ShadowCreakingEntity.LEVITATE_DURATION_TICKS
    private static final int TOTAL_INTRO_DURATION = EMERGE_DURATION_TICKS + LEVITATE_DURATION_TICKS; // 274 ticks total
    private static final int ALTAR_ACTIVE_DURATION_TICKS = 15 * 20; // 15 seconds from ShadowAltarBlockEntity
    private static final int TOTAL_CHARGING_DURATION = ALTAR_ACTIVE_DURATION_TICKS + TOTAL_INTRO_DURATION; // Altar + emergence + levitation
    
    // Total health pool across ALL phases (main + 2 minis + 4 tinies)
    private static final float TOTAL_MAX_HEALTH = 250.0f + (83.3333333333f * 2) + (8.0f * 4); // 448.66 HP
    
    // Track total damage dealt to the entire boss (across all phases)
    private float totalDamageDealt = 0.0f;
    
    // Track previous health values to calculate damage dealt
    private final Map<UUID, Float> previousHealthValues = new HashMap<>();
    
    public enum EntityPhase {
        MAIN(1.0f),      // Main shadow creaking - 100% of total health
        MINI(0.333f),    // Each mini - 33.3% of total health (2 total = 66.6%)
        TINY(0.166f);    // Each tiny - 16.6% of total health (4 total = 66.6%)
        
        private final float healthWeight;
        
        EntityPhase(float healthWeight) {
            this.healthWeight = healthWeight;
        }
        
        public float getHealthWeight() {
            return healthWeight;
        }
    }
    
    public ShadowCreakingBossBarManager(UUID mainEntityUuid) {
        this.mainEntityUuid = mainEntityUuid;
        this.trackedEntities = new HashMap<>();
        this.bossBar = new ServerBossBar(BOSS_BAR_NAME, BossBar.Color.PURPLE, BossBar.Style.PROGRESS);
        this.isActive = false;
        this.isEmerging = false;
        this.emergingTicks = 0;
        this.isCharging = false;
        this.chargingTicks = 0;
        
        // Configure boss bar visibility settings
        this.bossBar.setVisible(true);
        this.bossBar.setDarkenSky(false);
        this.bossBar.setDragonMusic(false);
        this.bossBar.setThickenFog(false);
        
        com.theendupdate.TemplateMod.LOGGER.info("Created Shadow Creaking boss bar for entity {}", mainEntityUuid);
    }
    
    /**
     * Starts the charging phase when the shadow altar is initially lit.
     * This begins the boss bar charging animation before the entity even spawns.
     */
    public void startChargingFromAltar(ServerWorld world) {
        if (world == null) return;
        
        this.isActive = true;
        this.isCharging = true;
        this.chargingTicks = 0;
        this.isEmerging = false;
        this.emergingTicks = 0;
        
        // Start charging from 0
        this.bossBar.setPercent(0.0f);
        this.bossBar.setStyle(BossBar.Style.NOTCHED_20); // More granular during charging
        
        // Add nearby players to the boss bar
        this.updateNearbyPlayers(world);
        
        com.theendupdate.TemplateMod.LOGGER.info("Shadow Creaking boss bar started charging from altar - will reach 100% in {} ticks, {} players watching", 
            TOTAL_CHARGING_DURATION, this.bossBar.getPlayers().size());
    }
    
    /**
     * Starts the boss fight. If the entity is emerging from altar, continues charging if already started.
     * Otherwise, starts at full health.
     */
    public void startBossFight(ShadowCreakingEntity mainEntity, boolean isEmergingFromAltar) {
        this.isActive = true;
        
        // Track the main entity
        this.trackedEntities.put(mainEntity.getUuid(), EntityPhase.MAIN);
        
        if (isEmergingFromAltar) {
            // If we were already charging from altar, continue the charging animation
            if (this.isCharging) {
                // Continue charging with the entity now tracked
                // Keep existing charging progress and state
                this.isEmerging = true;
                this.emergingTicks = 0;
                this.bossBar.setStyle(BossBar.Style.NOTCHED_20); // Keep granular during emerging
                com.theendupdate.TemplateMod.LOGGER.info("Shadow Creaking boss bar continued charging (emerging mode) - entity spawned at {}%", 
                    (int)(this.bossBar.getPercent() * 100));
            } else {
                // Start charging from 0 during emerging animation (fallback)
                this.isEmerging = true;
                this.emergingTicks = 0;
                this.bossBar.setPercent(0.0f);
                this.bossBar.setStyle(BossBar.Style.NOTCHED_20); // More granular during emerging
                com.theendupdate.TemplateMod.LOGGER.info("Shadow Creaking boss bar started (emerging mode) - will charge to 100%");
            }
        } else {
            // Spawned directly, start at 100% (no damage dealt yet)
            this.isEmerging = false;
            this.emergingTicks = 0;
            this.isCharging = false;
            this.chargingTicks = 0;
            this.bossBar.setPercent(1.0f);
            this.bossBar.setStyle(BossBar.Style.PROGRESS);
            com.theendupdate.TemplateMod.LOGGER.info("Shadow Creaking boss bar started (direct spawn mode) at 100%");
        }
        
        // Add nearby players to the boss bar
        if (mainEntity.getWorld() instanceof ServerWorld serverWorld) {
            this.updateNearbyPlayers(serverWorld);
            com.theendupdate.TemplateMod.LOGGER.info("Boss bar has {} players", this.bossBar.getPlayers().size());
        }
    }
    
    /**
     * Updates the boss bar each tick
     */
    public void tick(ServerWorld world) {
        if (!this.isActive || world == null) {
            com.theendupdate.TemplateMod.LOGGER.info("Boss bar tick skipped - active: {}, world: {}", this.isActive, world != null);
            return;
        }
        
        // Handle charging phase (before entity spawns)
        if (this.isCharging && !this.isEmerging) {
            this.chargingTicks++;
            // Charge to 100% over the entire duration (altar + emergence + levitation)
            float progress = Math.min(1.0f, (float) this.chargingTicks / TOTAL_CHARGING_DURATION);
            this.bossBar.setPercent(progress);
            
            // Log progress every 20 ticks for debugging
            if (this.chargingTicks % 20 == 0) {
                com.theendupdate.TemplateMod.LOGGER.info("Boss bar charging from altar: {}/{} ticks ({}%), {} players watching", 
                    this.chargingTicks, TOTAL_CHARGING_DURATION, (int)(progress * 100), this.bossBar.getPlayers().size());
            }
            
            // Continue charging until entity spawns and takes over
            this.updateNearbyPlayers(world);
            return;
        }
        
        // Handle emerging phase charging (includes both emerge + levitation intro)
        if (this.isEmerging) {
            this.emergingTicks++;
            
            // Check if any tracked entities have taken damage
            boolean hasDamage = false;
            for (UUID entityUuid : this.trackedEntities.keySet()) {
                ShadowCreakingEntity entity = this.findEntityByUuid(world, entityUuid);
                if (entity != null) {
                    float currentHealth = entity.getHealth();
                    float previousHealth = this.previousHealthValues.getOrDefault(entityUuid, entity.getMaxHealth());
                    if (currentHealth < previousHealth) {
                        hasDamage = true;
                        break;
                    }
                }
            }
            
            // If entity has taken damage, switch to health tracking immediately
            if (hasDamage) {
                com.theendupdate.TemplateMod.LOGGER.info("Entity taking damage during emerging, switching to health tracking");
                this.isEmerging = false;
                this.isCharging = false;
                this.bossBar.setStyle(BossBar.Style.PROGRESS);
                this.updateBossBarHealth(world);
            } else {
                // Continue charging animation: altar progress + emerging progress = total progress
                float totalTicks = this.chargingTicks + this.emergingTicks;
                float progress = Math.min(1.0f, (float) totalTicks / TOTAL_CHARGING_DURATION);
                this.bossBar.setPercent(progress);
            }
            
            // Log progress every 20 ticks for debugging
            if (this.emergingTicks % 20 == 0) {
                float currentPercent = this.bossBar.getPercent();
                com.theendupdate.TemplateMod.LOGGER.info("Boss bar charging (emerging): {}/{} ticks ({}%)", 
                    this.emergingTicks, TOTAL_INTRO_DURATION, (int)(currentPercent * 100));
            }
            
            // Switch to normal progress bar when entire intro is complete
            if (this.emergingTicks >= TOTAL_INTRO_DURATION) {
                this.isEmerging = false;
                this.isCharging = false; // End charging phase
                this.bossBar.setStyle(BossBar.Style.PROGRESS);
                this.updateBossBarHealth(world);
                // Update nearby players immediately after transition (before switching to proximity mode)
                this.updateNearbyPlayers(world);
                com.theendupdate.TemplateMod.LOGGER.info("Shadow Creaking intro complete (274 ticks), switching to health tracking - {} players watching", this.bossBar.getPlayers().size());
                // Fall through to normal health tracking mode instead of returning
            } else {
                // Still in intro, update players and return
                this.updateNearbyPlayers(world);
                return;
            }
        }
        
        // Update health based on current entities
        this.updateBossBarHealth(world);
        
        // Update nearby players regularly (every 20 ticks = 1 second)
        if (world.getTime() % 20 == 0) {
            this.updateNearbyPlayers(world);
            
            // Debug logging
            float percent = this.bossBar.getPercent();
            com.theendupdate.TemplateMod.LOGGER.info("Boss bar at {}%, {} damage dealt of {} total HP, {} players watching, {} entities tracked", 
                (int)(percent * 100), (int)totalDamageDealt, (int)TOTAL_MAX_HEALTH, this.bossBar.getPlayers().size(), this.trackedEntities.size());
        }
        
        // Check if all entities are dead (but only after intro is complete and not charging)
        if (this.trackedEntities.isEmpty() && !this.isEmerging && !this.isCharging) {
            com.theendupdate.TemplateMod.LOGGER.info("All Shadow Creaking entities defeated, ending boss fight");
            this.endBossFight();
        }
    }
    
    /**
     * Adds a mini entity to tracking when the main entity dies
     */
    public void addMiniEntity(ShadowCreakingEntity miniEntity) {
        this.trackedEntities.put(miniEntity.getUuid(), EntityPhase.MINI);
        // Initialize previous health value so damage tracking works immediately
        this.previousHealthValues.put(miniEntity.getUuid(), miniEntity.getMaxHealth());
        com.theendupdate.TemplateMod.LOGGER.info("Added mini entity to boss bar tracking (max HP: {})", miniEntity.getMaxHealth());
    }
    
    /**
     * Adds a tiny entity to tracking when a mini entity dies
     */
    public void addTinyEntity(ShadowCreakingEntity tinyEntity) {
        this.trackedEntities.put(tinyEntity.getUuid(), EntityPhase.TINY);
        // Initialize previous health value so damage tracking works immediately
        this.previousHealthValues.put(tinyEntity.getUuid(), tinyEntity.getMaxHealth());
        com.theendupdate.TemplateMod.LOGGER.info("Added tiny entity to boss bar tracking (max HP: {})", tinyEntity.getMaxHealth());
    }
    
    /**
     * Removes an entity from tracking when it dies
     */
    public void removeEntity(UUID entityUuid) {
        this.trackedEntities.remove(entityUuid);
        // Clean up previous health tracking
        this.previousHealthValues.remove(entityUuid);
    }
    
    /**
     * Updates the boss bar health based on total damage dealt across ALL phases.
     * The boss bar represents a pool of 448.66 HP across all entities:
     * - Main: 250 HP
     * - 2 Minis: 166.66 HP total
     * - 4 Tinies: 32 HP total
     * 
     * As entities take damage or die, we accumulate the total damage dealt.
     * Boss bar = (TOTAL_MAX_HEALTH - totalDamageDealt) / TOTAL_MAX_HEALTH
     * This ensures it starts at 100% and only reaches 0% when all phases are defeated.
     */
    private void updateBossBarHealth(ServerWorld world) {
        // Track damage dealt this tick by checking health changes
        for (Map.Entry<UUID, EntityPhase> entry : this.trackedEntities.entrySet()) {
            UUID entityUuid = entry.getKey();
            
            // Find the entity in the world
            ShadowCreakingEntity entity = this.findEntityByUuid(world, entityUuid);
            if (entity != null && !entity.isDead()) {
                float currentHealth = entity.getHealth();
                
                // Get previous health (or max health if first time seeing this entity)
                Float previousHealth = previousHealthValues.get(entityUuid);
                if (previousHealth == null) {
                    previousHealth = entity.getMaxHealth();
                    previousHealthValues.put(entityUuid, previousHealth);
                }
                
                // Calculate damage dealt since last tick
                float damageTaken = previousHealth - currentHealth;
                if (damageTaken > 0) {
                    totalDamageDealt += damageTaken;
                    previousHealthValues.put(entityUuid, currentHealth);
                    
                    // Debug logging for tiny entities
                    EntityPhase phase = entry.getValue();
                    if (phase == EntityPhase.TINY) {
                        com.theendupdate.TemplateMod.LOGGER.info("Tiny entity took {} damage, total damage: {}/{}", 
                            damageTaken, (int)totalDamageDealt, (int)TOTAL_MAX_HEALTH);
                    }
                }
            }
        }
        
        // Calculate remaining health as percentage of total boss health pool
        float remainingHealth = TOTAL_MAX_HEALTH - totalDamageDealt;
        float bossBarPercent = Math.max(0.0f, remainingHealth / TOTAL_MAX_HEALTH);
        this.bossBar.setPercent(Math.min(1.0f, bossBarPercent));
    }
    
    /**
     * Finds a ShadowCreakingEntity by UUID across all dimensions
     */
    private ShadowCreakingEntity findEntityByUuid(ServerWorld world, UUID uuid) {
        // First try the passed world for performance
        net.minecraft.entity.Entity entity = world.getEntity(uuid);
        if (entity instanceof ShadowCreakingEntity shadowCreaking) {
            return shadowCreaking;
        }
        
        // If not found in this world, search all dimensions
        net.minecraft.server.MinecraftServer server = world.getServer();
        if (server != null) {
            for (ServerWorld serverWorld : server.getWorlds()) {
                if (serverWorld != world) { // Already checked this one
                    entity = serverWorld.getEntity(uuid);
                    if (entity instanceof ShadowCreakingEntity shadowCreaking) {
                        return shadowCreaking;
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
        
        // Get all players within view distance of any tracked entity
        Set<ServerPlayerEntity> nearbyPlayers = new HashSet<>();
        
        // During charging/emerging phase, add all players from ALL dimensions (boss bar is global during intro)
        if (this.isCharging || this.isEmerging) {
            try {
                // Get the server from the world and iterate through all players in all dimensions
                net.minecraft.server.MinecraftServer server = world.getServer();
                if (server != null) {
                    for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                        nearbyPlayers.add(player);
                    }
                }
                // Only log occasionally to avoid spam
                int totalTicks = this.chargingTicks + this.emergingTicks;
                if (totalTicks % 20 == 0) {
                    com.theendupdate.TemplateMod.LOGGER.info("Charging boss bar: {} players across all dimensions (charging: {}, emerging: {})", 
                        nearbyPlayers.size(), this.isCharging, this.isEmerging);
                }
            } catch (Exception e) {
                // Fallback: if there's any issue getting players, skip this tick
                com.theendupdate.TemplateMod.LOGGER.error("Error getting players during charging/emerging phase", e);
                return;
            }
        } else {
            // Normal entity-based proximity tracking
            for (UUID entityUuid : this.trackedEntities.keySet()) {
                ShadowCreakingEntity entity = this.findEntityByUuid(world, entityUuid);
                if (entity != null && entity.getWorld() instanceof ServerWorld entityWorld) {
                    Box searchBox = Box.of(entity.getPos(), VIEW_DISTANCE * 2, VIEW_DISTANCE * 2, VIEW_DISTANCE * 2);
                    // Search for players in the entity's dimension, not the passed world
                    List<PlayerEntity> playersInRange = entityWorld.getEntitiesByClass(PlayerEntity.class, searchBox, 
                        p -> p.getPos().distanceTo(entity.getPos()) <= VIEW_DISTANCE);
                    
                    for (PlayerEntity player : playersInRange) {
                        if (player instanceof ServerPlayerEntity serverPlayer) {
                            nearbyPlayers.add(serverPlayer);
                        }
                    }
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
        com.theendupdate.TemplateMod.LOGGER.info("Ending boss fight - was charging: {}, was emerging: {}, {} players watching", 
            this.isCharging, this.isEmerging, this.bossBar.getPlayers().size());
        this.isActive = false;
        this.bossBar.clearPlayers();
        this.trackedEntities.clear();
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
     * Gets the UUID of the main entity
     */
    public UUID getMainEntityUuid() {
        return this.mainEntityUuid;
    }
}

