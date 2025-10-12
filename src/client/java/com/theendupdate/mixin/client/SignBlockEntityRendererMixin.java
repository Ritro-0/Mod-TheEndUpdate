package com.theendupdate.mixin.client;

import com.theendupdate.registry.ModBlocks;
import net.minecraft.block.WoodType;
import net.minecraft.client.render.block.entity.SignBlockEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.Map;

@Mixin(SignBlockEntityRenderer.class)
public class SignBlockEntityRendererMixin {
    
    @Inject(method = "<init>", at = @At("RETURN"))
    private void addCustomWoodTypeModels(net.minecraft.client.render.block.entity.BlockEntityRendererFactory.Context context, CallbackInfo ci) {
        try {
            // Find the Map<WoodType, ?> field by iterating through all fields
            Field typeToModelField = null;
            for (Field field : SignBlockEntityRenderer.class.getDeclaredFields()) {
                if (Map.class.isAssignableFrom(field.getType())) {
                    typeToModelField = field;
                    break;
                }
            }
            
            if (typeToModelField == null) {
                com.theendupdate.TemplateMod.LOGGER.error("Could not find typeToModel field in SignBlockEntityRenderer");
                return;
            }
            
            typeToModelField.setAccessible(true);
            
            @SuppressWarnings("unchecked")
            Map<WoodType, Object> oldMap = (Map<WoodType, Object>) typeToModelField.get(this);
            
            // Create a new mutable map with all existing entries plus our custom ones
            Map<WoodType, Object> newMap = new java.util.HashMap<>(oldMap);
            
            // Reuse oak's model pair for our custom wood types (same geometry, different textures)
            Object oakPair = newMap.get(WoodType.OAK);
            if (oakPair != null) {
                newMap.put(ModBlocks.ETHEREAL_WOOD_TYPE, oakPair);
                newMap.put(ModBlocks.SHADOW_WOOD_TYPE, oakPair);
                
                // Replace the immutable map with our mutable one
                typeToModelField.set(this, newMap);
                com.theendupdate.TemplateMod.LOGGER.info("Successfully registered custom sign wood types");
            }
        } catch (Exception e) {
            // Log but don't crash
            com.theendupdate.TemplateMod.LOGGER.error("Failed to add custom sign wood types to renderer", e);
        }
    }
}

