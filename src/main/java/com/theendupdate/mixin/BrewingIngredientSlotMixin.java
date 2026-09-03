package com.theendupdate.mixin;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.inventory.BrewingStandMenu$IngredientsSlot")
public class BrewingIngredientSlotMixin {
    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void theendupdate$acceptChrysanthemum(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (stack != null && (
            stack.is(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("theendupdate", "ender_chrysanthemum"))) ||
            stack.is(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("theendupdate", "closed_ender_chrysanthemum"))) ||
            stack.is(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("theendupdate", "king_phantom_essence")))
        )) {
            cir.setReturnValue(true);
        }
    }
}


