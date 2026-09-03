package com.theendupdate.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public abstract class RecoveryCompassUseMixin {

    @Inject(method = "use", at = @At("HEAD"))
    private void theendupdate$teleportOnSneakUse(Level world, Player user, InteractionHand hand, CallbackInfoReturnable<net.minecraft.world.InteractionResult> cir) {
        ItemStack stack = user.getItemInHand(hand);
        if (stack == null || stack.isEmpty()) return;

        // shadow hunter's tracker: same compass item, keyed off custom data
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        if (custom != null) {
            var tag = custom.copyTag();
            if (tag.contains("shadow_hunter_tracker") && tag.getBoolean("shadow_hunter_tracker").orElse(false)) {
                // works like the quantum gateway compass below but never teleports
                // client short-circuits to success here, server handles the actual binding
                if (world.isClientSide()) {
                    return;
                }

                boolean needsBinding = !(tag.contains("hollow_tree_x") && tag.contains("hollow_tree_y") && tag.contains("hollow_tree_z"));
                if (needsBinding) {
                    handleShadowHuntersTrackerBinding(world, user, hand, cir);
                    return;
                } else if (user.isShiftKeyDown()) {
                    handleShadowHuntersTrackerToggle(world, user, hand, cir);
                    return;
                } else {
                    handleShadowHuntersTrackerStatus(world, user, hand, cir);
                    return;
                }
            }
        }

        // quantum gateway compasses only trigger on sneak-use
        if (!user.isShiftKeyDown()) return;
        if (!stack.is(Items.RECOVERY_COMPASS)) return;

        custom = stack.get(DataComponents.CUSTOM_DATA);
        if (custom == null) return;
        var tag = custom.copyTag();
        if (!(tag.contains("gx") && tag.contains("gy") && tag.contains("gz") && tag.contains("gd"))) return;

        // client short-circuits to success, server performs the actual teleport
        if (world.isClientSide()) {
            return;
        }

        // per-stack cooldown for tagged compasses
        long now = (world instanceof ServerLevel sw) ? sw.getGameTime() : 0L;
        long readyAt = tag.getLong("gcd").orElse(0L);
        if (now < readyAt) {
            return;
        }

        String dimStr = tag.getString("gd").orElse("");
        int gx = tag.getInt("gx").orElse(0);
        int gy = tag.getInt("gy").orElse(0);
        int gz = tag.getInt("gz").orElse(0);

        if (dimStr.isEmpty() || !(user instanceof ServerPlayer serverPlayer)) {
            return;
        }

        Identifier dimId = Identifier.parse(dimStr);
        ResourceKey<Level> targetKey = ResourceKey.create(Registries.DIMENSION, dimId);
        ServerLevel currentWorld = (ServerLevel) world;
        ServerLevel targetWorld = currentWorld.getServer().getLevel(targetKey);
        if (targetWorld == null) {
            return;
        }

        BlockPos base = new BlockPos(gx, gy, gz);

        // only require the beacon block itself, not beam state - chunks may be cold-loaded
        if (!targetWorld.getBlockState(base.below()).is(Blocks.BEACON)) {
            if (!serverPlayer.isCreative()) {
                stack.shrink(1);
            }
            targetWorld.playSound(null, base, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.0f, 1.0f);
            return;
        }
        BlockPos teleportPos = findValidTeleportLocation(targetWorld, base, serverPlayer);

        if (teleportPos == null) {
            if (!serverPlayer.isCreative()) {
                stack.shrink(1);
            }
            targetWorld.playSound(null, base, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.0f, 1.0f);
            return;
        }

        double x = teleportPos.getX() + 0.5;
        double y = teleportPos.getY();
        double z = teleportPos.getZ() + 0.5;

        // 1.21.8 teleportTo wants an EnumSet<Relative> for the flags, not the old bitmask
        java.util.EnumSet<Relative> flags = java.util.EnumSet.noneOf(Relative.class);
        serverPlayer.teleportTo(targetWorld, x, y, z, flags, serverPlayer.getYRot(), serverPlayer.getXRot(), false);

        // clear momentum so leftover velocity doesn't cause fall damage on arrival
        serverPlayer.setDeltaMovement(0.0, 0.0, 0.0);
        serverPlayer.fallDistance = 0.0f;

        if (!serverPlayer.isCreative()) {
            stack.shrink(1);
        }

        targetWorld.playSound(null, base, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.0f, 1.0f);

        // cooldown is player-based (not item-based) so it drives the vanilla cooldown overlay
        serverPlayer.getCooldowns().addCooldown(Items.RECOVERY_COMPASS.getDefaultInstance(), 20);

        // don't cancel, let vanilla's own return proceed after ours
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
    private BlockPos findValidTeleportLocation(ServerLevel world, BlockPos gatewayPos, ServerPlayer player) {
        // expanding rings outward from the gateway
        for (int radius = 0; radius <= 20; radius++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    // only the ring boundary, interior already covered by smaller radii
                    if (radius > 0 && Math.abs(x) < radius && Math.abs(z) < radius) {
                        continue;
                    }

                    for (int y = 0; y <= 20; y++) {
                        BlockPos testPos = gatewayPos.offset(x, y, z);

                        if (isValidPlayerLocation(world, testPos, player)) {
                            return testPos;
                        }
                    }
                }
            }
        }

        return null;
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
    private boolean isValidPlayerLocation(ServerLevel world, BlockPos pos, ServerPlayer player) {
        AABB playerBox = new AABB(
            pos.getX() + 0.3, pos.getY(), pos.getZ() + 0.3,
            pos.getX() + 0.7, pos.getY() + 1.8, pos.getZ() + 0.7
        );
        
        return world.noCollision(player, playerBox);
    }

    

    /**
     * Handles initial binding of Shadow Hunter's Tracker to a structure
     */
    private void handleShadowHuntersTrackerBinding(Level world, Player user, InteractionHand hand, CallbackInfoReturnable<net.minecraft.world.InteractionResult> cir) {
        if (!(user instanceof ServerPlayer serverPlayer)) {
            return;
        }

        ItemStack stack = serverPlayer.getItemInHand(hand);
        
        BlockPos target = com.theendupdate.world.HollowTreeLocator.locate(
            (ServerLevel) world,
            serverPlayer.blockPosition()
        );
        if (target == null) {
            serverPlayer.sendOverlayMessage(
                net.minecraft.network.chat.Component.translatable("item.theendupdate.shadow_hunters_tracker.no_tree")
            );
            return;
        }

        CustomData existing = stack.get(DataComponents.CUSTOM_DATA);
        net.minecraft.nbt.CompoundTag tag = existing != null ? existing.copyTag() : new net.minecraft.nbt.CompoundTag();
        tag.putBoolean("shadow_hunter_tracker", true);
        tag.putInt("hollow_tree_x", target.getX());
        tag.putInt("hollow_tree_y", target.getY());
        tag.putInt("hollow_tree_z", target.getZ());
        tag.putString("world_dimension", ((ServerLevel) world).dimension().identifier().toString());
        tag.putBoolean("precise_mode", false);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

        serverPlayer.sendOverlayMessage(
            net.minecraft.network.chat.Component.translatable(
                "item.theendupdate.shadow_hunters_tracker.found_tree",
                target.getX(),
                target.getY(),
                target.getZ()
            )
        );
    }

    /**
     * Handles toggle between structure mode and precise altar mode (shift+right-click)
     */
    private void handleShadowHuntersTrackerToggle(Level world, Player user, InteractionHand hand, CallbackInfoReturnable<net.minecraft.world.InteractionResult> cir) {
        if (!(user instanceof ServerPlayer serverPlayer)) {
            return;
        }

        ItemStack stack = serverPlayer.getItemInHand(hand);
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        var tag = custom.copyTag();
        boolean currentPreciseMode = tag.contains("precise_mode") && tag.getBoolean("precise_mode").orElse(false);
        
        if (!currentPreciseMode) {
            // search for a nearby altar to switch into precise mode
            int structureX = tag.getInt("hollow_tree_x").orElse(0);
            int structureY = tag.getInt("hollow_tree_y").orElse(0);
            int structureZ = tag.getInt("hollow_tree_z").orElse(0);
            BlockPos structurePos = new BlockPos(structureX, structureY, structureZ);

            BlockPos altarPos = findAltarNear((ServerLevel) world, structurePos, serverPlayer.blockPosition());
            if (altarPos != null) {
                net.minecraft.nbt.CompoundTag newTag = tag.copy();
                newTag.putBoolean("precise_mode", true);
                newTag.putInt("altar_x", altarPos.getX());
                newTag.putInt("altar_y", altarPos.getY());
                newTag.putInt("altar_z", altarPos.getZ());
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(newTag));
            }
        } else {
            // back to structure mode
            net.minecraft.nbt.CompoundTag newTag = tag.copy();
            newTag.putBoolean("precise_mode", false);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(newTag));
        }
    }

    /**
     * Shows current mode status (regular right-click)
     */
    private void handleShadowHuntersTrackerStatus(Level world, Player user, InteractionHand hand, CallbackInfoReturnable<net.minecraft.world.InteractionResult> cir) {
        // stub, status display handled elsewhere
    }
    
    private static BlockPos findAltarNear(ServerLevel world, BlockPos structurePos, BlockPos playerPos) {
        // player position first, most likely to be near the altar
        for (int dx = -16; dx <= 16; dx++) {
            for (int dz = -16; dz <= 16; dz++) {
                for (int dy = -16; dy <= 16; dy++) {
                    BlockPos checkPos = playerPos.offset(dx, dy, dz);
                    try {
                        if (world.getBlockState(checkPos).is(com.theendupdate.registry.ModBlocks.SHADOW_ALTAR)) {
                            if (com.theendupdate.TheEndUpdate.DEBUG_MODE) {
                                com.theendupdate.TheEndUpdate.LOGGER.info("Found altar at {} (searched from player pos {})", checkPos, playerPos);
                            }
                            return checkPos;
                        }
                    } catch (Exception e) {
                    }
                }
            }
        }

        // fall back to a wider search around the structure
        for (int dx = -32; dx <= 32; dx++) {
            for (int dz = -32; dz <= 32; dz++) {
                for (int dy = -32; dy <= 32; dy++) {
                    BlockPos checkPos = structurePos.offset(dx, dy, dz);
                    try {
                        if (world.getBlockState(checkPos).is(com.theendupdate.registry.ModBlocks.SHADOW_ALTAR)) {
                            if (com.theendupdate.TheEndUpdate.DEBUG_MODE) {
                                com.theendupdate.TheEndUpdate.LOGGER.info("Found altar at {} (searched from structure pos {})", checkPos, structurePos);
                            }
                            return checkPos;
                        }
                    } catch (Exception e) {
                    }
                }
            }
        }
        
        com.theendupdate.TheEndUpdate.LOGGER.warn("No altar found. Player: {}, Structure: {}", playerPos, structurePos);
        return null;
    }

}


