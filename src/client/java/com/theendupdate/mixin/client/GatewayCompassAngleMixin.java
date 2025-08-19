package com.theendupdate.mixin.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(targets = {
    "net.minecraft.client.item.RecoveryCompassAnglePredicateProvider",
    "net.minecraft.client.item.CompassAnglePredicateProvider"
})
public abstract class GatewayCompassAngleMixin {

	@Inject(method = {
		"unclampedCall(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/world/ClientWorld;Lnet/minecraft/entity/LivingEntity;I)F",
		"call(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/world/ClientWorld;Lnet/minecraft/entity/LivingEntity;I)F"
	}, at = @At("HEAD"), cancellable = true)
	private void theendupdate$gatewayAngle(ItemStack stack, ClientWorld world, LivingEntity entity, int seed, CallbackInfoReturnable<Float> cir) {
		if (world == null || entity == null) return;
		NbtComponent custom = stack.get(DataComponentTypes.CUSTOM_DATA);
		if (custom == null) return;
		var tag = custom.copyNbt();
		if (!tag.contains("gx") || !tag.contains("gy") || !tag.contains("gz") || !tag.contains("gd")) return;
		String dim = tag.getString("gd").orElse("");
		String worldDim = world.getRegistryKey().getValue().toString();
		if (!worldDim.equals(dim)) return;
		int x = tag.getInt("gx").orElse(0);
		int z = tag.getInt("gz").orElse(0);
		double dx = (x + 0.5) - entity.getX();
		double dz = (z + 0.5) - entity.getZ();
		double target = Math.atan2(dz, dx);
		double yaw = MathHelper.wrapDegrees(entity.getYaw()) * (Math.PI / 180.0);
		double angle = Math.atan2(Math.sin(yaw - target), Math.cos(yaw - target));
		cir.setReturnValue((float) (angle / (Math.PI * 2.0)));
	}
}


