package com.theendupdate.entity.model;

import com.theendupdate.TemplateMod;
import com.theendupdate.entity.animation.Ethereal;
import com.theendupdate.entity.state.EtherealOrbRenderState;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.animation.Animation;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.Identifier;

/**
 * Model for the Ethereal Orb entity - Minecraft 1.21.8 version
 */
public class EtherealOrbEntityModel extends EntityModel<EtherealOrbRenderState> {
    // Entity model layer for registration
    public static final EntityModelLayer ETHEREAL_ORB_LAYER = new EntityModelLayer(
        Identifier.of(TemplateMod.MOD_ID, "ethereal_orb"), "main"
    );
    private final ModelPart root;
    private ModelPart headPart;
private final Animation moveforwards;
private final Animation stopmoving;
private final Animation rotate;
    public EtherealOrbEntityModel(ModelPart root) {
        super(root);
        this.root = root;
        moveforwards = Ethereal.ANIMATION.createAnimation(root);
        stopmoving =Ethereal.ANIMATION2.createAnimation(root);
        rotate = Ethereal.ANIMATION3.createAnimation(root);
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData modelPartData = modelData.getRoot();
        ModelPartData body = modelPartData.addChild("body", ModelPartBuilder.create().uv(0, 0).cuboid(-3.0F, -6.0F, -3.0F, 7.0F, 5.0F, 7.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 20.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        body.addChild("head", ModelPartBuilder.create().uv(0, 12).cuboid(-1.0F, -8.0F, -1.0F, 3.0F, 2.0F, 3.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        body.addChild("legs", ModelPartBuilder.create().uv(16, 12).cuboid(2.0F, -0.2F, -2.0F, 1.0F, 4.0F, 1.0F, new Dilation(0.0F))
            .uv(0, 17).cuboid(-2.0F, -0.2F, -2.0F, 1.0F, 4.0F, 1.0F, new Dilation(0.0F))
            .uv(8, 17).cuboid(-3.0F, 0.0F, 0.0F, 1.0F, 4.0F, 1.0F, new Dilation(0.0F))
            .uv(12, 17).cuboid(0.0F, -0.2F, -3.0F, 1.0F, 4.0F, 1.0F, new Dilation(0.0F))
            .uv(16, 17).cuboid(3.0F, 0.0F, 0.0F, 1.0F, 4.0F, 1.0F, new Dilation(0.0F))
            .uv(4, 17).cuboid(-2.0F, 0.0F, 2.0F, 1.0F, 4.0F, 1.0F, new Dilation(0.0F))
            .uv(12, 12).cuboid(2.0F, 0.0F, 2.0F, 1.0F, 4.0F, 1.0F, new Dilation(0.0F))
            .uv(20, 12).cuboid(0.0F, 0.0F, 3.0F, 1.0F, 4.0F, 1.0F, new Dilation(0.0F))
            .uv(20, 17).cuboid(0.0F, 0.0F, 0.0F, 1.0F, 4.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        return TexturedModelData.of(modelData, 32, 32);
    }

    public void setAngles(EtherealOrbRenderState state) {
        // Reset all transforms before applying animations to avoid accumulation
        for (ModelPart part : this.root.traverse()) {
            part.resetTransform();
        }
        if (headPart == null) {
            try {
                headPart = this.root.getChild("body").getChild("head");
            } catch (Throwable ignored) {}
        }
        if (headPart != null) {
            // Show head when bulbPresent is true; hide only when stunted and bulb is absent
            boolean hide = state.baby && state.stunted && !state.bulbPresent;
            headPart.visible = !hide;
        }
        moveforwards.apply(state.moveAnimationState, state.age, 1.0f);
        stopmoving.apply(state.finishmovementAnimationState, state.age, 1.0f);
        rotate.apply(state.rotateAnimationState, state.age, 1.0f);
        // Hook for future animation application using state
    }

    /**
     * Get the root ModelPart for direct rendering access
     * Used by feature renderers that need to submit render commands to OrderedRenderCommandQueue
     */
    public ModelPart getRoot() {
        return this.root;
    }
}
     