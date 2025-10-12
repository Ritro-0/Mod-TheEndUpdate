package com.theendupdate.mixin.client;

import com.theendupdate.TemplateMod;
import com.theendupdate.registry.ModBlocks;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TexturedRenderLayers.class)
public class TexturedRenderLayersSignsMixin {
    
    @Inject(method = "getSignTextureId", at = @At("HEAD"), cancellable = true)
    private static void getCustomSignTexture(net.minecraft.block.WoodType woodType, CallbackInfoReturnable<SpriteIdentifier> cir) {
        if (woodType == ModBlocks.ETHEREAL_WOOD_TYPE) {
            cir.setReturnValue(new SpriteIdentifier(
                TexturedRenderLayers.SIGNS_ATLAS_TEXTURE,
                Identifier.of(TemplateMod.MOD_ID, "entity/signs/ethereal")
            ));
        } else if (woodType == ModBlocks.SHADOW_WOOD_TYPE) {
            cir.setReturnValue(new SpriteIdentifier(
                TexturedRenderLayers.SIGNS_ATLAS_TEXTURE,
                Identifier.of(TemplateMod.MOD_ID, "entity/signs/shadow")
            ));
        }
    }
}

