package com.theendupdate.mixin.client;

import com.theendupdate.client.GatewayCompassContext;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.CompassAngle;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(CompassAngle.class)
public abstract class CompassPropertyContextMixin {

    @Inject(
        method = "get(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/client/multiplayer/ClientLevel;Lnet/minecraft/world/entity/ItemOwner;I)F",
        at = @At("HEAD"))
    private void theendupdate$setContext(ItemStack stack, ClientLevel world, ItemOwner owner, int seed,
        CallbackInfoReturnable<Float> cir) {
        GatewayCompassContext.set(stack);
    }

    @Inject(
        method = "get(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/client/multiplayer/ClientLevel;Lnet/minecraft/world/entity/ItemOwner;I)F",
        at = @At("RETURN"))
    private void theendupdate$clearContext(ItemStack stack, ClientLevel world, ItemOwner owner, int seed,
        CallbackInfoReturnable<Float> cir) {
        GatewayCompassContext.clear();
    }
}
