package com.theendupdate.mixin;

import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public abstract class RecoveryCompassUseMixin {

    @Inject(method = "use", at = @At("HEAD"))
    private void theendupdate$teleportOnSneakUse(World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<net.minecraft.util.ActionResult> cir) {
        ItemStack stack = user.getStackInHand(hand);
        if (stack == null || stack.isEmpty()) return;

        // Handle Shadow Hunter's Tracker (recovery compass with custom data)
        NbtComponent custom = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (custom != null) {
            var tag = custom.copyNbt();
            if (tag.contains("shadow_hunter_tracker") && tag.getBoolean("shadow_hunter_tracker").orElse(false)) {
                // Shadow Hunter's Tracker should work like quantum gateway compass but without teleportation
                // Client: short-circuit to success to avoid further processing; server handles binding
                if (world.isClient()) {
                    return;
                }
                
                // Check if we need to locate and bind to a hollow shadow tree (first use)
                boolean needsBinding = !(tag.contains("hollow_tree_x") && tag.contains("hollow_tree_y") && tag.contains("hollow_tree_z"));
                if (needsBinding) {
                    // Auto-bind to the nearest structure on first use
                    handleShadowHuntersTrackerBinding(world, user, hand, cir);
                    return;
                } else if (user.isSneaking()) {
                    // Only handle toggle mode when sneaking
                    handleShadowHuntersTrackerToggle(world, user, hand, cir);
                    return;
                } else {
                    // Regular right-click without sneaking - just show current mode
                    handleShadowHuntersTrackerStatus(world, user, hand, cir);
                    return;
                }
            }
        }

        // Handle quantum gateway compasses (only when sneaking)
        if (!user.isSneaking()) return;
        
        // Handle quantum gateway compasses
        if (!stack.isOf(Items.RECOVERY_COMPASS)) return;

        custom = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (custom == null) return;
        var tag = custom.copyNbt();
        if (!(tag.contains("gx") && tag.contains("gy") && tag.contains("gz") && tag.contains("gd"))) return;

        // Client: short-circuit to success to avoid further processing; server performs the teleport
        if (world.isClient()) {
            return;
        }

        // Per-stack cooldown (only for tagged compasses): block if still cooling down
        long now = (world instanceof ServerWorld sw) ? sw.getTime() : 0L;
        long readyAt = tag.getLong("gcd").orElse(0L);
        if (now < readyAt) {
            return;
        }

        // Server-side: resolve target world and position
        String dimStr = tag.getString("gd").orElse("");
        int gx = tag.getInt("gx").orElse(0);
        int gy = tag.getInt("gy").orElse(0);
        int gz = tag.getInt("gz").orElse(0);

        if (dimStr.isEmpty() || !(user instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }

        Identifier dimId = Identifier.of(dimStr);
        RegistryKey<World> targetKey = RegistryKey.of(RegistryKeys.WORLD, dimId);
        // Get server from the world the player is in
        ServerWorld currentWorld = (ServerWorld) world;
        ServerWorld targetWorld = currentWorld.getServer().getWorld(targetKey);
        if (targetWorld == null) {
            return;
        }

        BlockPos base = new BlockPos(gx, gy, gz);

        // Require a beacon block under the gateway; do not require beam state (chunks may be cold-loaded)
        if (!targetWorld.getBlockState(base.down()).isOf(Blocks.BEACON)) {
            // Consume the used compass unless in Creative mode
            if (!serverPlayer.isCreative()) {
                stack.decrement(1);
            }
            // Play beacon power down sound at the gateway
            targetWorld.playSound(null, base, SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.BLOCKS, 1.0f, 1.0f);
            return;
        }
        // Find a safe spot for the player (1x1x2 space) within 20 blocks of the gateway
        BlockPos teleportPos = findValidTeleportLocation(targetWorld, base, serverPlayer);
        
        if (teleportPos == null) {
            // No valid location found within 20 blocks - consume compass unless Creative, and play failure sound
            if (!serverPlayer.isCreative()) {
                stack.decrement(1);
            }
            targetWorld.playSound(null, base, SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.BLOCKS, 1.0f, 1.0f);
            return;
        }

        double x = teleportPos.getX() + 0.5;
        double y = teleportPos.getY();
        double z = teleportPos.getZ() + 0.5;

        // Teleport preserving yaw/pitch (1.21.8 signature with PositionFlag set and dismount=false)
        java.util.EnumSet<PositionFlag> flags = java.util.EnumSet.noneOf(PositionFlag.class);
        serverPlayer.teleport(targetWorld, x, y, z, flags, serverPlayer.getYaw(), serverPlayer.getPitch(), false);
        
        // Reset velocity and fall distance to prevent fall damage from previous momentum
        serverPlayer.setVelocity(0.0, 0.0, 0.0);
        serverPlayer.fallDistance = 0.0f;

        // Consume the used compass unless in Creative mode
        if (!serverPlayer.isCreative()) {
            stack.decrement(1);
        }

        // Play beacon power down sound at the gateway
        targetWorld.playSound(null, base, SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.BLOCKS, 1.0f, 1.0f);

        // Set player-based cooldown (20 ticks = 1 second) - this will show the visual overlay
        serverPlayer.getItemCooldownManager().set(Items.RECOVERY_COMPASS.getDefaultStack(), 20);

        // Do not cancel; allow vanilla return to proceed
    }

    /**
     * Finds a valid teleportation location for the player within 20 blocks of the gateway.
     * The location must have a 1x1x2 clear space for the player to fit.
     * 
     * @param world The target world
     * @param gatewayPos The position of the quantum gateway
     * @param player The player to teleport
     * @return A valid BlockPos for teleportation, or null if none found
     */
    private BlockPos findValidTeleportLocation(ServerWorld world, BlockPos gatewayPos, ServerPlayerEntity player) {
        // Search in expanding rings around the gateway, starting from directly above
        for (int radius = 0; radius <= 20; radius++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    // Skip if not on the current ring boundary
                    if (radius > 0 && Math.abs(x) < radius && Math.abs(z) < radius) {
                        continue;
                    }
                    
                    // Check positions from gateway level up to gateway + 20 blocks
                    for (int y = 0; y <= 20; y++) {
                        BlockPos testPos = gatewayPos.add(x, y, z);
                        
                        if (isValidPlayerLocation(world, testPos, player)) {
                            return testPos;
                        }
                    }
                }
            }
        }
        
        return null; // No valid location found
    }

    /**
     * Checks if a position is valid for player teleportation.
     * The player needs a 1x1x2 clear space (feet and head level must be clear).
     * 
     * @param world The world to check in
     * @param pos The position to check
     * @param player The player entity
     * @return true if the position is valid for teleportation
     */
    private boolean isValidPlayerLocation(ServerWorld world, BlockPos pos, ServerPlayerEntity player) {
        // Check if both feet level (pos) and head level (pos.up()) are clear
        // This ensures the player has the required 1x1x2 space
        Box playerBox = new Box(
            pos.getX() + 0.3, pos.getY(), pos.getZ() + 0.3,
            pos.getX() + 0.7, pos.getY() + 1.8, pos.getZ() + 0.7
        );
        
        return world.isSpaceEmpty(player, playerBox);
    }

    

    /**
     * Handles initial binding of Shadow Hunter's Tracker to a structure
     */
    private void handleShadowHuntersTrackerBinding(World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<net.minecraft.util.ActionResult> cir) {
        if (!(user instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }

        ItemStack stack = serverPlayer.getStackInHand(hand);
        
        // Try to locate a hollow shadow tree and bind to it
        BlockPos target = locateHollowTree((ServerWorld) world, serverPlayer.getBlockPos());
        if (target == null) {
            return;
        }

        // Bind to the hollow shadow tree
        net.minecraft.nbt.NbtCompound tag = new net.minecraft.nbt.NbtCompound();
        tag.putBoolean("shadow_hunter_tracker", true);
        tag.putInt("hollow_tree_x", target.getX());
        tag.putInt("hollow_tree_y", target.getY());
        tag.putInt("hollow_tree_z", target.getZ());
        tag.putString("world_dimension", ((ServerWorld) world).getRegistryKey().getValue().toString());
        tag.putBoolean("precise_mode", false); // Start in structure mode
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(tag));
        
    }

    /**
     * Handles toggle between structure mode and precise altar mode (shift+right-click)
     */
    private void handleShadowHuntersTrackerToggle(World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<net.minecraft.util.ActionResult> cir) {
        if (!(user instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }

        ItemStack stack = serverPlayer.getStackInHand(hand);
        NbtComponent custom = stack.get(DataComponentTypes.CUSTOM_DATA);
        var tag = custom.copyNbt();
        boolean currentPreciseMode = tag.contains("precise_mode") && tag.getBoolean("precise_mode").orElse(false);
        
        if (!currentPreciseMode) {
            // Try to switch to precise mode - search for the altar
            int structureX = tag.getInt("hollow_tree_x").orElse(0);
            int structureY = tag.getInt("hollow_tree_y").orElse(0);
            int structureZ = tag.getInt("hollow_tree_z").orElse(0);
            BlockPos structurePos = new BlockPos(structureX, structureY, structureZ);
            
            BlockPos altarPos = findAltarNear((ServerWorld) world, structurePos, serverPlayer.getBlockPos());
            if (altarPos != null) {
                // Found the altar! Switch to precise mode
                net.minecraft.nbt.NbtCompound newTag = tag.copy();
                newTag.putBoolean("precise_mode", true);
                newTag.putInt("altar_x", altarPos.getX());
                newTag.putInt("altar_y", altarPos.getY());
                newTag.putInt("altar_z", altarPos.getZ());
                stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(newTag));
                
            } else {
            }
        } else {
            // Switch back to structure mode
            net.minecraft.nbt.NbtCompound newTag = tag.copy();
            newTag.putBoolean("precise_mode", false);
            stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(newTag));
        }
    }

    /**
     * Shows current mode status (regular right-click)
     */
    private void handleShadowHuntersTrackerStatus(World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<net.minecraft.util.ActionResult> cir) {
        // Method stub - tracker status handled elsewhere
    }
    
    private static BlockPos findAltarNear(ServerWorld world, BlockPos structurePos, BlockPos playerPos) {
        // First search around the player position (most likely to be near the altar)
        for (int dx = -16; dx <= 16; dx++) {
            for (int dz = -16; dz <= 16; dz++) {
                for (int dy = -16; dy <= 16; dy++) {
                    BlockPos checkPos = playerPos.add(dx, dy, dz);
                    try {
                        if (world.getBlockState(checkPos).isOf(com.theendupdate.registry.ModBlocks.SHADOW_ALTAR)) {
                            com.theendupdate.TemplateMod.LOGGER.info("Found altar at {} (searched from player pos {})", checkPos, playerPos);
                            return checkPos;
                        }
                    } catch (Exception e) {
                        // Skip
                    }
                }
            }
        }
        
        // Then search around the structure position
        for (int dx = -32; dx <= 32; dx++) {
            for (int dz = -32; dz <= 32; dz++) {
                for (int dy = -32; dy <= 32; dy++) {
                    BlockPos checkPos = structurePos.add(dx, dy, dz);
                    try {
                        if (world.getBlockState(checkPos).isOf(com.theendupdate.registry.ModBlocks.SHADOW_ALTAR)) {
                            com.theendupdate.TemplateMod.LOGGER.info("Found altar at {} (searched from structure pos {})", checkPos, structurePos);
                            return checkPos;
                        }
                    } catch (Exception e) {
                        // Skip
                    }
                }
            }
        }
        
        com.theendupdate.TemplateMod.LOGGER.warn("No altar found. Player: {}, Structure: {}", playerPos, structurePos);
        return null;
    }

    /**
     * Locate the nearest hollow shadow tree structure
     */
    private static BlockPos locateHollowTree(ServerWorld world, BlockPos origin) {
        var registry = world.getRegistryManager().getOrThrow(RegistryKeys.STRUCTURE);
        net.minecraft.util.Identifier id = com.theendupdate.registry.ModStructures.SHADOW_HOLLOW_TREE_KEY.getValue();
        var entry = registry.getEntry(id).orElse(null);
        if (entry == null) return null;

        com.mojang.datafixers.util.Pair<BlockPos, RegistryEntry<net.minecraft.world.gen.structure.Structure>> pair = world.getChunkManager().getChunkGenerator().locateStructure(
            world,
            net.minecraft.registry.entry.RegistryEntryList.of(entry),
            origin,
            512, // Search radius in chunks
            false
        );
        return pair == null ? null : pair.getFirst();
    }
}


