package com.theendupdate;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.registry.CompostableRegistry;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.fabricmc.fabric.api.registry.FuelValueEvents;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.theendupdate.accessor.CowEntityAnimationAccessor;

public class TheEndUpdate implements ModInitializer {
    public static final String MOD_ID = "theendupdate";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final boolean DEBUG_MODE = false;
    public static final int VOID_SAP_SPREAD_RADIUS = 5;
    private static final int SPECTRAL_INTERVAL = 5;
    private static final int MAGNET_INTERVAL = 7;
    private static final Object2IntOpenHashMap<UUID> SPECTRAL_TICKERS = new Object2IntOpenHashMap<>();
    private static final Object2IntOpenHashMap<UUID> MAGNET_TICKERS = new Object2IntOpenHashMap<>();
    private static final Identifier TARDIGRADE_SHELL_ARMOR_MODIFIER_ID = Identifier.fromNamespaceAndPath(MOD_ID, "tardigrade_shell_armor_bonus");
    private static final Identifier TARDIGRADE_SHELL_TOUGHNESS_MODIFIER_ID = Identifier.fromNamespaceAndPath(MOD_ID, "tardigrade_shell_toughness_bonus");

    @Override
    public void onInitialize() {
        com.theendupdate.registry.ModItemGroups.register();
        com.theendupdate.registry.ModBlockEntities.register();
        com.theendupdate.registry.ModBlocks.registerModBlocks();
        com.theendupdate.registry.ModBlockEntities.registerSignBlockEntities(); // needs the sign blocks to already exist
        com.theendupdate.registry.ModScreenHandlers.register();
        com.theendupdate.registry.ModStructures.register();
        com.theendupdate.registry.ModStatusEffects.register(); // must run before potions
        com.theendupdate.registry.ModPotions.register();
        com.theendupdate.registry.ModItems.registerModItems();
        
        // brewing recipes reference potions, so must come after ModPotions.register()
        net.fabricmc.fabric.api.registry.FabricPotionBrewingBuilder.BUILD.register(builder -> {
            builder.addMix(
                net.minecraft.world.item.alchemy.Potions.AWKWARD,
                net.minecraft.world.item.Items.SLIME_BALL,
                com.theendupdate.registry.ModPotions.PHANTOM_WARD
            );
            builder.addMix(
                com.theendupdate.registry.ModPotions.PHANTOM_WARD,
                net.minecraft.world.item.Items.REDSTONE,
                com.theendupdate.registry.ModPotions.LONG_PHANTOM_WARD
            );
        });
        com.theendupdate.registry.ModSounds.register();
        com.theendupdate.registry.ModParticles.registerModParticles();
        com.theendupdate.registry.ModEntities.registerModEntities();
        com.theendupdate.world.ModEntitySpawns.register();
        com.theendupdate.registry.ModWorldgen.registerAll();
        com.theendupdate.network.EndFlashNetworking.registerServerReceiver();
        
        net.fabricmc.fabric.api.registry.StrippableBlockRegistry.register(
            com.theendupdate.registry.ModBlocks.SHADOW_CRYPTOMYCOTA,
            com.theendupdate.registry.ModBlocks.STRIPPED_SHADOW_CRYPTOMYCOTA
        );
        net.fabricmc.fabric.api.registry.StrippableBlockRegistry.register(
            com.theendupdate.registry.ModBlocks.SHADOW_UMBRACARP,
            com.theendupdate.registry.ModBlocks.STRIPPED_SHADOW_UMBRACARP
        );
        
        // ethereal wood burns at half the normal rate
        FuelValueEvents.BUILD.register((builder, context) -> {
            final int ETHEREAL_FUEL_TICKS = context.baseSmeltTime() / 2;
            builder.add(com.theendupdate.registry.ModBlocks.ETHEREAL_PLANKS, ETHEREAL_FUEL_TICKS);
            builder.add(com.theendupdate.registry.ModBlocks.ETHEREAL_SPOROCARP, ETHEREAL_FUEL_TICKS);
            builder.add(com.theendupdate.registry.ModBlocks.ETHEREAL_PUSTULE, ETHEREAL_FUEL_TICKS);
            builder.add(com.theendupdate.registry.ModBlocks.ETHEREAL_STAIRS, ETHEREAL_FUEL_TICKS);
            builder.add(com.theendupdate.registry.ModBlocks.ETHEREAL_SLAB, ETHEREAL_FUEL_TICKS);
            builder.add(com.theendupdate.registry.ModBlocks.ETHEREAL_FENCE, ETHEREAL_FUEL_TICKS);
            builder.add(com.theendupdate.registry.ModBlocks.ETHEREAL_FENCE_GATE, ETHEREAL_FUEL_TICKS);
            builder.add(com.theendupdate.registry.ModBlocks.ETHEREAL_DOOR, ETHEREAL_FUEL_TICKS);
            builder.add(com.theendupdate.registry.ModBlocks.ETHEREAL_TRAPDOOR, ETHEREAL_FUEL_TICKS);
            builder.add(com.theendupdate.registry.ModBlocks.ETHEREAL_BUTTON, context.baseSmeltTime() / 4);
            builder.add(com.theendupdate.registry.ModBlocks.ETHEREAL_PRESSURE_PLATE, context.baseSmeltTime() / 4);
            // shadow wood uses the same fuel values as ethereal
            builder.add(com.theendupdate.registry.ModBlocks.SHADOW_CRYPTOMYCOTA, ETHEREAL_FUEL_TICKS);
            builder.add(com.theendupdate.registry.ModBlocks.SHADOW_UMBRACARP, ETHEREAL_FUEL_TICKS);
            builder.add(com.theendupdate.registry.ModBlocks.STRIPPED_SHADOW_CRYPTOMYCOTA, ETHEREAL_FUEL_TICKS);
            builder.add(com.theendupdate.registry.ModBlocks.STRIPPED_SHADOW_UMBRACARP, ETHEREAL_FUEL_TICKS);
        });

        CompostableRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.MOLD_SPORE.asItem(), 0.30f);
        CompostableRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.MOLD_SPORE_TUFT.asItem(), 0.65f);
        CompostableRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.MOLD_SPORE_SPROUT.asItem(), 0.65f);
        CompostableRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.TENDRIL_SPROUT.asItem(), 0.65f);
        CompostableRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.TENDRIL_THREAD.asItem(), 0.65f);
        CompostableRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.TENDRIL_CORE.asItem(), 0.65f);
        CompostableRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.ETHEREAL_BUTTON.asItem(), 0.70f);
        CompostableRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.ETHEREAL_PRESSURE_PLATE.asItem(), 0.72f);
        CompostableRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.ETHEREAL_FENCE.asItem(), 0.74f);
        CompostableRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.ETHEREAL_FENCE_GATE.asItem(), 0.74f);
        CompostableRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.ETHEREAL_TRAPDOOR.asItem(), 0.74f);
        CompostableRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.ETHEREAL_DOOR.asItem(), 0.74f);
        CompostableRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.ETHEREAL_SLAB.asItem(), 0.78f);
        CompostableRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.ETHEREAL_STAIRS.asItem(), 0.82f);
        CompostableRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.ETHEREAL_PLANKS.asItem(), 0.85f);
        CompostableRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.ETHEREAL_SPOROCARP.asItem(), 0.85f);
        CompostableRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.ETHEREAL_PUSTULE.asItem(), 0.85f);
        CompostableRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.SHADOW_CRYPTOMYCOTA.asItem(), 0.85f);
        CompostableRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.SHADOW_UMBRACARP.asItem(), 0.85f);
        CompostableRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.STRIPPED_SHADOW_CRYPTOMYCOTA.asItem(), 0.85f);
        CompostableRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.STRIPPED_SHADOW_UMBRACARP.asItem(), 0.85f);
        CompostableRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.MOLD_BLOCK.asItem(), 0.65f);
        CompostableRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.ENDER_CHRYSANTHEMUM.asItem(), 0.65f);
        CompostableRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.VOID_BLOOM.asItem(), 0.65f);
        CompostableRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.MOLD_CRAWL.asItem(), 0.50f);

        // catches cases where mold_crawl should react but vanilla neighbor updates get skipped
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!world.isClientSide()) {
                BlockPos clickedPos = hitResult.getBlockPos();
                BlockPos placedPos = clickedPos.relative(hitResult.getDirection());
                com.theendupdate.block.MoldcrawlBlock.reactToExternalChange(world, clickedPos);
                com.theendupdate.block.MoldcrawlBlock.reactToExternalChange(world, placedPos);
            }
            return InteractionResult.PASS;
        });

        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, entity) -> {
            if (!world.isClientSide()) {
                com.theendupdate.block.MoldcrawlBlock.reactToExternalChange(world, pos);
            }
        });
        
        com.theendupdate.world.EtherealOrbOnCrystalsSpawner.init();
        com.theendupdate.world.ShadowlandsEyesSpawner.init();
        
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents.CHUNK_LOAD.register((level, chunk, generated) -> {
            if (level.dimension() != net.minecraft.world.level.Level.END) return;
            com.theendupdate.network.EnderChrysanthemumCloser.scanChunkForClosed(level, chunk);
        });
        

        ServerTickEvents.END_SERVER_TICK.register((MinecraftServer server) -> {
            for (ServerLevel world : server.getAllLevels()) {
                if (world != null) {
                    com.theendupdate.entity.ShadowCreakingBossBarRegistry.tickAll(world);
                    com.theendupdate.entity.KingPhantomBossBarRegistry.tickAll(world);
                    com.theendupdate.network.EnderChrysanthemumCloser.tick(world);
                }
                
                // milking animation freezes the cow in place while it plays
                for (Cow cow : world.getEntities(net.minecraft.world.entity.EntityTypes.COW, entity -> true)) {
                    if (cow instanceof CowEntityAnimationAccessor accessor) {
                        long startTime = accessor.theendupdate$getAnimationStartTime();
                        if (startTime > 0L) {
                            long elapsed = world.getGameTime() - startTime;
                            if (elapsed < 100L) {
                                cow.setDeltaMovement(Vec3.ZERO);
                                if (elapsed % 5 == 0) {
                                    cow.getNavigation().stop();
                                }
                            } else {
                                accessor.theendupdate$setAnimationStartTime(0L);
                            }
                        }
                    }
                }
                
                // mooshrooms are a separate EntityType from cows, so need their own loop
                for (net.minecraft.world.entity.animal.cow.MushroomCow mooshroom : world.getEntities(net.minecraft.world.entity.EntityTypes.MOOSHROOM, entity -> true)) {
                    if (mooshroom instanceof CowEntityAnimationAccessor accessor) {
                        long startTime = accessor.theendupdate$getAnimationStartTime();
                        if (startTime > 0L) {
                            long elapsed = world.getGameTime() - startTime;
                            if (elapsed < 100L) {
                                mooshroom.setDeltaMovement(Vec3.ZERO);
                                if (elapsed % 5 == 0) {
                                    mooshroom.getNavigation().stop();
                                }
                            } else {
                                accessor.theendupdate$setAnimationStartTime(0L);
                            }
                        }
                    }
                }
                
                
                boolean theendupdate$trackerCadence = (world.getGameTime() % 20) == 0; // once per second
                ObjectOpenHashSet<UUID> tickPlayers = new ObjectOpenHashSet<>();
                for (ServerPlayer player : world.players()) {
                    if (!player.isAlive()) continue;
                    UUID uuid = player.getUUID();
                    tickPlayers.add(uuid);
                    
                    // Shadow Hunter's Trackers auto-bind to the nearest hollow tree, End only
                    if (theendupdate$trackerCadence && world.dimension().equals(net.minecraft.world.level.Level.END)) {
                        theendupdate$autoBindShadowHuntersTrackers(world, player);
                    }
                    
                    if (theendupdate$shouldExecute(SPECTRAL_TICKERS, uuid, SPECTRAL_INTERVAL)) {
                        theendupdate$spawnSpectralTrimParticles(world, player);
                    }

                    // pull range scales with trim piece count: 2/4/6/8 blocks
                    int gravititePieces = theendupdate$countGravititeTrimPieces(player);
                    if (gravititePieces > 0 && theendupdate$shouldExecute(MAGNET_TICKERS, uuid, MAGNET_INTERVAL)) {
                        theendupdate$pullNearbyItems(world, player, gravititePieces);
                    }

                    // +25% armor & toughness per trimmed piece
                    theendupdate$updateTardigradeShellTrimBonus(player);
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

    private static int theendupdate$countSpectralTrimPieces(ServerPlayer player) {
        int count = 0;
        try {
            for (EquipmentSlot slot : new EquipmentSlot[] { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET }) {
                ItemStack armor = player.getItemBySlot(slot);
                if (armor == null || armor.isEmpty()) continue;
                ArmorTrim trim = armor.get(DataComponents.TRIM);
                if (trim == null) continue;
                Identifier matId = trim.material().unwrapKey().map(ResourceKey::identifier).orElse(null);
                if (matId == null) continue;
                String path = matId.getPath();
                if ("spectral".equals(path) || "spectral_cluster".equals(path)) count++;
            }
        } catch (Throwable ignored) {}
        return count;
    }

    private static int theendupdate$countGravititeTrimPieces(ServerPlayer player) {
        int count = 0;
        try {
            for (EquipmentSlot slot : new EquipmentSlot[] { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET }) {
                ItemStack armor = player.getItemBySlot(slot);
                if (armor == null || armor.isEmpty()) continue;
                ArmorTrim trim = armor.get(DataComponents.TRIM);
                if (trim == null) continue;
                Identifier matId = trim.material().unwrapKey().map(ResourceKey::identifier).orElse(null);
                if (matId == null) continue;
                if ("gravitite".equals(matId.getPath())) count++;
            }
        } catch (Throwable ignored) {}
        return count;
    }

    private static void theendupdate$updateTardigradeShellTrimBonus(ServerPlayer player) {
        int tardigradeShellPieces = 0;
        try {
            for (EquipmentSlot slot : new EquipmentSlot[] { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET }) {
                ItemStack armor = player.getItemBySlot(slot);
                if (armor == null || armor.isEmpty()) continue;
                ArmorTrim trim = armor.get(DataComponents.TRIM);
                if (!theendupdate$isTardigradeShellTrim(trim)) continue;
                tardigradeShellPieces++;
            }
        } catch (Throwable ignored) {}

        double multiplier = tardigradeShellPieces * 0.25;

        double armorBase = theendupdate$getAttributeValueWithoutModifier(player, Attributes.ARMOR, TARDIGRADE_SHELL_ARMOR_MODIFIER_ID);
        double toughnessBase = theendupdate$getAttributeValueWithoutModifier(player, Attributes.ARMOR_TOUGHNESS, TARDIGRADE_SHELL_TOUGHNESS_MODIFIER_ID);

        theendupdate$applyTardigradeShellBonus(player, Attributes.ARMOR, TARDIGRADE_SHELL_ARMOR_MODIFIER_ID, armorBase * multiplier);
        theendupdate$applyTardigradeShellBonus(player, Attributes.ARMOR_TOUGHNESS, TARDIGRADE_SHELL_TOUGHNESS_MODIFIER_ID, toughnessBase * multiplier);
    }

    private static boolean theendupdate$isTardigradeShellTrim(ArmorTrim trim) {
        if (trim == null) return false;
        Identifier matId = trim.material().unwrapKey().map(ResourceKey::identifier).orElse(null);
        return matId != null && "tardigrade_shell".equals(matId.getPath());
    }

    private static double theendupdate$getAttributeValueWithoutModifier(
        ServerPlayer player,
        Holder<Attribute> attribute,
        Identifier modifierId
    ) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return 0.0;
        }
        if (instance.getModifier(modifierId) != null) {
            instance.removeModifier(modifierId);
        }
        return instance.getValue();
    }

    private static void theendupdate$applyTardigradeShellBonus(
        ServerPlayer player,
        Holder<Attribute> attribute,
        Identifier modifierId,
        double value
    ) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }

        AttributeModifier existing = instance.getModifier(modifierId);
        if (value <= 0.0) {
            if (existing != null) {
                instance.removeModifier(modifierId);
            }
            return;
        }

        if (existing != null) {
            if (existing.operation() == AttributeModifier.Operation.ADD_VALUE && Math.abs(existing.amount() - value) < 1.0e-4) {
                return;
            }
            instance.removeModifier(modifierId);
        }

        instance.addPermanentModifier(new AttributeModifier(modifierId, value, AttributeModifier.Operation.ADD_VALUE));
    }

    private static void theendupdate$pullNearbyItems(ServerLevel world, ServerPlayer player, int pieces) {
        int range = switch (pieces) { case 1 -> 2; case 2 -> 4; case 3 -> 6; default -> 8; };
        AABB box = player.getBoundingBox().inflate(range);
        try {
            java.util.List<ItemEntity> items = world.getEntitiesOfClass(
                ItemEntity.class,
                box,
                e -> e != null && e.isAlive() && !e.isNoGravity()
            );
            if (items.isEmpty()) return;

            Vec3 playerPos = new Vec3(player.getX(), player.getY() + 0.5, player.getZ());
            double cadenceScale = MAGNET_INTERVAL;
            double lerpFactor = Math.min(1.0, 0.08 * cadenceScale);
            double targetSpeed = 0.16 + 0.02 * pieces;
            double maxSpeed = 0.35 + 0.05 * pieces;
            double upwardBias = 0.15;
            double maxDistanceSq = range * range * 4.0;

            for (ItemEntity item : items) {
                Vec3 itemPos = new Vec3(item.getX(), item.getY(), item.getZ());
                Vec3 diff = playerPos.subtract(itemPos);
                double distSq = diff.lengthSqr();
                if (distSq < 1.0e-4 || distSq > maxDistanceSq) continue;

                double dist = Math.sqrt(distSq);
                Vec3 dir = diff.normalize();
                dir = dir.add(0.0, upwardBias / Math.max(1.0, dist), 0.0).normalize();

                Vec3 targetVel = dir.scale(targetSpeed);
                Vec3 currentVel = item.getDeltaMovement();
                Vec3 newVel = currentVel.lerp(targetVel, lerpFactor);

                if (newVel.lengthSqr() > maxSpeed * maxSpeed) {
                    newVel = newVel.normalize().scale(maxSpeed);
                }
                newVel = newVel.scale(0.96);

                item.setDeltaMovement(newVel);
                item.needsSync = true;
                item.tickCount = 0;
            }
        } catch (Throwable ignored) {}
    }

    private static void theendupdate$spawnSpectralTrimParticles(ServerLevel world, ServerPlayer player) {
        try {
            double baseX = player.getX();
            double baseZ = player.getZ();
            double baseY = player.getY();
            double yawRad = Math.toRadians(player.getYRot());
            double forwardX = -Math.sin(yawRad);
            double forwardZ = Math.cos(yawRad);
            double forwardOffset = 0.28;
            
            for (EquipmentSlot slot : new EquipmentSlot[] { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET }) {
                ItemStack armor = player.getItemBySlot(slot);
                if (armor == null || armor.isEmpty()) continue;
                ArmorTrim trim = armor.get(DataComponents.TRIM);
                if (trim == null) continue;
                Identifier matId = trim.material().unwrapKey().map(ResourceKey::identifier).orElse(null);
                if (matId == null) continue;
                String path = matId.getPath();
                if (!"spectral".equals(path) && !"spectral_cluster".equals(path)) continue;
                
                double yOffset = switch (slot) {
                    case HEAD -> 1.55;
                    case CHEST -> 1.10;
                    case LEGS -> 0.75;
                    case FEET -> 0.25;
                    default -> 1.0;
                };
                
                double particleY = baseY + yOffset;
                double frontX = baseX + forwardX * forwardOffset;
                double frontZ = baseZ + forwardZ * forwardOffset;
                double backX = baseX - forwardX * forwardOffset;
                double backZ = baseZ - forwardZ * forwardOffset;
                
                // wearer gets their own client-only third-person effect, so skip them here
                for (ServerPlayer other : world.players()) {
                    if (other == player) continue;
                    world.sendParticles(other, ParticleTypes.END_ROD, false, false, frontX, particleY, frontZ, 1, 0.0, 0.0, 0.0, 0.0);
                    world.sendParticles(other, ParticleTypes.END_ROD, false, false, backX, particleY, backZ, 1, 0.0, 0.0, 0.0, 0.0);
                }
            }
        } catch (Throwable ignored) {}
    }

    private static void theendupdate$autoBindShadowHuntersTrackers(ServerLevel world, ServerPlayer player) {
        try {
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                var stack = player.getInventory().getItem(slot);
                if (stack == null || stack.isEmpty() || !stack.is(net.minecraft.world.item.Items.RECOVERY_COMPASS)) continue;
                
                var custom = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
                if (custom == null) continue;
                
                var tag = custom.copyTag();
                if (!tag.contains("shadow_hunter_tracker") || !tag.getBoolean("shadow_hunter_tracker").orElse(false)) continue;
                
                if (tag.contains("hollow_tree_x") && tag.contains("hollow_tree_y") && tag.contains("hollow_tree_z")) continue;
                
                var target = com.theendupdate.world.HollowTreeLocator.locate(world, player.blockPosition());
                if (target != null) {
                    var newTag = tag.copy();
                    newTag.putInt("hollow_tree_x", target.getX());
                    newTag.putInt("hollow_tree_y", target.getY());
                    newTag.putInt("hollow_tree_z", target.getZ());
                    newTag.putString("world_dimension", world.dimension().identifier().toString());
                    newTag.putBoolean("precise_mode", false);
                    stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(newTag));
                }
            }
        } catch (Throwable ignored) {}
    }

}