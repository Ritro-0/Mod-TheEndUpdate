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
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.entity.ItemEntity;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.block.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TemplateMod implements ModInitializer {
    public static final String MOD_ID = "theendupdate";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("[EndUpdate] onInitialize() starting");
        
        // Initialize mod content
        com.theendupdate.registry.ModBlocks.registerModBlocks();
        com.theendupdate.registry.ModBlockEntities.register();
        com.theendupdate.registry.ModScreenHandlers.register();
        com.theendupdate.registry.ModStructures.register();
        com.theendupdate.registry.ModItems.registerModItems();
        com.theendupdate.registry.ModSounds.register();
        com.theendupdate.registry.ModEntities.registerModEntities();
        com.theendupdate.registry.ModWorldgen.registerAll();
        
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

        // Global hooks to ensure mold_crawl reacts even if vanilla neighbor updates are skipped by renderer state:
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient) return ActionResult.PASS;
            BlockPos pos = hitResult.getBlockPos();
            BlockState state = world.getBlockState(pos);
            if (state.isOf(com.theendupdate.registry.ModBlocks.MOLD_CRAWL)) {
                world.updateNeighbors(pos, state.getBlock());
            }
            return ActionResult.PASS;
        });

        // Block break events for mold_crawl
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (state.isOf(com.theendupdate.registry.ModBlocks.MOLD_CRAWL)) {
                world.updateNeighbors(pos, state.getBlock());
            }
            return true; // Allow the break to continue
        });

        // Commands
        try { com.theendupdate.debug.DebugCommands.register(); } catch (Throwable ignored) {}
        // Post-gen spawners
        com.theendupdate.world.EtherealOrbOnCrystalsSpawner.init();
        LOGGER.info("[EndUpdate] onInitialize() completed");

        // Server tick: spawn subtle END_ROD particles around players wearing spectral trims
        ServerTickEvents.END_SERVER_TICK.register((MinecraftServer server) -> {
            for (ServerWorld world : server.getWorlds()) {
                if (world != null) {
                    com.theendupdate.entity.ShadowCreakingBossBarRegistry.tickAll(world);
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
                Vec3d playerPos = player.getPos();
                Vec3d itemPos = item.getPos();
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