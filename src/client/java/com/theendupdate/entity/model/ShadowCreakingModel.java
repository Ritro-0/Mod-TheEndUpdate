package com.theendupdate.entity.model;

import com.theendupdate.TheEndUpdate;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/** Placeholder layer, real geometry is {@link ShadowCreakingMesh} */
public class ShadowCreakingModel extends EntityModel<LivingEntityRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(TheEndUpdate.MOD_ID, "shadow_creaking"), "main");
    public static final ModelLayerLocation SHOULDERS_LAYER = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(TheEndUpdate.MOD_ID, "shadow_creaking"), "shoulders");

    public ShadowCreakingModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));
        return LayerDefinition.create(mesh, 96, 96);
    }

    public static LayerDefinition createShouldersLayer() {
        return createBodyLayer();
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
    }
}
