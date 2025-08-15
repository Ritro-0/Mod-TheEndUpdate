package com.theendupdate.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class BoundRecoveryCompassItem extends Item {
    public BoundRecoveryCompassItem(Settings settings) { super(settings); }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }
}


