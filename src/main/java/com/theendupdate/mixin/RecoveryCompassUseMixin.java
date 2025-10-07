package com.theendupdate.mixin;

import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BeaconBlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
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
    private void theendupdate$teleportOnSneakUse(World world, PlayerEntity user, Hand hand, CallbackInfoReturnable cir) {
        ItemStack stack = user.getStackInHand(hand);
        if (!user.isSneaking()) return;
        if (stack == null || stack.isEmpty() || !stack.isOf(Items.RECOVERY_COMPASS)) return;

        NbtComponent custom = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (custom == null) return;
        var tag = custom.copyNbt();
        if (!(tag.contains("gx") && tag.contains("gy") && tag.contains("gz") && tag.contains("gd"))) return;

        // Client: short-circuit to success to avoid further processing; server performs the teleport
        if (world.isClient) {
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
        ServerWorld targetWorld = serverPlayer.getServer().getWorld(targetKey);
        if (targetWorld == null) {
            return;
        }

        BlockPos base = new BlockPos(gx, gy, gz);

        // Require an active beacon with beam under the gateway; otherwise consume and play sound, but do not teleport
        if (!targetWorld.getBlockState(base.down()).isOf(Blocks.BEACON) || !isBeaconBeamActive(targetWorld, base.down())) {
            // Consume the used compass
            stack.decrement(1);
            // Play beacon power down sound at the gateway
            targetWorld.playSound(null, base, SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.BLOCKS, 1.0f, 1.0f);
            return;
        }
        // Find a safe spot for the player (1x1x2 space) within 20 blocks of the gateway
        BlockPos teleportPos = findValidTeleportLocation(targetWorld, base, serverPlayer);
        
        if (teleportPos == null) {
            // No valid location found within 20 blocks - consume compass and play failure sound
            stack.decrement(1);
            targetWorld.playSound(null, base, SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.BLOCKS, 1.0f, 1.0f);
            return;
        }

        double x = teleportPos.getX() + 0.5;
        double y = teleportPos.getY();
        double z = teleportPos.getZ() + 0.5;

        // Teleport preserving yaw/pitch (1.21.8 signature with PositionFlag set and dismount=false)
        java.util.EnumSet<PositionFlag> flags = java.util.EnumSet.noneOf(PositionFlag.class);
        serverPlayer.teleport(targetWorld, x, y, z, flags, serverPlayer.getYaw(), serverPlayer.getPitch(), false);
        
        // Reset velocity to prevent fall damage from previous momentum
        serverPlayer.setVelocity(0.0, 0.0, 0.0);

        // Consume the used compass
        stack.decrement(1);

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
     * Checks if a beacon at the given position has an active beam.
     * A beacon beam is considered active if it has beam segments and is not blocked.
     * 
     * @param world The world to check in
     * @param beaconPos The position of the beacon
     * @return true if the beacon has an active beam, false otherwise
     */
    private boolean isBeaconBeamActive(ServerWorld world, BlockPos beaconPos) {
        if (!world.getBlockState(beaconPos).isOf(Blocks.BEACON)) {
            return false;
        }
        
        var blockEntity = world.getBlockEntity(beaconPos);
        if (!(blockEntity instanceof BeaconBlockEntity beacon)) {
            return false;
        }
        
        try {
            var segments = beacon.getBeamSegments();
            return segments != null && !segments.isEmpty();
        } catch (Throwable ignored) {
            return false;
        }
    }
}


