package com.theendupdate.mixin.client;

import com.theendupdate.client.GatewayCompassContext;
import com.theendupdate.TemplateMod;
import net.minecraft.client.MinecraftClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(DrawContext.class)
public abstract class DrawContextMixin {

	@Inject(method = "drawItem(Lnet/minecraft/item/ItemStack;II)V", at = @At("HEAD"))
	private void theendupdate$setStackHead(ItemStack stack, int x, int y, CallbackInfo ci) {
		GatewayCompassContext.set(stack);
	}

	@Inject(method = "drawItem(Lnet/minecraft/item/ItemStack;II)V", at = @At("TAIL"))
	private void theendupdate$clearStackTail(ItemStack stack, int x, int y, CallbackInfo ci) {
		DrawContext self = (DrawContext)(Object)this;
		theendupdate$drawPerStackCooldownOverlay(self, stack, x, y);
		GatewayCompassContext.clear();
	}

	// Overload with seed
	@Inject(method = "drawItem(Lnet/minecraft/item/ItemStack;III)V", at = @At("HEAD"))
	private void theendupdate$setStackHeadSeed(ItemStack stack, int x, int y, int seed, CallbackInfo ci) {
		GatewayCompassContext.set(stack);
	}

	@Inject(method = "drawItem(Lnet/minecraft/item/ItemStack;III)V", at = @At("TAIL"))
	private void theendupdate$clearStackTailSeed(ItemStack stack, int x, int y, int seed, CallbackInfo ci) {
		DrawContext self = (DrawContext)(Object)this;
		theendupdate$drawPerStackCooldownOverlay(self, stack, x, y);
		GatewayCompassContext.clear();
	}

	private static void theendupdate$drawPerStackCooldownOverlay(DrawContext context, ItemStack stack, int x, int y) {
		if (stack == null || stack.isEmpty()) return;
		NbtComponent custom = stack.get(DataComponentTypes.CUSTOM_DATA);
		if (custom == null) return;
		var tag = custom.copyNbt();
		if (!(tag.contains("gx") && tag.contains("gy") && tag.contains("gz") && tag.contains("gd"))) return;
		long gcd = tag.getLong("gcd").orElse(0L);
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.world == null) return;
		long now = client.world.getTime();
		long remain = gcd - now;
		if (remain <= 0L) return;
		float duration = 20.0f;
		float f = Math.min(1.0f, Math.max(0.0f, remain / duration));
		int overlayTop = y + Math.round(16.0f * (1.0f - f));
		int alpha = (int)(f * 170.0f) << 24;
		int color = alpha | 0x00FFFFFF;
		context.fill(x, overlayTop, x + 16, y + 16, color);
	}
}


