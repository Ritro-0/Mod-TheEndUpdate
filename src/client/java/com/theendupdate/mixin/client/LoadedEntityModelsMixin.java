package com.theendupdate.mixin.client;

import com.theendupdate.TemplateMod;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.LoadedEntityModels;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LoadedEntityModels.class)
public class LoadedEntityModelsMixin {
    
    @Inject(method = "getModelPart", at = @At("HEAD"), cancellable = true)
    private void redirectCustomSignModels(EntityModelLayer layer, CallbackInfoReturnable<ModelPart> cir) {
        // EntityModelLayer uses id() method to get the identifier
        Identifier id = layer.id();
        
        // Redirect our custom sign model layers to use vanilla oak models
        if (id.getNamespace().equals(TemplateMod.MOD_ID)) {
            String path = id.getPath();
            
            if (path.startsWith("sign/standing/")) {
                // Redirect to vanilla oak standing sign
                EntityModelLayer oakLayer = new EntityModelLayer(Identifier.ofVanilla("sign/standing/oak"), "main");
                cir.setReturnValue(((LoadedEntityModels)(Object)this).getModelPart(oakLayer));
            } else if (path.startsWith("hanging_sign/")) {
                // Hanging signs have format: hanging_sign/{wood_type}/{part}
                // Extract the part (e.g., "ceiling_middle", "wall_left", etc.)
                String[] parts = path.split("/", 3);
                if (parts.length == 3) {
                    String part = parts[2]; // e.g., "ceiling_middle"
                    EntityModelLayer oakLayer = new EntityModelLayer(Identifier.ofVanilla("hanging_sign/oak/" + part), "main");
                    cir.setReturnValue(((LoadedEntityModels)(Object)this).getModelPart(oakLayer));
                }
            }
        }
    }
}

