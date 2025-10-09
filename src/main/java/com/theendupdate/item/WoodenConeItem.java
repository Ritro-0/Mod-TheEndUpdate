package com.theendupdate.item;

import com.theendupdate.accessor.CowEntityAnimationAccessor;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.passive.MooshroomEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WoodenConeItem extends Item {
    // Track last usage per player to prevent rapid-fire usage
    private static final Map<UUID, Long> LAST_USED_TIMES = new HashMap<>();
    
    public WoodenConeItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        if (hand != Hand.MAIN_HAND) return ActionResult.PASS;
        
        // Check for both CowEntity and MooshroomEntity explicitly
        boolean isMooshroom = entity instanceof MooshroomEntity;
        boolean isCow = entity instanceof CowEntity;
        
        if (!isCow && !isMooshroom) return ActionResult.PASS;
        if (entity.isBaby()) return ActionResult.PASS;
        
        World world = user.getWorld();
        if (world.isClient) return ActionResult.SUCCESS;

        // Prevent same-frame refill immediately after eating - check both ice cream types
        if (IceCreamConeItem.isOnCooldown(user, world) || StrawberryIceCreamConeItem.isOnCooldown(user, world)) {
            return ActionResult.PASS;
        }

        // Prevent rapid-fire consumption during right-click hold
        UUID playerUuid = user.getUuid();
        Long lastUsed = LAST_USED_TIMES.get(playerUuid);
        if (lastUsed != null && (world.getTime() - lastUsed) < 10L) { // 0.5 second cooldown
            return ActionResult.PASS;
        }

        // Set the last used timestamp to prevent rapid-fire usage
        LAST_USED_TIMES.put(playerUuid, world.getTime());

        boolean creative = user.getAbilities().creativeMode;
        
        // Determine which ice cream to give based on entity type
        ItemStack iceCream = new ItemStack(
            isMooshroom ? com.theendupdate.registry.ModItems.STRAWBERRY_ICE_CREAM_CONE 
                        : com.theendupdate.registry.ModItems.ICE_CREAM_CONE
        );
        
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
        
        // Mark the cow/mooshroom for animation
        if (entity instanceof CowEntityAnimationAccessor accessor) {
            accessor.theendupdate$setAnimationStartTime(world.getTime());
        }
        
        try { user.playSound(net.minecraft.sound.SoundEvents.ENTITY_COW_MILK, 1.0f, 1.0f); } catch (Throwable ignored) {}
        return ActionResult.CONSUME;
    }
}

