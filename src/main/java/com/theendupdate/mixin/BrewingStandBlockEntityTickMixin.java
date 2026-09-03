package com.theendupdate.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BrewingStandBlockEntity.class)
public class BrewingStandBlockEntityTickMixin {
    private static final Identifier ENDER_CHRYS = Identifier.fromNamespaceAndPath("theendupdate", "ender_chrysanthemum");
    private static final Identifier CLOSED_ENDER_CHRYS = Identifier.fromNamespaceAndPath("theendupdate", "closed_ender_chrysanthemum");
    private static final Identifier KING_PHANTOM_ESSENCE = Identifier.fromNamespaceAndPath("theendupdate", "king_phantom_essence");

    @Shadow
    private static boolean isBrewable(PotionBrewing recipes, NonNullList<ItemStack> slots) { return false; }

    @Shadow
    private static void doBrew(Level world, BlockPos pos, NonNullList<ItemStack> slots) {}

    private static NonNullList<ItemStack> theendupdate$effectiveSlots(NonNullList<ItemStack> slots) {
        try {
            ItemStack reagent = slots.get(3);
            if (!reagent.isEmpty()) {
                NonNullList<ItemStack> copy = NonNullList.withSize(slots.size(), ItemStack.EMPTY);
                for (int i = 0; i < slots.size(); i++) copy.set(i, slots.get(i));
                
                // neither has a vanilla recipe, sub a placeholder vanilla item so isBrewable recognizes it
                if (reagent.is(BuiltInRegistries.ITEM.getValue(ENDER_CHRYS)) || reagent.is(BuiltInRegistries.ITEM.getValue(CLOSED_ENDER_CHRYS))) {
                    copy.set(3, new ItemStack(Items.COBWEB));
                    return copy;
                }
                else if (reagent.is(BuiltInRegistries.ITEM.getValue(KING_PHANTOM_ESSENCE))) {
                    copy.set(3, new ItemStack(Items.SLIME_BALL));
                    return copy;
                }
            }
        } catch (Throwable ignored) {}
        return slots;
    }

    @Redirect(method = "serverTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/BrewingStandBlockEntity;isBrewable(Lnet/minecraft/world/item/alchemy/PotionBrewing;Lnet/minecraft/core/NonNullList;)Z"))
    private static boolean theendupdate$redirectCanCraft(PotionBrewing recipes, NonNullList<ItemStack> slots, Level world, BlockPos pos, BlockState state, BrewingStandBlockEntity self) {
        return isBrewable(recipes, theendupdate$effectiveSlots(slots));
    }

    @Redirect(method = "serverTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/BrewingStandBlockEntity;doBrew(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/NonNullList;)V"))
    private static void theendupdate$redirectCraft(Level world, BlockPos pos, NonNullList<ItemStack> slots, Level world2, BlockPos pos2, BlockState state, BrewingStandBlockEntity self) {
        // substitute reagent in-place (not a copy) so outputs write back to the real slots
        boolean substituted = false;
        ItemStack savedReagent = ItemStack.EMPTY;
        int vanillaInit = 0;
        net.minecraft.world.item.Item substituteItem = null;
        
        try {
            ItemStack top = slots.get(3);
            if (!top.isEmpty()) {
                if (top.is(BuiltInRegistries.ITEM.getValue(ENDER_CHRYS)) || top.is(BuiltInRegistries.ITEM.getValue(CLOSED_ENDER_CHRYS))) {
                    savedReagent = top.copy();
                    substituteItem = Items.COBWEB;
                    vanillaInit = 1;
                    slots.set(3, new ItemStack(substituteItem, vanillaInit));
                    substituted = true;
                }
                else if (top.is(BuiltInRegistries.ITEM.getValue(KING_PHANTOM_ESSENCE))) {
                    savedReagent = top.copy();
                    substituteItem = Items.SLIME_BALL;
                    vanillaInit = 1;
                    slots.set(3, new ItemStack(substituteItem, vanillaInit));
                    substituted = true;
                }
            }
        } catch (Throwable ignored) {}

        doBrew(world, pos, slots);

        // restore the real reagent, minus whatever the placeholder had consumed
        if (substituted && substituteItem != null) {
            try {
                int vanillaAfter = 0;
                ItemStack topAfter = slots.get(3);
                if (!topAfter.isEmpty() && topAfter.is(substituteItem)) {
                    vanillaAfter = topAfter.getCount();
                }
                int consumed = Math.max(0, vanillaInit - vanillaAfter);
                int newCount = Math.max(0, savedReagent.getCount() - consumed);
                if (newCount > 0) {
                    slots.set(3, new ItemStack(savedReagent.getItem(), newCount));
                } else {
                    slots.set(3, ItemStack.EMPTY);
                }
            } catch (Throwable ignored) {}
        }
    }
}


