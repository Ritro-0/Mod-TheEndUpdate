package com.theendupdate.item;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class WoodenConeItem extends Item {
    public WoodenConeItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        if (hand != Hand.MAIN_HAND) return ActionResult.PASS;
        if (!(entity instanceof CowEntity cow)) return ActionResult.PASS;
        if (cow.isBaby()) return ActionResult.PASS;
        World world = user.getWorld();
        if (world.isClient) return ActionResult.PASS;

        // Prevent same-frame refill immediately after eating
        NbtComponent custom = stack.get(DataComponentTypes.CUSTOM_DATA);
        long blockedUntil = 0L;
        if (custom != null) {
            NbtCompound tag = custom.copyNbt();
            blockedUntil = tag.getLong("theendupdate_refill_blocked_until").orElse(0L);
        }
        if ((world.getTime()) < blockedUntil) {
            return ActionResult.PASS;
        }

        // Prevent rapid-fire consumption during right-click hold
        long lastUsed = custom != null ? custom.copyNbt().getLong("theendupdate_last_used").orElse(0L) : 0L;
        if ((world.getTime() - lastUsed) < 10L) { // 0.5 second cooldown
            return ActionResult.PASS;
        }

        // Set the last used timestamp to prevent rapid-fire usage
        NbtCompound newTag = new NbtCompound();
        if (custom != null) {
            newTag = custom.copyNbt();
        }
        newTag.putLong("theendupdate_last_used", world.getTime());
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(newTag));

        boolean creative = user.getAbilities().creativeMode;
        ItemStack iceCream = new ItemStack(com.theendupdate.registry.ModItems.ICE_CREAM_CONE);
        if (!creative) {
            if (stack.getCount() == 1) {
                user.setStackInHand(hand, iceCream);
            } else {
                stack.decrement(1);
                if (!user.getInventory().insertStack(iceCream)) {
                    user.dropItem(iceCream, false);
                }
            }
        } else {
            if (!user.getInventory().insertStack(iceCream)) {
                user.dropItem(iceCream, false);
            }
        }
        try { user.playSound(net.minecraft.sound.SoundEvents.ENTITY_COW_MILK, 1.0f, 1.0f); } catch (Throwable ignored) {}
        return ActionResult.CONSUME;
    }
}


