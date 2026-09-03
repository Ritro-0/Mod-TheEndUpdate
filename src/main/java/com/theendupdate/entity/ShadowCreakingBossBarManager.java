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

/**
 * Unified boss bar for the Shadow Creaking fight. Tracks the main entity plus its
 * two mini and four tiny splits; the bar only hits zero once all of them are dead.
 */
public class ShadowCreakingBossBarManager {
    private static final Component BOSS_BAR_NAME = Component.translatable("entity.theendupdate.shadow_creaking");
    private static final int VIEW_DISTANCE = 64;
    
    private final ServerBossEvent bossBar;
    private final UUID mainEntityUuid;
    private final Map<UUID, EntityPhase> trackedEntities;
    private boolean isActive;
    public boolean isEmerging; // public so the registry can poke it directly
    private int emergingTicks;
    public boolean isCharging;
    public int chargingTicks;
    private net.minecraft.core.BlockPos altarPos;
    private net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> altarDimension;
    private static final int ALTAR_SUMMON_DURATION_TICKS = com.theendupdate.block.ShadowAltarBlockEntity.ALTAR_SUMMON_TICKS;
    private static final int SPAWN_REVEAL_DURATION_TICKS = ShadowCreakingEntity.SPAWN_REVEAL_TICKS;
    private static final int TOTAL_INTRO_DURATION = ALTAR_SUMMON_DURATION_TICKS + SPAWN_REVEAL_DURATION_TICKS;
    private static final int ALTAR_ACTIVE_DURATION_TICKS = 15 * 20; // mirrors ShadowAltarBlockEntity's summon length
    private static final int TOTAL_CHARGING_DURATION = ALTAR_ACTIVE_DURATION_TICKS + TOTAL_INTRO_DURATION;
    
    // Combined health pool across every phase (main + 2 minis + 4 tinies) that the bar represents
    private static final float TOTAL_MAX_HEALTH = 500.0f + (200.0f * 2) + (40.0f * 4);
    
    private float totalDamageDealt = 0.0f;
    private final Map<UUID, Float> previousHealthValues = new HashMap<>();
    
    public enum EntityPhase {
        MAIN(1.0f),
        MINI(0.333f),
        TINY(0.166f);
        
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
        this.bossBar = new ServerBossEvent(UUID.randomUUID(), BOSS_BAR_NAME, BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);
        this.isActive = false;
        this.isEmerging = false;
        this.emergingTicks = 0;
        this.isCharging = false;
        this.chargingTicks = 0;
        
        this.bossBar.setVisible(true);
        this.bossBar.setDarkenScreen(false);
        this.bossBar.setPlayBossMusic(false);
        this.bossBar.setCreateWorldFog(false);
    }
    
    /** Starts the charging animation when the altar is lit, before the entity actually spawns. */
    public void startChargingFromAltar(ServerLevel world, net.minecraft.core.BlockPos altarPos) {
        if (world == null) return;
        
        this.isActive = true;
        this.isCharging = true;
        this.chargingTicks = 0;
        this.isEmerging = false;
        this.emergingTicks = 0;
        this.altarPos = altarPos; // kept around so tick() can confirm the altar hasn't been broken
        this.altarDimension = world.dimension();
        
        this.bossBar.setProgress(0.0f);
        this.bossBar.setOverlay(BossEvent.BossBarOverlay.NOTCHED_20); // notched looks better during the slow charge crawl
        this.bossBar.setVisible(true);
        
        this.updateNearbyPlayers(world);
    }
    
    /**
     * Starts the boss fight. If already charging from the altar, continues that animation;
     * otherwise starts at full health.
     */
    public void startBossFight(ShadowCreakingEntity mainEntity, boolean isEmergingFromAltar) {
        this.isActive = true;
        
        this.trackedEntities.put(mainEntity.getUUID(), EntityPhase.MAIN);
        this.previousHealthValues.put(mainEntity.getUUID(), mainEntity.getMaxHealth());
        
        if (isEmergingFromAltar) {
            if (this.isCharging) {
                this.altarPos = null; // entity exists now, no need to keep checking the altar
                this.isEmerging = true;
                this.emergingTicks = 0;
                this.bossBar.setOverlay(BossEvent.BossBarOverlay.NOTCHED_20);
            } else {
                // fallback: entity emerged without going through the altar charge first
                this.isEmerging = true;
                this.emergingTicks = 0;
                this.bossBar.setProgress(0.0f);
                this.bossBar.setOverlay(BossEvent.BossBarOverlay.NOTCHED_20);
            }
        } else {
            this.isEmerging = false;
            this.emergingTicks = 0;
            this.isCharging = false;
            this.chargingTicks = 0;
            this.bossBar.setProgress(1.0f);
            this.bossBar.setOverlay(BossEvent.BossBarOverlay.PROGRESS);
        }
        
        if (mainEntity.level() instanceof ServerLevel serverWorld) {
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
                com.theendupdate.TheEndUpdate.LOGGER.info("All players logged out, ending boss fight");
                this.endBossFight();
                return;
            }
        } catch (Exception e) {
            com.theendupdate.TheEndUpdate.LOGGER.error("Error checking player list", e);
        }
        
