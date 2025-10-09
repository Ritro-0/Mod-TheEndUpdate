package com.theendupdate.item;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class IceCreamConeItem extends Item {
	// Track refill cooldowns per player to avoid adding NBT data to cones
	private static final Map<UUID, Long> REFILL_COOLDOWNS = new HashMap<>();
	
	public IceCreamConeItem(Settings settings) {
		super(settings);
	}

	@Override
	public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
		ItemStack result = super.finishUsing(stack, world, user);
		if (!(user instanceof PlayerEntity player)) {
			return result;
		}
        // Apply effects even if not hungry (handled by alwaysEdible food component)
        try {
            int duration = 30 * 20;
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, duration, 1));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, duration, 1));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, duration, 1));
        } catch (Throwable ignored) {}
		if (player.getAbilities().creativeMode) {
			return result;
		}
		// Return a wooden cone as the remainder, with a brief anti-refill cooldown
		// Store cooldown per player instead of on the item to allow proper stacking
		long blockUntil = world.getTime() + 3L; // ~0.15s at 20 tps
		REFILL_COOLDOWNS.put(player.getUuid(), blockUntil);
		
		ItemStack cone = new ItemStack(com.theendupdate.registry.ModItems.WOODEN_CONE);
		if (result.isEmpty()) {
			return cone;
		} else {
			if (!player.getInventory().insertStack(cone)) {
				player.dropItem(cone, false);
			}
			return result;
		}
	}
	
	// Public method to check if a player is on cooldown
	public static boolean isOnCooldown(PlayerEntity player, World world) {
		Long blockedUntil = REFILL_COOLDOWNS.get(player.getUuid());
		if (blockedUntil == null) {
			return false;
		}
		if (world.getTime() >= blockedUntil) {
			REFILL_COOLDOWNS.remove(player.getUuid());
			return false;
		}
		return true;
	}
}

