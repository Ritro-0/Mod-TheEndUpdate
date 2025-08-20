package com.theendupdate.mixin.client;

import com.theendupdate.client.GatewayCompassContext;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.item.property.numeric.CompassProperty;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(value = CompassProperty.class)
public abstract class CompassPropertyContextMixin {

	@Inject(
		method = "getValue(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/world/ClientWorld;Lnet/minecraft/entity/LivingEntity;I)F",
		at = @At("HEAD")
	)
	private void theendupdate$setContext(ItemStack stack, ClientWorld world, LivingEntity entity, int seed, CallbackInfoReturnable<Float> cir) {
		GatewayCompassContext.set(stack);
	}

	@Inject(
		method = "getValue(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/world/ClientWorld;Lnet/minecraft/entity/LivingEntity;I)F",
		at = @At("RETURN")
	)
	private void theendupdate$clearContext(ItemStack stack, ClientWorld world, LivingEntity entity, int seed, CallbackInfoReturnable<Float> cir) {
		GatewayCompassContext.clear();
	}
}


