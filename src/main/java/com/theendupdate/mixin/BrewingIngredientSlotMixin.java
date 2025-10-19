package com.theendupdate.mixin;

import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.screen.BrewingStandScreenHandler$IngredientSlot")
public class BrewingIngredientSlotMixin {
    @Inject(method = "canInsert", at = @At("HEAD"), cancellable = true)
    private void theendupdate$acceptChrysanthemum(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (stack != null && (
            stack.isOf(Registries.ITEM.get(Identifier.of("theendupdate", "ender_chrysanthemum"))) ||
            stack.isOf(Registries.ITEM.get(Identifier.of("theendupdate", "king_phantom_essence")))
        )) {
            cir.setReturnValue(true);
        }
    }
}


