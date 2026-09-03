package com.theendupdate.entity.model;

import com.theendupdate.TheEndUpdate;
import com.theendupdate.entity.state.EyesRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;

/** Flat 16x16 plane from the Blockbench Eyes export */
public class EyesEntityModel extends EntityModel<EyesRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(TheEndUpdate.MOD_ID, "eyes"),
        "main"
    );

    public EyesEntityModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild(
            "bb_main",
            CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -16.0F, -0.05F, 16.0F, 16.0F, 0.1F, new CubeDeformation(0.0F)),
            PartPose.offset(0.0F, 24.0F, 0.0F)
        );
        return LayerDefinition.create(mesh, 16, 16);
    }

    @Override
    public void setupAnim(EyesRenderState state) {
        super.setupAnim(state);
    }
}
