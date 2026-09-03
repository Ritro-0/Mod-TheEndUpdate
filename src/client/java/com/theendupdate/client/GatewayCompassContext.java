package com.theendupdate.client;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class GatewayCompassContext {
    private static final ThreadLocal<ItemStack> CURRENT_STACK = new ThreadLocal<>();

    private GatewayCompassContext() {}

    public static void set(ItemStack stack) {
        CURRENT_STACK.set(stack);
    }

    public static void clear() {
        CURRENT_STACK.remove();
    }

    public static ItemStack get() {
        return CURRENT_STACK.get();
    }

    public static boolean isTaggedGatewayCompass(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.is(Items.RECOVERY_COMPASS)) return false;
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        if (custom == null) return false;
        var tag = custom.copyTag();
        return tag.contains("gx") && tag.contains("gy") && tag.contains("gz") && tag.contains("gd");
    }

    public static boolean isShadowHuntersTracker(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.is(Items.RECOVERY_COMPASS)) return false;
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        if (custom == null) return false;
        var tag = custom.copyTag();
        return tag.contains("shadow_hunter_tracker") && tag.getBoolean("shadow_hunter_tracker").orElse(false);
    }
}
