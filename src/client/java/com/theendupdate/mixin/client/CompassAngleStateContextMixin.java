package com.theendupdate.mixin.client;

import com.theendupdate.client.GatewayCompassContext;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.CompassAngleState;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// makes the held stack available while CompassAngleState.calculate() resolves the target GlobalPos
@Environment(EnvType.CLIENT)
@Mixin(CompassAngleState.class)
public abstract class CompassAngleStateContextMixin {

    @Inject(
        method = "calculate(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/client/multiplayer/ClientLevel;ILnet/minecraft/world/entity/ItemOwner;)F",
        at = @At("HEAD"))
    private void theendupdate$setContext(ItemStack stack, ClientLevel world, int seed, ItemOwner owner,
        CallbackInfoReturnable<Float> cir) {
        GatewayCompassContext.set(stack);
    }

    @Inject(
        method = "calculate(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/client/multiplayer/ClientLevel;ILnet/minecraft/world/entity/ItemOwner;)F",
        at = @At("RETURN"))
    private void theendupdate$clearContext(ItemStack stack, ClientLevel world, int seed, ItemOwner owner,
        CallbackInfoReturnable<Float> cir) {
        GatewayCompassContext.clear();
    }
}
