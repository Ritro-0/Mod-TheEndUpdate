package com.theendupdate.item;

import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;

/**
 * Custom filled map item that preserves the original when used in duplication recipes.
 * This is used to replace filled_map in the recipe so we can control the remainder behavior.
 */
public class ShadowHuntersFilledMapItem extends FilledMapItem {
    public ShadowHuntersFilledMapItem(Settings settings) {
        super(settings);
    }

    @Override
    public ItemStack getRecipeRemainder(ItemStack stack) {
        // Keep the original filled map in the crafting grid
        if (stack == null || stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack copy = stack.copy();
        copy.setCount(1);
        return copy;
    }
}
