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
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
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
        BlockPos.Mutable dest = base.up().mutableCopy();

        // Require an active beacon under the gateway; otherwise consume and play sound, but do not teleport
        if (!targetWorld.getBlockState(base.down()).isOf(Blocks.BEACON)) {
            // Consume the used compass
            stack.decrement(1);
            // Play beacon power down sound at the gateway
            targetWorld.playSound(null, base, SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.BLOCKS, 1.0f, 1.0f);
            return;
        }
        // Find a safe spot up to 2 blocks above the gateway
        for (int i = 0; i < 3; i++) {
            if (targetWorld.isAir(dest) && targetWorld.isAir(dest.up())) break;
            dest.move(0, 1, 0);
        }

        double x = dest.getX() + 0.5;
        double y = dest.getY();
        double z = dest.getZ() + 0.5;

        // Teleport preserving yaw/pitch (1.21.8 signature with PositionFlag set and dismount=false)
        java.util.EnumSet<PositionFlag> flags = java.util.EnumSet.noneOf(PositionFlag.class);
        serverPlayer.teleport(targetWorld, x, y, z, flags, serverPlayer.getYaw(), serverPlayer.getPitch(), false);

        // Consume the used compass
        stack.decrement(1);

        // Play beacon power down sound at the gateway
        targetWorld.playSound(null, base, SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.BLOCKS, 1.0f, 1.0f);

        // Cooldown: 20 ticks stored per-stack in CUSTOM_DATA; use target world's time for consistency with client overlay
        long cooldownTicks = 20L;
        long targetNow = targetWorld.getTime();
        tag.putLong("gcd", targetNow + cooldownTicks);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(tag));

        // Do not cancel; allow vanilla return to proceed
    }
}


