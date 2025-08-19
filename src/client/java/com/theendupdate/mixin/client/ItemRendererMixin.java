package com.theendupdate.mixin.client;

import com.theendupdate.client.GatewayCompassContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {

	// Legacy signature (no boolean)
	@Inject(method = "renderInGui(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/item/ItemStack;II)V", at = @At("HEAD"))
	private void theendupdate$setContextRenderInGui(DrawContext context, ItemStack stack, int x, int y, CallbackInfo ci) {
		GatewayCompassContext.set(stack);
	}
	@Inject(method = "renderInGui(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/item/ItemStack;II)V", at = @At("TAIL"))
	private void theendupdate$clearContextRenderInGui(DrawContext context, ItemStack stack, int x, int y, CallbackInfo ci) {
		GatewayCompassContext.clear();
	}

	// Boolean signature (some mappings add renderOverlay boolean)
	@Inject(method = "renderInGui(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/item/ItemStack;IIZ)V", at = @At("HEAD"), require = 0)
	private void theendupdate$setContextRenderInGuiBool(DrawContext context, ItemStack stack, int x, int y, boolean renderOverlay, CallbackInfo ci) {
		GatewayCompassContext.set(stack);
	}
	@Inject(method = "renderInGui(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/item/ItemStack;IIZ)V", at = @At("TAIL"), require = 0)
	private void theendupdate$clearContextRenderInGuiBool(DrawContext context, ItemStack stack, int x, int y, boolean renderOverlay, CallbackInfo ci) {
		GatewayCompassContext.clear();
	}

	// Legacy overrides signature
	@Inject(method = "renderInGuiWithOverrides(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/item/ItemStack;III)V", at = @At("HEAD"))
	private void theendupdate$setContextRenderInGuiWithOverrides(DrawContext context, PlayerEntity player, ItemStack stack, int x, int y, int seed, CallbackInfo ci) {
		GatewayCompassContext.set(stack);
	}
	@Inject(method = "renderInGuiWithOverrides(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/item/ItemStack;III)V", at = @At("TAIL"))
	private void theendupdate$clearContextRenderInGuiWithOverrides(DrawContext context, PlayerEntity player, ItemStack stack, int x, int y, int seed, CallbackInfo ci) {
		theendupdate$drawPerStackCooldownOverlay(context, stack, x, y);
		GatewayCompassContext.clear();
	}

	// Boolean overrides signature
	@Inject(method = "renderInGuiWithOverrides(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/item/ItemStack;IIIZ)V", at = @At("HEAD"), require = 0)
	private void theendupdate$setContextRenderInGuiWithOverridesBool(DrawContext context, PlayerEntity player, ItemStack stack, int x, int y, int seed, boolean renderOverlay, CallbackInfo ci) {
		GatewayCompassContext.set(stack);
	}
	@Inject(method = "renderInGuiWithOverrides(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/item/ItemStack;IIIZ)V", at = @At("TAIL"), require = 0)
	private void theendupdate$clearContextRenderInGuiWithOverridesBool(DrawContext context, PlayerEntity player, ItemStack stack, int x, int y, int seed, boolean renderOverlay, CallbackInfo ci) {
		theendupdate$drawPerStackCooldownOverlay(context, stack, x, y);
		GatewayCompassContext.clear();
	}

	private void theendupdate$drawPerStackCooldownOverlay(DrawContext context, ItemStack stack, int x, int y) {
		if (stack == null || stack.isEmpty()) return;
		NbtComponent custom = stack.get(DataComponentTypes.CUSTOM_DATA);
		if (custom == null) return;
		var tag = custom.copyNbt();
		if (!tag.contains("gx") || !tag.contains("gy") || !tag.contains("gz") || !tag.contains("gd")) return;
		long gcd = tag.getLong("gcd").orElse(0L);
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.world == null) return;
		long now = client.world.getTime();
		long remain = gcd - now;
		if (remain <= 0L) return;
		float duration = 20.0f;
		float f = Math.min(1.0f, Math.max(0.0f, remain / duration));
		int overlayTop = y + Math.round(16.0f * (1.0f - f));
		int alpha = (int)(f * 170.0f) << 24; // translucent black
		context.fill(x, overlayTop, x + 16, y + 16, alpha);
	}
}


