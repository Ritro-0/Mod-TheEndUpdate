package com.theendupdate.mixin.client;

import com.theendupdate.TemplateMod;
import com.theendupdate.client.GatewayCompassContext;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(InGameHud.class)
public abstract class InGameHudMixin {

    @Inject(
        method = "renderHotbarItem(Lnet/minecraft/client/gui/DrawContext;IILnet/minecraft/client/render/RenderTickCounter;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/item/ItemStack;I)V",
        at = @At("HEAD")
    )
    private void theendupdate$setContextForHotbarItemHead(DrawContext context, int x, int y, RenderTickCounter tickCounter, PlayerEntity player, ItemStack stack, int seed, CallbackInfo ci) {
        GatewayCompassContext.set(stack);
    }

    @Inject(
        method = "renderHotbarItem(Lnet/minecraft/client/gui/DrawContext;IILnet/minecraft/client/render/RenderTickCounter;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/item/ItemStack;I)V",
        at = @At("TAIL")
    )
    private void theendupdate$clearContextForHotbarItemTail(DrawContext context, int x, int y, RenderTickCounter tickCounter, PlayerEntity player, ItemStack stack, int seed, CallbackInfo ci) {
        // Draw the overlay for hotbar items too (white like vanilla)
        net.minecraft.component.type.NbtComponent custom = stack.get(net.minecraft.component.DataComponentTypes.CUSTOM_DATA);
        if (custom != null) {
            var tag = custom.copyNbt();
            if (tag.contains("gx") && tag.contains("gy") && tag.contains("gz") && tag.contains("gd")) {
                net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
                if (client.world != null) {
                    long gcd = tag.getLong("gcd").orElse(0L);
                    long now = client.world.getTime();
                    long remain = gcd - now;
                    if (remain > 0L) {
                        float f = Math.min(1.0f, Math.max(0.0f, remain / 20.0f));
                        int overlayTop = y + Math.round(16.0f * (1.0f - f));
                        int alpha = (int)(f * 170.0f) << 24;
                        int color = alpha | 0x00FFFFFF;
                        context.fill(x, overlayTop, x + 16, y + 16, color);
                    }
                }
            }
        }
        GatewayCompassContext.clear();
    }
}


