package com.theendupdate.item;

import com.theendupdate.accessor.CowEntityAnimationAccessor;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class WoodenConeItem extends Item {
    private static final Map<UUID, Long> LAST_USED_TIMES = new HashMap<>();
    
    public WoodenConeItem(Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity entity, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;

        boolean isMooshroom = entity instanceof MushroomCow;
        boolean isCow = entity instanceof Cow;
        
        if (!isCow && !isMooshroom) return InteractionResult.PASS;
        if (entity.isBaby()) return InteractionResult.PASS;
        
        Level world = user.level();
        if (world.isClientSide()) return InteractionResult.SUCCESS;

        // avoid same-frame refill right after eating either ice cream type
        if (IceCreamConeItem.isOnCooldown(user, world) || StrawberryIceCreamConeItem.isOnCooldown(user, world)) {
            return InteractionResult.PASS;
        }

        UUID playerUuid = user.getUUID();
        Long lastUsed = LAST_USED_TIMES.get(playerUuid);
        if (lastUsed != null && (world.getGameTime() - lastUsed) < 10L) { // 0.5s cooldown, blocks rapid-fire right-click hold
            return InteractionResult.PASS;
        }

        LAST_USED_TIMES.put(playerUuid, world.getGameTime());

        boolean creative = user.getAbilities().instabuild;

        ItemStack iceCream = new ItemStack(
            isMooshroom ? com.theendupdate.registry.ModItems.STRAWBERRY_ICE_CREAM_CONE 
                        : com.theendupdate.registry.ModItems.ICE_CREAM_CONE
        );
        
        if (!creative) {
            if (stack.getCount() == 1) {
                user.setItemInHand(hand, iceCream);
            } else {
                stack.shrink(1);
                if (!user.getInventory().add(iceCream)) {
                    user.drop(iceCream, false);
                }
            }
        } else {
            if (!user.getInventory().add(iceCream)) {
                user.drop(iceCream, false);
            }
        }

        if (entity instanceof CowEntityAnimationAccessor accessor) {
            accessor.theendupdate$setAnimationStartTime(world.getGameTime());
        }
        
        try { user.playSound(net.minecraft.sounds.SoundEvents.COW_MILK, 1.0f, 1.0f); } catch (Throwable ignored) {}
        return InteractionResult.CONSUME;
    }
}