        if (this.isCharging && !this.isEmerging) {
            if (this.altarPos == null) {
                this.beginHealthTracking();
            } else {
                // don't check the altar the instant charging starts, avoids a race with block placement
                if (this.altarDimension != null && this.chargingTicks > 5) {
                    try {
                        net.minecraft.server.MinecraftServer server = world.getServer();
                        if (server != null) {
                            ServerLevel altarWorld = server.getLevel(this.altarDimension);
                            if (altarWorld != null) {
                                net.minecraft.world.level.block.entity.BlockEntity be = altarWorld.getBlockEntity(this.altarPos);
                                if (!(be instanceof com.theendupdate.block.ShadowAltarBlockEntity)) {
                                    com.theendupdate.TheEndUpdate.LOGGER.info("Shadow Altar was broken during charging, ending boss fight");
                                    this.endBossFight();
                                    return;
                                }
                            }
                        }
                    } catch (Exception e) {
                        com.theendupdate.TheEndUpdate.LOGGER.error("Error checking altar state during charging", e);
                    }
                }

                this.chargingTicks++;
                float progress = Math.min(1.0f, (float) this.chargingTicks / TOTAL_CHARGING_DURATION);
                this.bossBar.setProgress(progress);
                this.bossBar.setVisible(true);
                this.updateNearbyPlayers(world);
                return;
            }
        }
        
        // emerging covers both the visual emerge and the follow-up levitation intro
        if (this.isEmerging) {
            this.emergingTicks++;
            
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
            
            if (hasDamage) {
                // getting hit mid-intro skips straight to real health tracking
                this.isEmerging = false;
                this.isCharging = false;
                this.bossBar.setOverlay(BossEvent.BossBarOverlay.PROGRESS);
                this.updateBossBarHealth(world);
            } else {
                float totalTicks = this.chargingTicks + this.emergingTicks;
                float progress = Math.min(1.0f, (float) totalTicks / TOTAL_CHARGING_DURATION);
                this.bossBar.setProgress(progress);
            }
            
            if (this.emergingTicks >= TOTAL_INTRO_DURATION) {
                this.isEmerging = false;
                this.isCharging = false;
                this.bossBar.setOverlay(BossEvent.BossBarOverlay.PROGRESS);
                this.updateBossBarHealth(world);
                this.updateNearbyPlayers(world);
                // fall through to normal health tracking below instead of returning
            } else {
                this.updateNearbyPlayers(world);
                return;
            }
        }
        
        this.updateBossBarHealth(world);
        
        if (!this.isEmerging && !this.isCharging) {
            boolean hasAnyEntity = false;
            for (UUID entityUuid : this.trackedEntities.keySet()) {
                ShadowCreakingEntity entity = this.findEntityByUuid(world, entityUuid);
                if (entity != null && !entity.isDeadOrDying() && !entity.isRemoved()) {
                    hasAnyEntity = true;
                    break;
                }
            }
            
            if (!hasAnyEntity && !this.trackedEntities.isEmpty()) {
                com.theendupdate.TheEndUpdate.LOGGER.info("All Shadow Creaking entities gone (removed/killed/peaceful), ending boss fight");
                this.endBossFight();
                return;
            }
            
            if (this.trackedEntities.isEmpty()) {
                com.theendupdate.TheEndUpdate.LOGGER.info("All Shadow Creaking entities defeated, ending boss fight");
                this.endBossFight();
                return;
            }
        }
        
