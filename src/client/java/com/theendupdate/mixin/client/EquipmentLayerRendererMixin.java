package com.theendupdate.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Spectral trim is drawn as a separate overlay with normal entity lighting, so it
 * goes dark in caves. Keep vanilla's armor-trim render type (it samples the trim
 * atlas correctly) but force full-bright lighting so the pattern stays visible.
 */
@Mixin(EquipmentLayerRenderer.class)
public class EquipmentLayerRendererMixin {
    private static final int FULL_BRIGHT = 0xF000F0;

    @Unique
    private ItemStack theendupdate$currentArmor;

    @Inject(
        method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V",
        at = @At("HEAD")
    )
    private void theendupdate$storeArmorStack(
        net.minecraft.client.resources.model.EquipmentClientInfo.LayerType layerType,
        ResourceKey<?> equipmentAsset,
        Model<?> armorModel,
        Object renderState,
        ItemStack item,
        PoseStack poseStack,
        net.minecraft.client.renderer.SubmitNodeCollector nodeCollector,
        int packedLight,
        Identifier texture,
        int outlineColor,
        int key,
        CallbackInfo ci
    ) {
        this.theendupdate$currentArmor = item;
    }

    @Inject(
        method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V",
        at = @At("RETURN")
    )
    private void theendupdate$clearArmorStack(CallbackInfo ci) {
        this.theendupdate$currentArmor = null;
    }

    @WrapOperation(
        method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/OrderedSubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"
        )
    )
    private void theendupdate$emissiveSpectralTrim(
        OrderedSubmitNodeCollector collector,
        Model<?> model,
        Object state,
        PoseStack poseStack,
        RenderType renderType,
        int light,
        int overlay,
        int color,
        TextureAtlasSprite sprite,
        int outline,
        ModelFeatureRenderer.CrumblingOverlay crumbling,
        Operation<Void> original
    ) {
        // only the trim overlay passes a sprite - base armor and glint pass null
        int packedLight = light;
        if (sprite != null && theendupdate$isSpectralTrim(this.theendupdate$currentArmor)) {
            packedLight = FULL_BRIGHT;
        }
        original.call(collector, model, state, poseStack, renderType, packedLight, overlay, color, sprite, outline, crumbling);
    }

    @Unique
    private static boolean theendupdate$isSpectralTrim(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        ArmorTrim trim = stack.get(DataComponents.TRIM);
        if (trim == null) {
            return false;
        }
        Identifier matId = trim.material().unwrapKey().map(ResourceKey::identifier).orElse(null);
        if (matId == null) {
            return false;
        }
        String path = matId.getPath();
        return "spectral".equals(path) || "spectral_cluster".equals(path);
    }
}
