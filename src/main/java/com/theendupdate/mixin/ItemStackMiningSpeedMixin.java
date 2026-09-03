package com.theendupdate.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.state.BlockState;

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
        method = "getDestroySpeed(Lnet/minecraft/world/level/block/state/BlockState;)F",
        at = @At("RETURN"),
        cancellable = true,
        require = 1
    )
    private void theendupdate$applyChoppingToMiningSpeed(BlockState state, CallbackInfoReturnable<Float> cir) {
        ItemStack self = (ItemStack) (Object) this;
        if (self.isEmpty()) return;
        if (!self.is(ItemTags.AXES)) return;
        if (state == null || !state.is(BlockTags.MINEABLE_WITH_AXE)) return;

        ItemEnchantments ench = self.get(DataComponents.ENCHANTMENTS);
        if (ench == null) return;
        String enchStr = ench.toString();
        if (!enchStr.contains("theendupdate:chopping")) return;

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
        if (self.is(Items.NETHERITE_AXE) && effLevel >= 5 && (state.is(BlockTags.LOGS) || state.is(BlockTags.PLANKS))) {
            cir.setReturnValue(Math.max(original * NET5_CHOPPING_TOOL_MULTIPLIER, 128.0f));
            return;
        }

        float multiplier = BASE_CHOPPING_TOOL_MULTIPLIER;
        if (self.is(Items.NETHERITE_AXE) && effLevel >= 5) {
            multiplier = NET5_CHOPPING_TOOL_MULTIPLIER;
        }

        cir.setReturnValue(original * multiplier);
    }
}