        if (world.getGameTime() % 20 == 0) {
            this.updateNearbyPlayers(world);
        }
    }
    
    /** Switches from the intro/charging animation over to live health tracking. */
    public void beginHealthTracking() {
        this.isCharging = false;
        this.isEmerging = false;
        this.bossBar.setOverlay(BossEvent.BossBarOverlay.PROGRESS);
    }

    public void addMiniEntity(ShadowCreakingEntity miniEntity) {
        this.trackedEntities.put(miniEntity.getUUID(), EntityPhase.MINI);
        this.previousHealthValues.put(miniEntity.getUUID(), miniEntity.getMaxHealth());
    }
    
    public void addTinyEntity(ShadowCreakingEntity tinyEntity) {
        this.trackedEntities.put(tinyEntity.getUUID(), EntityPhase.TINY);
        this.previousHealthValues.put(tinyEntity.getUUID(), tinyEntity.getMaxHealth());
    }
    
    public void removeEntity(UUID entityUuid) {
        Float remaining = this.previousHealthValues.get(entityUuid);
        if (remaining != null && remaining > 0.0F) {
            this.totalDamageDealt += remaining;
            float bossBarPercent = Math.max(0.0f, (TOTAL_MAX_HEALTH - this.totalDamageDealt) / TOTAL_MAX_HEALTH);
            this.bossBar.setProgress(Math.min(1.0f, bossBarPercent));
        }
        this.trackedEntities.remove(entityUuid);
        this.previousHealthValues.remove(entityUuid);
    }
    
    /**
     * Boss bar = (TOTAL_MAX_HEALTH - totalDamageDealt) / TOTAL_MAX_HEALTH, so it starts
     * full and only empties once every phase across the whole fight is dead.
     */
    private void updateBossBarHealth(ServerLevel world) {
        for (Map.Entry<UUID, EntityPhase> entry : this.trackedEntities.entrySet()) {
            UUID entityUuid = entry.getKey();
            
            ShadowCreakingEntity entity = this.findEntityByUuid(world, entityUuid);
            if (entity != null) {
                float currentHealth = entity.isDeadOrDying() ? 0.0F : entity.getHealth();
                
                Float previousHealth = previousHealthValues.get(entityUuid);
                if (previousHealth == null) {
                    previousHealth = entity.getMaxHealth();
                    previousHealthValues.put(entityUuid, previousHealth);
                }
                
                float damageTaken = previousHealth - currentHealth;
                if (damageTaken > 0) {
                    totalDamageDealt += damageTaken;
                    previousHealthValues.put(entityUuid, currentHealth);
                }
            }
        }
        
        float remainingHealth = TOTAL_MAX_HEALTH - totalDamageDealt;
        float bossBarPercent = Math.max(0.0f, remainingHealth / TOTAL_MAX_HEALTH);
        this.bossBar.setProgress(Math.min(1.0f, bossBarPercent));
    }
    
    private ShadowCreakingEntity findEntityByUuid(ServerLevel world, UUID uuid) {
        net.minecraft.world.entity.Entity entity = world.getEntity(uuid);
        if (entity instanceof ShadowCreakingEntity ShadowCreakingEntity) {
            return ShadowCreakingEntity;
        }
        
        // main world didn't have it, entity may have changed dimension
        net.minecraft.server.MinecraftServer server = world.getServer();
        if (server != null) {
            for (ServerLevel serverWorld : server.getAllLevels()) {
                if (serverWorld != world) {
                    entity = serverWorld.getEntity(uuid);
                    if (entity instanceof ShadowCreakingEntity ShadowCreakingEntity) {
                        return ShadowCreakingEntity;
                    }
                }
            }
        }
        
        return null;
    }
    
    private void updateNearbyPlayers(ServerLevel world) {
        if (!this.isActive) return;
        
        Set<ServerPlayer> nearbyPlayers = new HashSet<>();
        
        if (this.isCharging && !this.isEmerging && this.altarPos != null && this.altarDimension != null) {
            // no entity yet, so proximity is measured from the altar instead
            try {
                net.minecraft.server.MinecraftServer server = world.getServer();
                if (server != null) {
                    ServerLevel altarWorld = server.getLevel(this.altarDimension);
                    if (altarWorld != null) {
                        net.minecraft.world.phys.Vec3 altarVec = net.minecraft.world.phys.Vec3.atCenterOf(this.altarPos);
                        AABB searchBox = AABB.ofSize(altarVec, VIEW_DISTANCE * 2, VIEW_DISTANCE * 2, VIEW_DISTANCE * 2);
                        List<Player> playersInRange = altarWorld.getEntitiesOfClass(Player.class, searchBox,
                            p -> new Vec3(p.getX(), p.getY(), p.getZ()).distanceTo(altarVec) <= VIEW_DISTANCE);
                        
                        for (Player player : playersInRange) {
                            if (player instanceof ServerPlayer serverPlayer) {
                                nearbyPlayers.add(serverPlayer);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                com.theendupdate.TheEndUpdate.LOGGER.error("Error getting players during charging phase", e);
                return;
            }
        } else if (this.isEmerging || (this.isCharging && this.altarPos == null)) {
            for (UUID entityUuid : this.trackedEntities.keySet()) {
                ShadowCreakingEntity entity = this.findEntityByUuid(world, entityUuid);
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
            }
        } else {
            for (UUID entityUuid : this.trackedEntities.keySet()) {
                ShadowCreakingEntity entity = this.findEntityByUuid(world, entityUuid);
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
        this.isCharging = false;
        this.isEmerging = false;
        this.altarPos = null;
        this.altarDimension = null;
        this.bossBar.removeAllPlayers();
        this.trackedEntities.clear();
        this.previousHealthValues.clear();
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
    
    public UUID getMainEntityUuid() {
        return this.mainEntityUuid;
    }
}
