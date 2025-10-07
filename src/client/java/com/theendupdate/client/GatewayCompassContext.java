package com.theendupdate.client;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

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
        if (stack == null || stack.isEmpty() || !stack.isOf(Items.RECOVERY_COMPASS)) return false;
        NbtComponent custom = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (custom == null) return false;
        var tag = custom.copyNbt();
        return tag.contains("gx") && tag.contains("gy") && tag.contains("gz") && tag.contains("gd");
    }
}
