package com.theendupdate;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.util.ActionResult;
import net.fabricmc.fabric.api.registry.CompostingChanceRegistry;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.equipment.trim.ArmorTrim;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.entity.EquipmentSlot;
import net.fabricmc.fabric.api.registry.FuelRegistryEvents;
import net.minecraft.entity.ItemEntity;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.Identifier;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.entity.passive.CowEntity;
import com.theendupdate.accessor.CowEntityAnimationAccessor;
import net.minecraft.block.BlockState;

public class TemplateMod implements ModInitializer {
    public static final String MOD_ID = "theendupdate";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final boolean DEBUG_MODE = false;
    public static final int VOID_SAP_SPREAD_RADIUS = 5;
    private static final int SPECTRAL_INTERVAL = 5;
    private static final int MAGNET_INTERVAL = 7;
    private static final Object2IntOpenHashMap<UUID> SPECTRAL_TICKERS = new Object2IntOpenHashMap<>();
    private static final Object2IntOpenHashMap<UUID> MAGNET_TICKERS = new Object2IntOpenHashMap<>();
    private static final Identifier TARD_SHELL_ARMOR_MODIFIER_ID = Identifier.of(MOD_ID, "tard_shell_armor_bonus");
    private static final Identifier TARD_SHELL_TOUGHNESS_MODIFIER_ID = Identifier.of(MOD_ID, "tard_shell_toughness_bonus");

