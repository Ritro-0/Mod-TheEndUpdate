package com.theendupdate.entity.model;

import com.theendupdate.TheEndUpdate;
import com.theendupdate.entity.animation.Ethereal;
import com.theendupdate.entity.state.EtherealOrbRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;

/**
 * Model for the Ethereal Orb entity - Minecraft 1.21.8 version
 */
public class EtherealOrbEntityModel extends EntityModel<EtherealOrbRenderState> {
    public static final ModelLayerLocation ETHEREAL_ORB_LAYER = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(TheEndUpdate.MOD_ID, "ethereal_orb"), "main"
    );
    private final ModelPart root;
    private ModelPart headPart;
    private final KeyframeAnimation moveforwards;
    private final KeyframeAnimation stopmoving;
    private final KeyframeAnimation rotate;
    public EtherealOrbEntityModel(ModelPart root) {
        super(root);
        this.root = root;
        moveforwards = Ethereal.ANIMATION.bake(root);
        stopmoving = Ethereal.ANIMATION2.bake(root);
        rotate = Ethereal.ANIMATION3.bake(root);
    }

    public static LayerDefinition getTexturedModelData() {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition root = meshDefinition.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -6.0F, -3.0F, 7.0F, 5.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 20.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 12).addBox(-1.0F, -8.0F, -1.0F, 3.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("legs", CubeListBuilder.create().texOffs(16, 12).addBox(2.0F, -0.2F, -2.0F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
            .texOffs(0, 17).addBox(-2.0F, -0.2F, -2.0F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
            .texOffs(8, 17).addBox(-3.0F, 0.0F, 0.0F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
            .texOffs(12, 17).addBox(0.0F, -0.2F, -3.0F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
            .texOffs(16, 17).addBox(3.0F, 0.0F, 0.0F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
            .texOffs(4, 17).addBox(-2.0F, 0.0F, 2.0F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
            .texOffs(12, 12).addBox(2.0F, 0.0F, 2.0F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
            .texOffs(20, 12).addBox(0.0F, 0.0F, 3.0F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
            .texOffs(20, 17).addBox(0.0F, 0.0F, 0.0F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        return LayerDefinition.create(meshDefinition, 32, 32);
    }

    public void setupAnim(EtherealOrbRenderState state) {
        // reset first, otherwise the keyframe animations below accumulate each tick
        for (ModelPart part : this.root.getAllParts()) {
            part.resetPose();
        }
        if (headPart == null) {
            try {
                headPart = this.root.getChild("body").getChild("head");
            } catch (Throwable ignored) {}
        }
        if (headPart != null) {
            // hide only when stunted with no bulb, otherwise always show
            boolean hide = state.baby && state.stunted && !state.bulbPresent;
            headPart.visible = !hide;
        }
        moveforwards.apply(state.moveAnimationState, state.ageInTicks, 1.0f);
        stopmoving.apply(state.finishmovementAnimationState, state.ageInTicks, 1.0f);
        rotate.apply(state.rotateAnimationState, state.ageInTicks, 1.0f);
    }

    // used by feature renderers that submit render commands directly
    public ModelPart getRoot() {
        return this.root;
    }
}
     