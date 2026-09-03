package com.theendupdate.item;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class StrawberryIceCreamConeItem extends Item {
	// per-player rather than on the item, avoids adding NBT data to cones
	private static final Map<UUID, Long> REFILL_COOLDOWNS = new HashMap<>();
	
	public StrawberryIceCreamConeItem(Properties settings) {
		super(settings);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity user) {
		ItemStack result = super.finishUsingItem(stack, world, user);
		if (!(user instanceof Player player)) {
			return result;
		}
        // applies even if not hungry, alwaysEdible food component handles that
        try {
            int duration = 30 * 20;
            player.addEffect(new MobEffectInstance(MobEffects.SPEED, duration, 1));
            player.addEffect(new MobEffectInstance(MobEffects.HASTE, duration, 1));
            player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, duration, 1));
        } catch (Throwable ignored) {}
		if (player.getAbilities().instabuild) {
			return result;
		}
		long blockUntil = world.getGameTime() + 3L; // brief anti-refill cooldown, ~0.15s at 20 tps
		REFILL_COOLDOWNS.put(player.getUUID(), blockUntil);
		
		ItemStack cone = new ItemStack(com.theendupdate.registry.ModItems.WOODEN_CONE);
		if (result.isEmpty()) {
			return cone;
		} else {
			if (!player.getInventory().add(cone)) {
				player.drop(cone, false);
			}
			return result;
		}
	}

	public static boolean isOnCooldown(Player player, Level world) {
		Long blockedUntil = REFILL_COOLDOWNS.get(player.getUUID());
		if (blockedUntil == null) {
			return false;
		}
		if (world.getGameTime() >= blockedUntil) {
			REFILL_COOLDOWNS.remove(player.getUUID());
			return false;
		}
		return true;
	}
}

