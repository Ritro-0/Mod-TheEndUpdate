package com.theendupdate;

import net.fabricmc.api.ModInitializer;
// removed unused lifecycle/command imports
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
// unused imports removed
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.util.ActionResult;
import net.fabricmc.fabric.api.registry.CompostingChanceRegistry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
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
import net.minecraft.loot.LootPool;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.loot.function.SetCountLootFunction;
import net.minecraft.loot.LootTables;
// Explosion blast immunity for certain item entities is implemented via different API versions.
// For now, remove Fabric ExplosionEvents usage due to missing module in this env.
// import net.fabricmc.fabric.api.event.world.ExplosionEvents;
import net.minecraft.entity.ItemEntity;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.decoration.ArmorStandEntity;
// entity attribute registry called from ModEntities
// debug-related imports removed
// (no server tick hooks used currently)

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
        com.theendupdate.registry.ModMapDecorations.register();
        com.theendupdate.registry.ModItems.registerModItems();
        com.theendupdate.registry.ModSounds.register();
        com.theendupdate.registry.ModEntities.registerModEntities();
        
        // Shadow Hunter's Map no longer uses global use item event
        
        // Entity attributes are registered inside ModEntities.registerModEntities()
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
            builder.add(com.theendupdate.registry.ModBlocks.ETHEREAL_PRESSURE_PLATE, ETHEREAL_FUEL_TICKS);

            // Shadow wood set - full burn time like traditional logs
            builder.add(com.theendupdate.registry.ModBlocks.SHADOW_PLANKS, context.baseSmeltTime());
            builder.add(com.theendupdate.registry.ModBlocks.SHADOW_CRYPTOMYCOTA, context.baseSmeltTime());
            builder.add(com.theendupdate.registry.ModBlocks.SHADOW_UMBRACARP, context.baseSmeltTime());
            builder.add(com.theendupdate.registry.ModBlocks.SHADOW_STAIRS, context.baseSmeltTime());
            builder.add(com.theendupdate.registry.ModBlocks.SHADOW_SLAB, context.baseSmeltTime() / 2);
            builder.add(com.theendupdate.registry.ModBlocks.SHADOW_FENCE, context.baseSmeltTime());
            builder.add(com.theendupdate.registry.ModBlocks.SHADOW_FENCE_GATE, context.baseSmeltTime());
            builder.add(com.theendupdate.registry.ModBlocks.SHADOW_DOOR, context.baseSmeltTime());
            builder.add(com.theendupdate.registry.ModBlocks.SHADOW_TRAPDOOR, context.baseSmeltTime());
            builder.add(com.theendupdate.registry.ModBlocks.SHADOW_BUTTON, context.baseSmeltTime() / 4);
            builder.add(com.theendupdate.registry.ModBlocks.SHADOW_PRESSURE_PLATE, context.baseSmeltTime() / 2);
        });

        // Composting: mirror vanilla chances
        // - Moss Block: 65%
        // - Twisting Vines: 50%
        // - Tall Grass: 30%
        CompostingChanceRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.MOLD_BLOCK.asItem(), 0.65f);
        CompostingChanceRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.MOLD_CRAWL.asItem(), 0.50f);
        CompostingChanceRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.VOID_BLOOM.asItem(), 0.30f);
        CompostingChanceRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.ENDER_CHRYSANTHEMUM.asItem(), 0.30f);
        // Mold plants composting (match vanilla equivalents):
        // - Nether Sprouts ~30%
        CompostingChanceRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.MOLD_SPORE.asItem(), 0.30f);
        // - Warped Roots ~65%
        CompostingChanceRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.MOLD_SPORE_TUFT.asItem(), 0.65f);
        // - Large Fern (double-tall) ~65%
        CompostingChanceRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.MOLD_SPORE_SPROUT.asItem(), 0.65f);

        // Tendril plants: match Warped Fungus (~65%)
        CompostingChanceRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.TENDRIL_SPROUT.asItem(), 0.65f);
        CompostingChanceRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.TENDRIL_THREAD.asItem(), 0.65f);
        CompostingChanceRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.TENDRIL_CORE.asItem(), 0.65f);

        // Ethereal wood set: all higher than moss (65%), scaled by size
        // Small
        CompostingChanceRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.ETHEREAL_BUTTON.asItem(), 0.70f);
        CompostingChanceRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.ETHEREAL_PRESSURE_PLATE.asItem(), 0.72f);
        // Mid-size components
        CompostingChanceRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.ETHEREAL_FENCE.asItem(), 0.74f);
        CompostingChanceRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.ETHEREAL_FENCE_GATE.asItem(), 0.74f);
        CompostingChanceRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.ETHEREAL_TRAPDOOR.asItem(), 0.74f);
        CompostingChanceRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.ETHEREAL_DOOR.asItem(), 0.74f);
        // Larger shapes
        CompostingChanceRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.ETHEREAL_SLAB.asItem(), 0.78f);
        CompostingChanceRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.ETHEREAL_STAIRS.asItem(), 0.82f);
        // Full blocks / logs
        CompostingChanceRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.ETHEREAL_PLANKS.asItem(), 0.85f);
        CompostingChanceRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.ETHEREAL_SPOROCARP.asItem(), 0.85f);
        CompostingChanceRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.ETHEREAL_PUSTULE.asItem(), 0.85f);

        // Brewing handled via registry data (potion_mixing) JSON

        // Global hooks to ensure mold_crawl reacts even if vanilla neighbor updates are skipped by renderer state:
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!world.isClient) {
                // Clicked block position
                var clickedPos = hitResult.getBlockPos();
                // Intended placed position is one block in the clicked face direction
                var placedPos = clickedPos.offset(hitResult.getSide());
                com.theendupdate.block.MoldcrawlBlock.reactToExternalChange(world, clickedPos);
                com.theendupdate.block.MoldcrawlBlock.reactToExternalChange(world, placedPos);
            }
            return ActionResult.PASS;
        });
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, entity) -> {
            if (!world.isClient) {
                com.theendupdate.block.MoldcrawlBlock.reactToExternalChange(world, pos);
            }
        });

        // Wooden cone interaction now handled on the item itself

        // Blast-proof items: rely on fireproof to survive lava/fire and high blast resistance to persist in typical detonations.

        // Worldgen registration
        com.theendupdate.registry.ModWorldgen.registerAll();
        
        // Add shadow hunters map to end city loot
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            if (LootTables.END_CITY_TREASURE_CHEST.equals(key)) {
                LootPool.Builder poolBuilder = LootPool.builder()
                    .rolls(ConstantLootNumberProvider.create(1))
                    .with(ItemEntry.builder(com.theendupdate.registry.ModItems.SHADOW_HUNTERS_MAP)
                        .weight(1) // Weight of 1 - as rare as enchanted books, ender pearls, and chorus fruit
                        .apply(SetCountLootFunction.builder(ConstantLootNumberProvider.create(1))));
                tableBuilder.pool(poolBuilder);
            }
        });
        
        // Commands
        try { com.theendupdate.debug.DebugCommands.register(); } catch (Throwable ignored) {}
        // Post-gen spawners
        com.theendupdate.world.EtherealOrbOnCrystalsSpawner.init();
        LOGGER.info("[EndUpdate] onInitialize() completed");
        
        // On player join, restore Shadow Hunter map icons from NBT so the marker persists across sessions
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            try {
                ServerPlayerEntity player = handler.getPlayer();
                if (player == null) return;
                ServerWorld world = player.getWorld();
                if (world == null) return;
                var inv = player.getInventory();
                for (int i = 0; i < inv.size(); i++) {
                    ItemStack stack = inv.getStack(i);
                    com.theendupdate.item.ShadowHuntersMapItem.restoreDecorationFromNbt(world, stack);
                }
                // Also check hands just in case
                com.theendupdate.item.ShadowHuntersMapItem.restoreDecorationFromNbt(world, player.getMainHandStack());
                com.theendupdate.item.ShadowHuntersMapItem.restoreDecorationFromNbt(world, player.getOffHandStack());
            } catch (Throwable ignored) {}
        });

        // Server tick: spawn subtle END_ROD particles around players wearing spectral trims
        ServerTickEvents.END_SERVER_TICK.register((MinecraftServer server) -> {
            for (ServerWorld world : server.getWorlds()) {
                // Update all active Shadow Creaking boss bars
                com.theendupdate.entity.ShadowCreakingBossBarRegistry.tickAll(world);
                // Cadence ~6.7 Hz for elegant motion
                boolean theendupdate$cadence = (world.getTime() % 3) == 0;
                // Process players (existing behavior)
                for (ServerPlayerEntity player : world.getPlayers()) {
                    if (!player.isAlive()) continue;

                    boolean hasHead = theendupdate$isSpectralTrim(player, EquipmentSlot.HEAD);
                    boolean hasChest = theendupdate$isSpectralTrim(player, EquipmentSlot.CHEST);
                    boolean hasLegs = theendupdate$isSpectralTrim(player, EquipmentSlot.LEGS);
                    boolean hasFeet = theendupdate$isSpectralTrim(player, EquipmentSlot.FEET);

                    int spectralPieces = 0;
                    if (hasHead) spectralPieces++;
                    if (hasChest) spectralPieces++;
                    if (hasLegs) spectralPieces++;
                    if (hasFeet) spectralPieces++;

                    // Spectral particles: gated but do not early-continue so other effects still run
                    boolean shouldSpawnSpectral = false;
                    if (spectralPieces > 0) {
                        float spawnChance = switch (spectralPieces) {
                            case 1 -> 0.5f;
                            case 2 -> 0.7f;
                            case 3 -> 0.85f;
                            default -> 1.0f;
                        };
                        shouldSpawnSpectral = world.getRandom().nextFloat() <= spawnChance;
                    }

                    // Orientation vectors based on player yaw
                    double yawRad = Math.toRadians(player.getYaw());
                    double fwdX = -Math.sin(yawRad);
                    double fwdZ =  Math.cos(yawRad);
                    double rightX =  Math.cos(yawRad);
                    double rightZ =  Math.sin(yawRad);

                    // Alternate sides over time (front/back or left/right) and never both
                    boolean phase = ((world.getTime() / 20) % 2) == 0; // toggles ~every second

                    if (shouldSpawnSpectral && hasChest) {
                        // Chest: front/back alternate. Heavier randomization while biased forward/back
                        double baseY = player.getY() + 1.30;
                        double fbMag = 0.85;
                        double lrMag = 0.45;
                        double yMag  = 0.28;
                        double offX = (phase ? fwdX : -fwdX) * fbMag;
                        double offZ = (phase ? fwdZ : -fwdZ) * fbMag;
                        boolean lateralLeft = world.getRandom().nextBoolean();
                        double latScale = lrMag * world.getRandom().nextDouble();
                        double latX = (lateralLeft ? -rightX : rightX) * latScale;
                        double latZ = (lateralLeft ? -rightZ : rightZ) * latScale;
                        double spreadFB = (world.getRandom().nextDouble() - 0.5) * 0.6;
                        double sprX = fwdX * spreadFB;
                        double sprZ = fwdZ * spreadFB;
                        double x = player.getX() + offX + latX + sprX + (world.getRandom().nextDouble() - 0.5) * 0.14;
                        double y = baseY + (world.getRandom().nextDouble() - 0.5) * yMag;
                        double z = player.getZ() + offZ + latZ + sprZ + (world.getRandom().nextDouble() - 0.5) * 0.14;
                        theendupdate$spawnForOthers(world, player, x, y, z);
                    }

                    if (shouldSpawnSpectral && hasHead) {
                        // Head: front/back alternate near helmet with more variability
                        double baseY = player.getEyeY() + 0.00;
                        double fbMag = 0.55;
                        double lrMag = 0.30;
                        double yMag  = 0.22;
                        double offX = (phase ? fwdX : -fwdX) * fbMag;
                        double offZ = (phase ? fwdZ : -fwdZ) * fbMag;
                        boolean lateralLeft = world.getRandom().nextBoolean();
                        double latScale = lrMag * world.getRandom().nextDouble();
                        double latX = (lateralLeft ? -rightX : rightX) * latScale;
                        double latZ = (lateralLeft ? -rightZ : rightZ) * latScale;
                        double spreadFB = (world.getRandom().nextDouble() - 0.5) * 0.5;
                        double sprX = fwdX * spreadFB;
                        double sprZ = fwdZ * spreadFB;
                        double x = player.getX() + offX + latX + sprX + (world.getRandom().nextDouble() - 0.5) * 0.14;
                        double y = baseY + (world.getRandom().nextDouble() - 0.5) * yMag;
                        double z = player.getZ() + offZ + latZ + sprZ + (world.getRandom().nextDouble() - 0.5) * 0.14;
                        theendupdate$spawnForOthers(world, player, x, y, z);
                    }

                    if (shouldSpawnSpectral && hasLegs) {
                        // Legs: left/right alternate near waist; increase lateral and add forward/back spread
                        double baseY = player.getY() + 0.95;
                        double lrMag = 0.55;
                        double fbMag = 0.35;
                        double yMag  = 0.18;
                        double offX = (phase ? rightX : -rightX) * lrMag;
                        double offZ = (phase ? rightZ : -rightZ) * lrMag;
                        boolean forwardSide = world.getRandom().nextBoolean();
                        double swayScale = fbMag * world.getRandom().nextDouble();
                        double swayX = (forwardSide ? fwdX : -fwdX) * swayScale;
                        double swayZ = (forwardSide ? fwdZ : -fwdZ) * swayScale;
                        double x = player.getX() + offX + swayX + (world.getRandom().nextDouble() - 0.5) * 0.16;
                        double y = baseY + (world.getRandom().nextDouble() - 0.5) * yMag;
                        double z = player.getZ() + offZ + swayZ + (world.getRandom().nextDouble() - 0.5) * 0.16;
                        theendupdate$spawnForOthers(world, player, x, y, z);
                    }

                    if (shouldSpawnSpectral && hasFeet) {
                        // Feet: left/right alternate near boots; increase lateral and forward/back sway
                        double baseY = player.getY() + 0.25;
                        double lrMag = 0.45;
                        double fbMag = 0.28;
                        double yMag  = 0.14;
                        double offX = (phase ? rightX : -rightX) * lrMag;
                        double offZ = (phase ? rightZ : -rightZ) * lrMag;
                        boolean forwardSide = world.getRandom().nextBoolean();
                        double swayScale = fbMag * world.getRandom().nextDouble();
                        double swayX = (forwardSide ? fwdX : -fwdX) * swayScale;
                        double swayZ = (forwardSide ? fwdZ : -fwdZ) * swayScale;
                        double x = player.getX() + offX + swayX + (world.getRandom().nextDouble() - 0.5) * 0.14;
                        double y = baseY + (world.getRandom().nextDouble() - 0.5) * yMag;
                        double z = player.getZ() + offZ + swayZ + (world.getRandom().nextDouble() - 0.5) * 0.14;
                        theendupdate$spawnForOthers(world, player, x, y, z);
                    }
                    // Gravitite trim: attract nearby items based on number of pieces (2,4,6,8 blocks)
                    int gravititePieces = theendupdate$countGravititeTrimPieces(player);
                    if (gravititePieces > 0 && theendupdate$cadence) {
                        theendupdate$pullNearbyItems(world, player, gravititePieces);
                    }
                }

                // Armor stands: spawn the same spectral-trim END_ROD particles when wearing spectral-trimmed armor
                // Collect nearby armor stands around players to limit processing to potentially visible entities
                java.util.Set<ArmorStandEntity> theendupdate$stands = new java.util.HashSet<>();
                for (ServerPlayerEntity player : world.getPlayers()) {
                    if (!player.isAlive()) continue;
                    // Use a moderate radius roughly matching typical view distance
                    Box box = player.getBoundingBox().expand(64.0);
                    java.util.List<ArmorStandEntity> found = world.getEntitiesByClass(ArmorStandEntity.class, box, e -> e != null && e.isAlive());
                    if (!found.isEmpty()) theendupdate$stands.addAll(found);
                }

                if (!theendupdate$stands.isEmpty()) {
                    boolean phase = ((world.getTime() / 20) % 2) == 0; // toggles ~every second
                    for (ArmorStandEntity stand : theendupdate$stands) {
                        if (stand == null || !stand.isAlive()) continue;
                        // Skip marker stands (invisible hitbox, often decorative) to avoid odd visuals
                        try { if (stand.isMarker()) continue; } catch (Throwable ignored) {}

                        boolean hasHead = theendupdate$isSpectralTrim(stand.getEquippedStack(EquipmentSlot.HEAD));
                        boolean hasChest = theendupdate$isSpectralTrim(stand.getEquippedStack(EquipmentSlot.CHEST));
                        boolean hasLegs = theendupdate$isSpectralTrim(stand.getEquippedStack(EquipmentSlot.LEGS));
                        boolean hasFeet = theendupdate$isSpectralTrim(stand.getEquippedStack(EquipmentSlot.FEET));

                        int spectralPieces = 0;
                        if (hasHead) spectralPieces++;
                        if (hasChest) spectralPieces++;
                        if (hasLegs) spectralPieces++;
                        if (hasFeet) spectralPieces++;
                        if (spectralPieces <= 0) continue;

                        float spawnChance = switch (spectralPieces) {
                            case 1 -> 0.5f;
                            case 2 -> 0.7f;
                            case 3 -> 0.85f;
                            default -> 1.0f;
                        };
                        if (world.getRandom().nextFloat() > spawnChance) continue;

                        // Orientation vectors based on armor stand body yaw (more accurate for stands)
                        double yawRad;
                        try { yawRad = Math.toRadians(stand.getBodyYaw()); } catch (Throwable t) { yawRad = Math.toRadians(stand.getYaw()); }
                        double fwdX = -Math.sin(yawRad);
                        double fwdZ =  Math.cos(yawRad);
                        double rightX =  Math.cos(yawRad);
                        double rightZ =  Math.sin(yawRad);

                        // Scale offsets relative to player height (1.8) so small/large stands look proportional
                        double heightScale;
                        try { heightScale = Math.max(0.5, stand.getHeight() / 1.80); } catch (Throwable t) { heightScale = 1.0; }
                        // Use entity width to keep particles outside the model silhouette
                        double width;
                        try { width = Math.max(0.45, stand.getWidth()); } catch (Throwable t) { width = 0.6; }
                        double baseRadius = (width * 0.5) + 0.10; // push a little outside the legs/boots
                        double jitterXY = 0.05 * heightScale;     // tighter jitter for crisp positioning

                        if (hasChest) {
                            double baseY = stand.getY() + 1.22 * heightScale;
                            double fbMag = baseRadius * 1.20;
                            double lrMag = baseRadius * 0.70;
                            double yMag  = 0.10 * heightScale;
                            double offX = (phase ? fwdX : -fwdX) * fbMag;
                            double offZ = (phase ? fwdZ : -fwdZ) * fbMag;
                            boolean lateralLeft = world.getRandom().nextBoolean();
                            double latScale = lrMag * (0.35 + 0.65 * world.getRandom().nextDouble());
                            double latX = (lateralLeft ? -rightX : rightX) * latScale;
                            double latZ = (lateralLeft ? -rightZ : rightZ) * latScale;
                            double x = stand.getX() + offX + latX + (world.getRandom().nextDouble() - 0.5) * jitterXY;
                            double y = baseY + (world.getRandom().nextDouble() - 0.5) * yMag;
                            double z = stand.getZ() + offZ + latZ + (world.getRandom().nextDouble() - 0.5) * jitterXY;
                            world.spawnParticles(ParticleTypes.END_ROD, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
                        }

                        if (hasHead) {
                            double baseY;
                            try { baseY = stand.getEyeY(); } catch (Throwable t) { baseY = stand.getY() + 1.50 * heightScale; }
                            double fbMag = baseRadius * 0.80;
                            double lrMag = baseRadius * 0.50;
                            double yMag  = 0.08 * heightScale;
                            double offX = (phase ? fwdX : -fwdX) * fbMag;
                            double offZ = (phase ? fwdZ : -fwdZ) * fbMag;
                            boolean lateralLeft = world.getRandom().nextBoolean();
                            double latScale = lrMag * (0.35 + 0.65 * world.getRandom().nextDouble());
                            double latX = (lateralLeft ? -rightX : rightX) * latScale;
                            double latZ = (lateralLeft ? -rightZ : rightZ) * latScale;
                            double x = stand.getX() + offX + latX + (world.getRandom().nextDouble() - 0.5) * jitterXY;
                            double y = baseY + (world.getRandom().nextDouble() - 0.5) * yMag;
                            double z = stand.getZ() + offZ + latZ + (world.getRandom().nextDouble() - 0.5) * jitterXY;
                            world.spawnParticles(ParticleTypes.END_ROD, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
                        }

                        if (hasLegs) {
                            double baseY = stand.getY() + 0.90 * heightScale;
                            double lrMag = baseRadius * 1.00;
                            double fbMag = baseRadius * 0.60;
                            double yMag  = 0.08 * heightScale;
                            double offX = (phase ? rightX : -rightX) * lrMag;
                            double offZ = (phase ? rightZ : -rightZ) * lrMag;
                            boolean forwardSide = world.getRandom().nextBoolean();
                            double swayScale = fbMag * (0.35 + 0.65 * world.getRandom().nextDouble());
                            double swayX = (forwardSide ? fwdX : -fwdX) * swayScale;
                            double swayZ = (forwardSide ? fwdZ : -fwdZ) * swayScale;
                            double x = stand.getX() + offX + swayX + (world.getRandom().nextDouble() - 0.5) * jitterXY;
                            double y = baseY + (world.getRandom().nextDouble() - 0.5) * yMag;
                            double z = stand.getZ() + offZ + swayZ + (world.getRandom().nextDouble() - 0.5) * jitterXY;
                            world.spawnParticles(ParticleTypes.END_ROD, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
                        }

                        if (hasFeet) {
                            double baseY = stand.getY() + 0.20 * heightScale;
                            double lrMag = baseRadius * 1.10;
                            double fbMag = baseRadius * 0.50;
                            double yMag  = 0.06 * heightScale;
                            double offX = (phase ? rightX : -rightX) * lrMag;
                            double offZ = (phase ? rightZ : -rightZ) * lrMag;
                            boolean forwardSide = world.getRandom().nextBoolean();
                            double swayScale = fbMag * (0.35 + 0.65 * world.getRandom().nextDouble());
                            double swayX = (forwardSide ? fwdX : -fwdX) * swayScale;
                            double swayZ = (forwardSide ? fwdZ : -fwdZ) * swayScale;
                            double x = stand.getX() + offX + swayX + (world.getRandom().nextDouble() - 0.5) * jitterXY;
                            double y = baseY + (world.getRandom().nextDouble() - 0.5) * yMag;
                            double z = stand.getZ() + offZ + swayZ + (world.getRandom().nextDouble() - 0.5) * jitterXY;
                            world.spawnParticles(ParticleTypes.END_ROD, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
                        }
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

    // Send END_ROD particle packets to all other players so the owner's own first-person view doesn't render them
    private static void theendupdate$spawnForOthers(ServerWorld world, ServerPlayerEntity owner, double x, double y, double z) {
        try {
            // Send vanilla particle to all players except the owner, so the owner won't see it in first-person
            for (ServerPlayerEntity other : world.getPlayers()) {
                if (other == owner) continue;
                other.networkHandler.sendPacket(new ParticleS2CPacket(
                    ParticleTypes.END_ROD,
                    false, // override limiter
                    false, // long distance
                    x, y, z,
                    0.0f, 0.0f, 0.0f,
                    0.0f,
                    1
                ));
            }
        } catch (Throwable t) {
            // Fallback to normal spawn if packet construction fails
            world.spawnParticles(ParticleTypes.END_ROD, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private static boolean theendupdate$isSpectralTrim(ServerPlayerEntity player, EquipmentSlot slot) {
        try {
            ItemStack armor = player.getEquippedStack(slot);
            if (armor == null || armor.isEmpty()) return false;
            ArmorTrim trim = armor.get(DataComponentTypes.TRIM);
            if (trim == null) return false;
            Identifier matId = trim.material().getKey().map(RegistryKey::getValue).orElse(null);
            if (matId == null) return false;
            String path = matId.getPath();
            return "spectral".equals(path) || "spectral_cluster".equals(path);
        } catch (Throwable ignored) {}
        return false;
    }

    // Server-side stack check (shared by armor stands)
    private static boolean theendupdate$isSpectralTrim(ItemStack armor) {
        try {
            if (armor == null || armor.isEmpty()) return false;
            ArmorTrim trim = armor.get(DataComponentTypes.TRIM);
            if (trim == null) return false;
            Identifier matId = trim.material().getKey().map(RegistryKey::getValue).orElse(null);
            if (matId == null) return false;
            String path = matId.getPath();
            return "spectral".equals(path) || "spectral_cluster".equals(path);
        } catch (Throwable ignored) {}
        return false;
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
        // Range scales 2,4,6,8 for 1..4 pieces
        int range = switch (pieces) { case 1 -> 2; case 2 -> 4; case 3 -> 6; default -> 8; };
        Box box = player.getBoundingBox().expand(range);
        try {
            java.util.List<ItemEntity> items = world.getEntitiesByClass(ItemEntity.class, box, e -> e != null && e.isAlive());
            if (items.isEmpty()) return;
            Vec3d playerPos = player.getPos().add(0.0, 0.8, 0.0);
            double baseAccel = 0.42 + 0.05 * pieces; // slightly reduced for smoother, elegant motion
            for (ItemEntity item : items) {
                // Skip if just thrown by the same player this tick (optional), keep simple for now
                Vec3d diff = playerPos.subtract(item.getPos());
                double dist = diff.length();
                if (dist < 0.001) continue;
                // Scale pull a bit with distance but clamp so far items still move
                double strength = baseAccel * Math.min(1.0, (dist / range) + 0.35);
                Vec3d dir = diff.normalize();
                // Add a slight upward bias to help climb steps/blocks against gravity
                dir = new Vec3d(dir.x, Math.max(dir.y + 0.06, 0.035), dir.z).normalize();
                Vec3d pull = dir.multiply(strength);
                // Blend pull with current velocity for smoothing
                Vec3d vel = item.getVelocity().multiply(0.80).add(pull.multiply(0.90));
                // Damp excessive velocities
                if (vel.lengthSquared() > 1.4) vel = vel.normalize().multiply(1.15);
                item.setVelocity(vel);
                item.velocityModified = true;
            }
        } catch (Throwable ignored) {}
    }
    
    // Removed registerShadowHuntersMapEvent()
}


