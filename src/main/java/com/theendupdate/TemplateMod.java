package com.theendupdate;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.util.ActionResult;
import net.fabricmc.fabric.api.registry.CompostingChanceRegistry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.equipment.trim.ArmorTrim;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.entity.EquipmentSlot;
import net.fabricmc.fabric.api.registry.FuelRegistryEvents;
import net.minecraft.entity.ItemEntity;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.entity.passive.CowEntity;
import com.theendupdate.accessor.CowEntityAnimationAccessor;
import net.minecraft.block.BlockState;

public class TemplateMod implements ModInitializer {
    public static final String MOD_ID = "theendupdate";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("[EndUpdate] onInitialize() starting");
        
        // Initialize mod content
        com.theendupdate.registry.ModItemGroups.register();
        com.theendupdate.registry.ModBlockEntities.register();
        com.theendupdate.registry.ModBlocks.registerModBlocks(); // Register blocks first
        com.theendupdate.registry.ModBlockEntities.registerSignBlockEntities(); // Register sign block entities AFTER sign blocks are created
        com.theendupdate.registry.ModScreenHandlers.register();
        com.theendupdate.registry.ModStructures.register();
        com.theendupdate.registry.ModItems.registerModItems();
        com.theendupdate.registry.ModSounds.register();
        com.theendupdate.registry.ModEntities.registerModEntities();
        com.theendupdate.registry.ModWorldgen.registerAll();
        
        
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
        
        // Register commands
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            com.theendupdate.command.FixFlowersCommand.register(dispatcher);
        });
        
        LOGGER.info("[EndUpdate] onInitialize() completed");

        // Server tick: spawn subtle END_ROD particles around players wearing spectral trims
        ServerTickEvents.END_SERVER_TICK.register((MinecraftServer server) -> {
            for (ServerWorld world : server.getWorlds()) {
                if (world != null) {
                    com.theendupdate.entity.ShadowCreakingBossBarRegistry.tickAll(world);
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
                                cow.velocityModified = true;
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
                                mooshroom.velocityModified = true;
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
                
                
                boolean theendupdate$cadence = (world.getTime() % 3) == 0;
                boolean theendupdate$trackerCadence = (world.getTime() % 20) == 0; // Every second
                for (ServerPlayerEntity player : world.getPlayers()) {
                    if (!player.isAlive()) continue;
                    
                    // Auto-bind Shadow Hunter's Trackers that don't have coordinates yet
                    if (theendupdate$trackerCadence) {
                        theendupdate$autoBindShadowHuntersTrackers(world, player);
                    }
                    
                    int count = theendupdate$countSpectralTrimPieces(player);
                    if (count >= 2) {
                        theendupdate$pullItems(world, player);
                    }
                    if (count >= 3 && theendupdate$cadence) {
                        double x = player.getX();
                        double y = player.getY() + 1.0;
                        double z = player.getZ();
                        theendupdate$spawnForOthers(world, player, x, y, z);
                    }

                    // Gravitite trim: attract nearby items based on number of pieces (2,4,6,8 blocks)
                    int gravititePieces = theendupdate$countGravititeTrimPieces(player);
                    if (gravititePieces > 0 && theendupdate$cadence) {
                        theendupdate$pullNearbyItems(world, player, gravititePieces);
                    }
                }
            }
        });
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

    private static void theendupdate$pullNearbyItems(ServerWorld world, ServerPlayerEntity player, int pieces) {
        int range = switch (pieces) { case 1 -> 2; case 2 -> 4; case 3 -> 6; default -> 8; };
        Box box = player.getBoundingBox().expand(range);
        try {
            java.util.List<ItemEntity> items = world.getEntitiesByClass(ItemEntity.class, box, e -> e != null && e.isAlive());
            if (items.isEmpty()) return;
            Vec3d playerPos = new Vec3d(player.getX(), player.getY(), player.getZ()).add(0.0, 0.8, 0.0);
            double baseAccel = 0.42 + 0.05 * pieces;
            for (ItemEntity item : items) {
                Vec3d diff = playerPos.subtract(new Vec3d(item.getX(), item.getY(), item.getZ()));
                double dist = diff.length();
                if (dist < 0.001) continue;
                double strength = baseAccel * Math.min(1.0, (dist / range) + 0.35);
                Vec3d dir = diff.normalize();
                dir = new Vec3d(dir.x, Math.max(dir.y + 0.06, 0.035), dir.z).normalize();
                Vec3d pull = dir.multiply(strength);
                Vec3d vel = item.getVelocity().multiply(0.80).add(pull.multiply(0.90));
                if (vel.lengthSquared() > 1.4) vel = vel.normalize().multiply(1.15);
                item.setVelocity(vel);
                item.velocityModified = true;
            }
        } catch (Throwable ignored) {}
    }

    private static void theendupdate$spawnForOthers(ServerWorld world, ServerPlayerEntity owner, double x, double y, double z) {
        try {
            for (ServerPlayerEntity other : world.getPlayers()) {
                if (other == owner) continue;
                other.networkHandler.sendPacket(new ParticleS2CPacket(
                    ParticleTypes.END_ROD,
                    false, false,
                    x, y, z,
                    0.0f, 0.0f, 0.0f,
                    0.0f, 1
                ));
            }
        } catch (Throwable ignored) {}
    }

    private static void theendupdate$pullItems(ServerWorld world, ServerPlayerEntity player) {
        try {
            Box box = player.getBoundingBox().expand(2.5);
            var items = world.getEntitiesByClass(ItemEntity.class, box, item -> true);
            for (ItemEntity item : items) {
                if (item == null || item.isRemoved()) continue;
                Vec3d playerPos = new Vec3d(player.getX(), player.getY(), player.getZ());
                Vec3d itemPos = new Vec3d(item.getX(), item.getY(), item.getZ());
                Vec3d pull = playerPos.subtract(itemPos).normalize().multiply(0.15);
                Vec3d vel = item.getVelocity().multiply(0.80).add(pull.multiply(0.90));
                if (vel.lengthSquared() > 1.4) vel = vel.normalize().multiply(1.15);
                item.setVelocity(vel);
                item.velocityModified = true;
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