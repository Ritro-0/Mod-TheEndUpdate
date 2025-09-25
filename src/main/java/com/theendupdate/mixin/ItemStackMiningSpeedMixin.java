package com.theendupdate.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Apply a modest tool-level speed multiplier for Chopping on axe-mineable blocks,
 * with a slightly stronger boost for Efficiency V netherite axes so they cross the
 * insta-mine threshold on wood.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMiningSpeedMixin {

    private static final float BASE_CHOPPING_TOOL_MULTIPLIER = 1.20f; // modest baseline
    private static final float NET5_CHOPPING_TOOL_MULTIPLIER = 1.45f; // stronger for netherite + Eff V
    private static final Pattern EFF_LEVEL_PATTERN = Pattern.compile("minecraft:efficiency\\D*(\\d+)");

    @Inject(
        method = "getMiningSpeedMultiplier(Lnet/minecraft/block/BlockState;)F",
        at = @At("RETURN"),
        cancellable = true,
        require = 1
    )
    private void theendupdate$applyChoppingToMiningSpeed(BlockState state, CallbackInfoReturnable<Float> cir) {
        ItemStack self = (ItemStack) (Object) this;
        if (self.isEmpty()) return;
        if (!self.isIn(ItemTags.AXES)) return;
        if (state == null || !state.isIn(BlockTags.AXE_MINEABLE)) return;

        ItemEnchantmentsComponent ench = self.get(DataComponentTypes.ENCHANTMENTS);
        if (ench == null) return;
        String enchStr = ench.toString();
        if (!enchStr.contains("theendupdate:chopping")) return;

        // Determine efficiency level if present
        int effLevel = 0;
        try {
            Matcher m = EFF_LEVEL_PATTERN.matcher(enchStr);
            if (m.find()) {
                effLevel = Integer.parseInt(m.group(1));
            }
        } catch (Throwable ignored) {}

        float original = cir.getReturnValueF();
        if (original <= 0.0f) return;

        // Guarantee insta-mine on standard wooden blocks for netherite + Efficiency V
        if (self.isOf(Items.NETHERITE_AXE) && effLevel >= 5 && (state.isIn(BlockTags.LOGS) || state.isIn(BlockTags.PLANKS))) {
            cir.setReturnValue(Math.max(original * NET5_CHOPPING_TOOL_MULTIPLIER, 128.0f));
            return;
        }

        float multiplier = BASE_CHOPPING_TOOL_MULTIPLIER;
        if (self.isOf(Items.NETHERITE_AXE) && effLevel >= 5) {
            multiplier = NET5_CHOPPING_TOOL_MULTIPLIER;
        }

        cir.setReturnValue(original * multiplier);
    }
}