    @Override
    public void onInitialize() {
        
        // Initialize mod content
        com.theendupdate.registry.ModItemGroups.register();
        com.theendupdate.registry.ModBlockEntities.register();
        com.theendupdate.registry.ModBlocks.registerModBlocks(); // Register blocks first
        com.theendupdate.registry.ModBlockEntities.registerSignBlockEntities(); // Register sign block entities AFTER sign blocks are created
        com.theendupdate.registry.ModScreenHandlers.register();
        com.theendupdate.registry.ModStructures.register();
        com.theendupdate.registry.ModStatusEffects.register(); // Register status effects before potions
        com.theendupdate.registry.ModPotions.register(); // Register potions
        com.theendupdate.registry.ModItems.registerModItems();
        
        // Register custom brewing recipes (must be after potions are registered)
        net.fabricmc.fabric.api.registry.FabricBrewingRecipeRegistryBuilder.BUILD.register(builder -> {
            builder.registerPotionRecipe(
                net.minecraft.potion.Potions.AWKWARD,
                net.minecraft.item.Items.SLIME_BALL,
                com.theendupdate.registry.ModPotions.PHANTOM_WARD
            );
            builder.registerPotionRecipe(
                com.theendupdate.registry.ModPotions.PHANTOM_WARD,
                net.minecraft.item.Items.REDSTONE,
                com.theendupdate.registry.ModPotions.LONG_PHANTOM_WARD
            );
        });
        com.theendupdate.registry.ModSounds.register();
        com.theendupdate.registry.ModEntities.registerModEntities();
        com.theendupdate.world.ModEntitySpawns.register();
        com.theendupdate.registry.ModWorldgen.registerAll();
        com.theendupdate.network.EndFlashNetworking.registerServerReceiver();
        
        
        // Register strippable blocks (axe right-click)
        net.fabricmc.fabric.api.registry.StrippableBlockRegistry.register(
            com.theendupdate.registry.ModBlocks.SHADOW_CRYPTOMYCOTA,
            com.theendupdate.registry.ModBlocks.STRIPPED_SHADOW_CRYPTOMYCOTA
        );
        net.fabricmc.fabric.api.registry.StrippableBlockRegistry.register(
            com.theendupdate.registry.ModBlocks.SHADOW_UMBRACARP,
            com.theendupdate.registry.ModBlocks.STRIPPED_SHADOW_UMBRACARP
        );
        
        // Fuels: make ethereal wood a poor fuel source (~half normal wood burn time)
        FuelRegistryEvents.BUILD.register((builder, context) -> {
            final int ETHEREAL_FUEL_TICKS = context.baseSmeltTime() / 2; // usually 100 ticks
            builder.add(com.theendupdate.registry.ModBlocks.ETHEREAL_PLANKS, ETHEREAL_FUEL_TICKS);
            builder.add(com.theendupdate.registry.ModBlocks.ETHEREAL_SPOROCARP, ETHEREAL_FUEL_TICKS);
            builder.add(com.theendupdate.registry.ModBlocks.ETHEREAL_PUSTULE, ETHEREAL_FUEL_TICKS);
            builder.add(com.theendupdate.registry.ModBlocks.ETHEREAL_STAIRS, ETHEREAL_FUEL_TICKS);
            builder.add(com.theendupdate.registry.ModBlocks.ETHEREAL_SLAB, ETHEREAL_FUEL_TICKS);
            builder.add(com.theendupdate.registry.ModBlocks.ETHEREAL_FENCE, ETHEREAL_FUEL_TICKS);
            builder.add(com.theendupdate.registry.ModBlocks.ETHEREAL_FENCE_GATE, ETHEREAL_FUEL_TICKS);
            builder.add(com.theendupdate.registry.ModBlocks.ETHEREAL_DOOR, ETHEREAL_FUEL_TICKS);
            builder.add(com.theendupdate.registry.ModBlocks.ETHEREAL_TRAPDOOR, ETHEREAL_FUEL_TICKS);
            builder.add(com.theendupdate.registry.ModBlocks.ETHEREAL_BUTTON, context.baseSmeltTime() / 4); // very low
            builder.add(com.theendupdate.registry.ModBlocks.ETHEREAL_PRESSURE_PLATE, context.baseSmeltTime() / 4); // very low
            // Shadow wood (same fuel values as ethereal)
            builder.add(com.theendupdate.registry.ModBlocks.SHADOW_CRYPTOMYCOTA, ETHEREAL_FUEL_TICKS);
            builder.add(com.theendupdate.registry.ModBlocks.SHADOW_UMBRACARP, ETHEREAL_FUEL_TICKS);
            builder.add(com.theendupdate.registry.ModBlocks.STRIPPED_SHADOW_CRYPTOMYCOTA, ETHEREAL_FUEL_TICKS);
            builder.add(com.theendupdate.registry.ModBlocks.STRIPPED_SHADOW_UMBRACARP, ETHEREAL_FUEL_TICKS);
        });

        // Composting: add all plant items with appropriate chances
        CompostingChanceRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.MOLD_SPORE.asItem(), 0.30f);
        CompostingChanceRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.MOLD_SPORE_TUFT.asItem(), 0.65f);
        CompostingChanceRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.MOLD_SPORE_SPROUT.asItem(), 0.65f);
        CompostingChanceRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.TENDRIL_SPROUT.asItem(), 0.65f);
        CompostingChanceRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.TENDRIL_THREAD.asItem(), 0.65f);
        CompostingChanceRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.TENDRIL_CORE.asItem(), 0.65f);
        CompostingChanceRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.ETHEREAL_BUTTON.asItem(), 0.70f);
        CompostingChanceRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.ETHEREAL_PRESSURE_PLATE.asItem(), 0.72f);
        CompostingChanceRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.ETHEREAL_FENCE.asItem(), 0.74f);
        CompostingChanceRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.ETHEREAL_FENCE_GATE.asItem(), 0.74f);
        CompostingChanceRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.ETHEREAL_TRAPDOOR.asItem(), 0.74f);
        CompostingChanceRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.ETHEREAL_DOOR.asItem(), 0.74f);
        CompostingChanceRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.ETHEREAL_SLAB.asItem(), 0.78f);
        CompostingChanceRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.ETHEREAL_STAIRS.asItem(), 0.82f);
        CompostingChanceRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.ETHEREAL_PLANKS.asItem(), 0.85f);
        CompostingChanceRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.ETHEREAL_SPOROCARP.asItem(), 0.85f);
        CompostingChanceRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.ETHEREAL_PUSTULE.asItem(), 0.85f);
        CompostingChanceRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.SHADOW_CRYPTOMYCOTA.asItem(), 0.85f);
        CompostingChanceRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.SHADOW_UMBRACARP.asItem(), 0.85f);
        CompostingChanceRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.STRIPPED_SHADOW_CRYPTOMYCOTA.asItem(), 0.85f);
        CompostingChanceRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.STRIPPED_SHADOW_UMBRACARP.asItem(), 0.85f);
        CompostingChanceRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.MOLD_BLOCK.asItem(), 0.65f);
        CompostingChanceRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.ENDER_CHRYSANTHEMUM.asItem(), 0.65f);
        CompostingChanceRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.VOID_BLOOM.asItem(), 0.65f);
        CompostingChanceRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.MOLD_CRAWL.asItem(), 0.50f);

        // Global hooks to ensure mold_crawl reacts even if vanilla neighbor updates are skipped by renderer state:
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!world.isClient()) {
                // Clicked block position
                BlockPos clickedPos = hitResult.getBlockPos();
                // Intended placed position is one block in the clicked face direction
                BlockPos placedPos = clickedPos.offset(hitResult.getSide());
                com.theendupdate.block.MoldcrawlBlock.reactToExternalChange(world, clickedPos);
                com.theendupdate.block.MoldcrawlBlock.reactToExternalChange(world, placedPos);
            }
            return ActionResult.PASS;
        });

        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, entity) -> {
            if (!world.isClient()) {
                com.theendupdate.block.MoldcrawlBlock.reactToExternalChange(world, pos);
            }
        });

        // Post-gen spawners
        com.theendupdate.world.EtherealOrbOnCrystalsSpawner.init();
        
        // World load complete - using in-memory tracking
        // (Silent - no logging)
        
        // Scan chunks for existing closed chrysanthemums on load (async, End-only, player-vicinity, disabled by default)
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents.CHUNK_LOAD.register((world, chunk) -> {
            // End dimension check
            if (world.getRegistryKey() != net.minecraft.world.World.END) return;
            if (world instanceof net.minecraft.server.world.ServerWorld serverWorld) {
                com.theendupdate.network.EnderChrysanthemumCloser.scanChunkForClosed(serverWorld, chunk);
            }
        });
        

        // Server tick: spawn subtle END_ROD particles around players wearing spectral trims
        ServerTickEvents.END_SERVER_TICK.register((MinecraftServer server) -> {
            for (ServerWorld world : server.getWorlds()) {
                if (world != null) {
                    com.theendupdate.entity.ShadowCreakingBossBarRegistry.tickAll(world);
                    com.theendupdate.entity.KingPhantomBossBarRegistry.tickAll(world);
                    com.theendupdate.network.EnderChrysanthemumCloser.tick(world);
                }
                
                // Handle cow and mooshroom milking animation  
                for (CowEntity cow : world.getEntitiesByType(net.minecraft.entity.EntityType.COW, entity -> true)) {
                    if (cow instanceof CowEntityAnimationAccessor accessor) {
                        long startTime = accessor.theendupdate$getAnimationStartTime();
                        if (startTime > 0L) {
                            long elapsed = world.getTime() - startTime;
                            if (elapsed < 100L) {
                                // Freeze the cow's movement
                                cow.setVelocity(Vec3d.ZERO);
                                if (elapsed % 5 == 0) {
                                    cow.getNavigation().stop();
                                }
                            } else {
                                // Animation finished, reset
                                accessor.theendupdate$setAnimationStartTime(0L);
                            }
                        }
                    }
                }
                
                // Handle mooshrooms (they have a separate EntityType)
                for (net.minecraft.entity.passive.MooshroomEntity mooshroom : world.getEntitiesByType(net.minecraft.entity.EntityType.MOOSHROOM, entity -> true)) {
                    if (mooshroom instanceof CowEntityAnimationAccessor accessor) {
                        long startTime = accessor.theendupdate$getAnimationStartTime();
                        if (startTime > 0L) {
                            long elapsed = world.getTime() - startTime;
                            if (elapsed < 100L) {
                                // Freeze the mooshroom's movement
                                mooshroom.setVelocity(Vec3d.ZERO);
                                if (elapsed % 5 == 0) {
                                    mooshroom.getNavigation().stop();
                                }
                            } else {
                                // Animation finished, reset
                                accessor.theendupdate$setAnimationStartTime(0L);
                            }
                        }
                    }
                }
                
                
                boolean theendupdate$trackerCadence = (world.getTime() % 20) == 0; // Every second
                ObjectOpenHashSet<UUID> tickPlayers = new ObjectOpenHashSet<>();
                for (ServerPlayerEntity player : world.getPlayers()) {
                    if (!player.isAlive()) continue;
                    UUID uuid = player.getUuid();
                    tickPlayers.add(uuid);
                    
                    // Auto-bind Shadow Hunter's Trackers that don't have coordinates yet
                    if (theendupdate$trackerCadence) {
                        theendupdate$autoBindShadowHuntersTrackers(world, player);
                    }
                    
                    // Spectral trim: spawn particles at each piece's location (per-player cadence)
                    if (theendupdate$shouldExecute(SPECTRAL_TICKERS, uuid, SPECTRAL_INTERVAL)) {
                        theendupdate$spawnSpectralTrimParticles(world, player);
                    }

                    // Gravitite trim: attract nearby items based on number of pieces (2,4,6,8 blocks)
                    int gravititePieces = theendupdate$countGravititeTrimPieces(player);
                    if (gravititePieces > 0 && theendupdate$shouldExecute(MAGNET_TICKERS, uuid, MAGNET_INTERVAL)) {
                        theendupdate$pullNearbyItems(world, player, gravititePieces);
                    }

                    // Tard shell trim: boost armor hardness (armor & toughness) by 25% per trimmed piece
                    theendupdate$updateTardShellTrimBonus(player);
                }
                SPECTRAL_TICKERS.keySet().removeIf(uuid -> !tickPlayers.contains(uuid));
                MAGNET_TICKERS.keySet().removeIf(uuid -> !tickPlayers.contains(uuid));
            }
        });
    }

    private static boolean theendupdate$shouldExecute(Object2IntOpenHashMap<UUID> ticker, UUID uuid, int interval) {
        int remaining = ticker.getOrDefault(uuid, 0);
        if (remaining > 0) {
            ticker.put(uuid, remaining - 1);
            return false;
        }
        ticker.put(uuid, Math.max(0, interval - 1));
        return true;
    }

    private static int theendupdate$countSpectralTrimPieces(ServerPlayerEntity player) {
        int count = 0;
        try {
            for (EquipmentSlot slot : new EquipmentSlot[] { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET }) {
                ItemStack armor = player.getEquippedStack(slot);
                if (armor == null || armor.isEmpty()) continue;
                ArmorTrim trim = armor.get(DataComponentTypes.TRIM);
                if (trim == null) continue;
                Identifier matId = trim.material().getKey().map(RegistryKey::getValue).orElse(null);
                if (matId == null) continue;
                String path = matId.getPath();
                if ("spectral".equals(path) || "spectral_cluster".equals(path)) count++;
            }
        } catch (Throwable ignored) {}
        return count;
    }

    private static int theendupdate$countGravititeTrimPieces(ServerPlayerEntity player) {
        int count = 0;
        try {
            for (EquipmentSlot slot : new EquipmentSlot[] { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET }) {
                ItemStack armor = player.getEquippedStack(slot);
                if (armor == null || armor.isEmpty()) continue;
                ArmorTrim trim = armor.get(DataComponentTypes.TRIM);
                if (trim == null) continue;
                Identifier matId = trim.material().getKey().map(RegistryKey::getValue).orElse(null);
                if (matId == null) continue;
                if ("gravitite".equals(matId.getPath())) count++;
            }
        } catch (Throwable ignored) {}
        return count;
    }

    private static void theendupdate$updateTardShellTrimBonus(ServerPlayerEntity player) {
        int tardShellPieces = 0;
        try {
            for (EquipmentSlot slot : new EquipmentSlot[] { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET }) {
                ItemStack armor = player.getEquippedStack(slot);
                if (armor == null || armor.isEmpty()) continue;
                ArmorTrim trim = armor.get(DataComponentTypes.TRIM);
                if (!theendupdate$isTardShellTrim(trim)) continue;
                tardShellPieces++;
            }
        } catch (Throwable ignored) {}

        double multiplier = tardShellPieces * 0.25;

        double armorBase = theendupdate$getAttributeValueWithoutModifier(player, EntityAttributes.ARMOR, TARD_SHELL_ARMOR_MODIFIER_ID);
        double toughnessBase = theendupdate$getAttributeValueWithoutModifier(player, EntityAttributes.ARMOR_TOUGHNESS, TARD_SHELL_TOUGHNESS_MODIFIER_ID);

        theendupdate$applyTardShellBonus(player, EntityAttributes.ARMOR, TARD_SHELL_ARMOR_MODIFIER_ID, armorBase * multiplier);
        theendupdate$applyTardShellBonus(player, EntityAttributes.ARMOR_TOUGHNESS, TARD_SHELL_TOUGHNESS_MODIFIER_ID, toughnessBase * multiplier);
    }

    private static boolean theendupdate$isTardShellTrim(ArmorTrim trim) {
        if (trim == null) return false;
        Identifier matId = trim.material().getKey().map(RegistryKey::getValue).orElse(null);
        return matId != null && "tard_shell".equals(matId.getPath());
    }

    private static double theendupdate$getAttributeValueWithoutModifier(
        ServerPlayerEntity player,
        RegistryEntry<EntityAttribute> attribute,
        Identifier modifierId
    ) {
        EntityAttributeInstance instance = player.getAttributeInstance(attribute);
        if (instance == null) {
            return 0.0;
        }
        if (instance.getModifier(modifierId) != null) {
            instance.removeModifier(modifierId);
        }
        return instance.getValue();
    }

    private static void theendupdate$applyTardShellBonus(
        ServerPlayerEntity player,
        RegistryEntry<EntityAttribute> attribute,
        Identifier modifierId,
        double value
    ) {
        EntityAttributeInstance instance = player.getAttributeInstance(attribute);
        if (instance == null) {
            return;
        }

        EntityAttributeModifier existing = instance.getModifier(modifierId);
        if (value <= 0.0) {
            if (existing != null) {
                instance.removeModifier(modifierId);
            }
            return;
        }

        if (existing != null) {
            if (existing.operation() == EntityAttributeModifier.Operation.ADD_VALUE && Math.abs(existing.value() - value) < 1.0e-4) {
                return;
            }
            instance.removeModifier(modifierId);
        }

        instance.addPersistentModifier(new EntityAttributeModifier(modifierId, value, EntityAttributeModifier.Operation.ADD_VALUE));
    }

    private static void theendupdate$pullNearbyItems(ServerWorld world, ServerPlayerEntity player, int pieces) {
        int range = switch (pieces) { case 1 -> 2; case 2 -> 4; case 3 -> 6; default -> 8; };
        Box box = player.getBoundingBox().expand(range);
        try {
            java.util.List<ItemEntity> items = world.getEntitiesByClass(
                ItemEntity.class,
                box,
                e -> e != null && e.isAlive() && !e.hasNoGravity()
            );
            if (items.isEmpty()) return;

            Vec3d playerPos = new Vec3d(player.getX(), player.getY() + 0.5, player.getZ());
            double cadenceScale = MAGNET_INTERVAL;
            double lerpFactor = Math.min(1.0, 0.08 * cadenceScale);
            double targetSpeed = 0.16 + 0.02 * pieces;
            double maxSpeed = 0.35 + 0.05 * pieces;
            double upwardBias = 0.15;
            double maxDistanceSq = range * range * 4.0;

            for (ItemEntity item : items) {
                Vec3d itemPos = new Vec3d(item.getX(), item.getY(), item.getZ());
                Vec3d diff = playerPos.subtract(itemPos);
                double distSq = diff.lengthSquared();
                if (distSq < 1.0e-4 || distSq > maxDistanceSq) continue;

                double dist = Math.sqrt(distSq);
                Vec3d dir = diff.normalize();
                dir = dir.add(0.0, upwardBias / Math.max(1.0, dist), 0.0).normalize();

                Vec3d targetVel = dir.multiply(targetSpeed);
                Vec3d currentVel = item.getVelocity();
                Vec3d newVel = currentVel.lerp(targetVel, lerpFactor);

                if (newVel.lengthSquared() > maxSpeed * maxSpeed) {
                    newVel = newVel.normalize().multiply(maxSpeed);
                }
                newVel = newVel.multiply(0.96);

                item.setVelocity(newVel);
                item.velocityDirty = true;
                item.age = 0;
            }
        } catch (Throwable ignored) {}
    }

    private static void theendupdate$spawnSpectralTrimParticles(ServerWorld world, ServerPlayerEntity player) {
        try {
            double baseX = player.getX();
            double baseZ = player.getZ();
            double baseY = player.getY();
            
            // Check each armor slot and spawn particles at appropriate positions
            for (EquipmentSlot slot : new EquipmentSlot[] { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET }) {
                ItemStack armor = player.getEquippedStack(slot);
                if (armor == null || armor.isEmpty()) continue;
                ArmorTrim trim = armor.get(DataComponentTypes.TRIM);
                if (trim == null) continue;
                Identifier matId = trim.material().getKey().map(RegistryKey::getValue).orElse(null);
                if (matId == null) continue;
                String path = matId.getPath();
                if (!"spectral".equals(path) && !"spectral_cluster".equals(path)) continue;
                
                // Calculate Y position based on slot
                double yOffset = switch (slot) {
                    case HEAD -> player.getEyeHeight(player.getPose()); // ~1.6 for standing
                    case CHEST -> 1.0;
                    case LEGS -> 0.6;
                    case FEET -> 0.1;
                    default -> 1.0;
                };
                
                double particleY = baseY + yOffset;
                
                // Spawn particles visible to other players
                for (ServerPlayerEntity other : world.getPlayers()) {
                    if (other == player) continue;
                    other.networkHandler.sendPacket(new ParticleS2CPacket(
                        ParticleTypes.END_ROD,
                        false, false,
                        baseX, particleY, baseZ,
                        0.0f, 0.0f, 0.0f,
                        0.0f, 1
                    ));
                }
            }
        } catch (Throwable ignored) {}
    }

    private static void theendupdate$autoBindShadowHuntersTrackers(ServerWorld world, ServerPlayerEntity player) {
        try {
            // Check all inventory slots for Shadow Hunter's Trackers that need binding
            for (int slot = 0; slot < player.getInventory().size(); slot++) {
                var stack = player.getInventory().getStack(slot);
                if (stack == null || stack.isEmpty() || !stack.isOf(net.minecraft.item.Items.RECOVERY_COMPASS)) continue;
                
                var custom = stack.get(net.minecraft.component.DataComponentTypes.CUSTOM_DATA);
                if (custom == null) continue;
                
                var tag = custom.copyNbt();
                if (!tag.contains("shadow_hunter_tracker") || !tag.getBoolean("shadow_hunter_tracker").orElse(false)) continue;
                
                // Check if already bound
                if (tag.contains("hollow_tree_x") && tag.contains("hollow_tree_y") && tag.contains("hollow_tree_z")) continue;
                
                // Try to locate a hollow shadow tree and bind to it
                var target = locateHollowTree(world, player.getBlockPos());
                if (target != null) {
                    // Bind to the hollow shadow tree
                    var newTag = tag.copy();
                    newTag.putInt("hollow_tree_x", target.getX());
                    newTag.putInt("hollow_tree_y", target.getY());
                    newTag.putInt("hollow_tree_z", target.getZ());
                    newTag.putString("world_dimension", world.getRegistryKey().getValue().toString());
                    newTag.putBoolean("precise_mode", false); // Start in structure mode
                    stack.set(net.minecraft.component.DataComponentTypes.CUSTOM_DATA, net.minecraft.component.type.NbtComponent.of(newTag));
                    
                }
            }
        } catch (Throwable ignored) {}
    }

    private static BlockPos locateHollowTree(ServerWorld world, BlockPos origin) {
        try {
            var registry = world.getRegistryManager().getOrThrow(net.minecraft.registry.RegistryKeys.STRUCTURE);
            var id = com.theendupdate.registry.ModStructures.SHADOW_HOLLOW_TREE_KEY.getValue();
            var entry = registry.getEntry(id).orElse(null);
            if (entry == null) return null;

            var pair = world.getChunkManager().getChunkGenerator().locateStructure(
                world,
                net.minecraft.registry.entry.RegistryEntryList.of(entry),
                origin,
                512,
                false
            );
            return pair == null ? null : pair.getFirst();
        } catch (Throwable ignored) {
            return null;
        }
    }

}