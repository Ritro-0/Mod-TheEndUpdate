package com.theendupdate.item;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

public class IceCreamConeItem extends Item {
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
		ItemStack cone = new ItemStack(com.theendupdate.registry.ModItems.WOODEN_CONE);
		NbtCompound tag = new NbtCompound();
		long blockUntil = world.getTime() + 3L; // ~0.15s at 20 tps
		tag.putLong("theendupdate_refill_blocked_until", blockUntil);
		cone.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(tag));
		if (result.isEmpty()) {
			return cone;
		} else {
			if (!player.getInventory().insertStack(cone)) {
				player.dropItem(cone, false);
			}
			return result;
		}
	}
}


