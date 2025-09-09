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
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
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
        com.theendupdate.registry.ModItems.registerModItems();
        com.theendupdate.registry.ModSounds.register();
        com.theendupdate.registry.ModEntities.registerModEntities();
        
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

            // Mirror fuel values for Shadow wood set
            builder.add(com.theendupdate.registry.ModBlocks.SHADOW_PLANKS, ETHEREAL_FUEL_TICKS);
            builder.add(com.theendupdate.registry.ModBlocks.SHADOW_CRYPTOMYCOTA, ETHEREAL_FUEL_TICKS);
            builder.add(com.theendupdate.registry.ModBlocks.UMBRACARP, ETHEREAL_FUEL_TICKS);
            builder.add(com.theendupdate.registry.ModBlocks.SHADOW_STAIRS, ETHEREAL_FUEL_TICKS);
            builder.add(com.theendupdate.registry.ModBlocks.SHADOW_SLAB, ETHEREAL_FUEL_TICKS);
            builder.add(com.theendupdate.registry.ModBlocks.SHADOW_FENCE, ETHEREAL_FUEL_TICKS);
            builder.add(com.theendupdate.registry.ModBlocks.SHADOW_FENCE_GATE, ETHEREAL_FUEL_TICKS);
            builder.add(com.theendupdate.registry.ModBlocks.SHADOW_DOOR, ETHEREAL_FUEL_TICKS);
            builder.add(com.theendupdate.registry.ModBlocks.SHADOW_TRAPDOOR, ETHEREAL_FUEL_TICKS);
            builder.add(com.theendupdate.registry.ModBlocks.SHADOW_BUTTON, context.baseSmeltTime() / 4);
            builder.add(com.theendupdate.registry.ModBlocks.SHADOW_PRESSURE_PLATE, ETHEREAL_FUEL_TICKS);
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

        // Worldgen registration
        com.theendupdate.registry.ModWorldgen.registerAll();
        // Commands
        try { com.theendupdate.debug.DebugCommands.register(); } catch (Throwable ignored) {}
        // Post-gen spawners
        com.theendupdate.world.EtherealOrbOnCrystalsSpawner.init();
        LOGGER.info("[EndUpdate] onInitialize() completed");
        
        // Server tick: spawn subtle END_ROD particles around players wearing spectral trims
        ServerTickEvents.END_SERVER_TICK.register((MinecraftServer server) -> {
            for (ServerWorld world : server.getWorlds()) {
                if (world.getTime() % 10 != 0) continue; // check twice per second
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
                    if (spectralPieces <= 0) continue;

                    // Scale chance with pieces: 1->0.5, 2->0.7, 3->0.85, 4->1.0
                    float spawnChance = switch (spectralPieces) {
                        case 1 -> 0.5f;
                        case 2 -> 0.7f;
                        case 3 -> 0.85f;
                        default -> 1.0f;
                    };
                    if (world.getRandom().nextFloat() > spawnChance) continue;

                    // Orientation vectors based on player yaw
                    double yawRad = Math.toRadians(player.getYaw());
                    double fwdX = -Math.sin(yawRad);
                    double fwdZ =  Math.cos(yawRad);
                    double rightX =  Math.cos(yawRad);
                    double rightZ =  Math.sin(yawRad);

                    // Alternate sides over time (front/back or left/right) and never both
                    boolean phase = ((world.getTime() / 20) % 2) == 0; // toggles ~every second

                    if (hasChest) {
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

                    if (hasHead) {
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

                    if (hasLegs) {
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

                    if (hasFeet) {
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
}


