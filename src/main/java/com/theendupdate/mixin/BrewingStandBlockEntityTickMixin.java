package com.theendupdate.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BrewingStandBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.BrewingRecipeRegistry;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BrewingStandBlockEntity.class)
public class BrewingStandBlockEntityTickMixin {
    private static final Identifier ENDER_CHRYS = Identifier.of("theendupdate", "ender_chrysanthemum");
    private static final Identifier KING_PHANTOM_ESSENCE = Identifier.of("theendupdate", "king_phantom_essence");

    @Shadow
    private static boolean canCraft(BrewingRecipeRegistry recipes, DefaultedList<ItemStack> slots) { return false; }

    @Shadow
    private static void craft(World world, BlockPos pos, DefaultedList<ItemStack> slots) {}

    private static DefaultedList<ItemStack> theendupdate$effectiveSlots(DefaultedList<ItemStack> slots) {
        try {
            ItemStack reagent = slots.get(3);
            if (!reagent.isEmpty()) {
                DefaultedList<ItemStack> copy = DefaultedList.ofSize(slots.size(), ItemStack.EMPTY);
                for (int i = 0; i < slots.size(); i++) copy.set(i, slots.get(i));
                
                // Substitute ender chrysanthemum with cobweb
                if (reagent.isOf(Registries.ITEM.get(ENDER_CHRYS))) {
                    copy.set(3, new ItemStack(Items.COBWEB));
                    return copy;
                }
                // Substitute king phantom essence with slime ball (has no vanilla recipes)
                else if (reagent.isOf(Registries.ITEM.get(KING_PHANTOM_ESSENCE))) {
                    copy.set(3, new ItemStack(Items.SLIME_BALL));
                    return copy;
                }
            }
        } catch (Throwable ignored) {}
        return slots;
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/entity/BrewingStandBlockEntity;canCraft(Lnet/minecraft/recipe/BrewingRecipeRegistry;Lnet/minecraft/util/collection/DefaultedList;)Z"))
    private static boolean theendupdate$redirectCanCraft(BrewingRecipeRegistry recipes, DefaultedList<ItemStack> slots, World world, BlockPos pos, BlockState state, BrewingStandBlockEntity self) {
        return canCraft(recipes, theendupdate$effectiveSlots(slots));
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/entity/BrewingStandBlockEntity;craft(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/collection/DefaultedList;)V"))
    private static void theendupdate$redirectCraft(World world, BlockPos pos, DefaultedList<ItemStack> slots, World world2, BlockPos pos2, BlockState state, BrewingStandBlockEntity self) {
        // Perform craft, substituting reagent in-place so outputs are written back to the real slots
        boolean substituted = false;
        ItemStack savedReagent = ItemStack.EMPTY;
        int vanillaInit = 0;
        net.minecraft.item.Item substituteItem = null;
        
        try {
            ItemStack top = slots.get(3);
            if (!top.isEmpty()) {
                // Handle ender chrysanthemum -> cobweb
                if (top.isOf(Registries.ITEM.get(ENDER_CHRYS))) {
                    savedReagent = top.copy();
                    substituteItem = Items.COBWEB;
                    vanillaInit = 1;
                    slots.set(3, new ItemStack(substituteItem, vanillaInit));
                    substituted = true;
                }
                // Handle king phantom essence -> slime ball (has no vanilla recipes)
                else if (top.isOf(Registries.ITEM.get(KING_PHANTOM_ESSENCE))) {
                    savedReagent = top.copy();
                    substituteItem = Items.SLIME_BALL;
                    vanillaInit = 1;
                    slots.set(3, new ItemStack(substituteItem, vanillaInit));
                    substituted = true;
                }
            }
        } catch (Throwable ignored) {}

        craft(world, pos, slots);

        // Restore reagent and propagate consumption
        if (substituted && substituteItem != null) {
            try {
                int vanillaAfter = 0;
                ItemStack topAfter = slots.get(3);
                if (!topAfter.isEmpty() && topAfter.isOf(substituteItem)) {
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


