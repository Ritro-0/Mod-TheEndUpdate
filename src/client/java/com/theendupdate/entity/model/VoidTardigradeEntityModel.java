package com.theendupdate.entity.model;

import com.theendupdate.TheEndUpdate;
import com.theendupdate.entity.state.VoidTardigradeRenderState;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class VoidTardigradeEntityModel extends EntityModel<VoidTardigradeRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(TheEndUpdate.MOD_ID, "void_tardigrade"),
        "main"
    );

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart[] legs;
    private final float[] legPhaseOffsets = new float[]{
        0.0F,
        0.9F,
        (float)Math.PI,
        (float)Math.PI + 0.9F,
        0.6F,
        1.5F,
        (float)Math.PI + 0.6F,
        (float)Math.PI + 1.5F
    };

    public VoidTardigradeEntityModel(ModelPart root) {
        super(root);
        this.root = root;
        this.body = root.getChild("body");

        this.legs = new ModelPart[]{
            this.body.getChild("leg_front_left_outer"),
            this.body.getChild("leg_front_left_inner"),
            this.body.getChild("leg_front_right_inner"),
            this.body.getChild("leg_front_right_outer"),
            this.body.getChild("leg_back_left_outer"),
            this.body.getChild("leg_back_left_inner"),
            this.body.getChild("leg_back_right_inner"),
            this.body.getChild("leg_back_right_outer")
        };
    }

    public static LayerDefinition getLayerDefinition() {
        MeshDefinition modelData = new MeshDefinition();
        PartDefinition root = modelData.getRoot();

        PartDefinition body = root .addOrReplaceChild(
            "body",
            CubeListBuilder.create()
                .texOffs(0, 0).addBox(-0.5F, -4.0F, -2.0F, 5.0F, 3.0F, 4.0F)
                .texOffs(0, 7).addBox(-2.0F, -3.0F, -1.0F, 7.0F, 2.0F, 2.0F)
                .texOffs(16, 11).addBox(-2.5F, -2.25F, -0.5F, 1.0F, 1.0F, 1.0F),
            PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, 0.0F, 0.0F, 0.0F)
        );

        body .addOrReplaceChild(
            "leg_front_left_outer",
            CubeListBuilder.create().texOffs(0, 11).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 2.0F, 1.0F),
            PartPose.offsetAndRotation(-1.0F, -2.0F, -1.0F, 0.0F, 0.0F, 0.0F)
        );
        body .addOrReplaceChild(
            "leg_front_left_inner",
            CubeListBuilder.create().texOffs(12, 11).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 2.0F, 1.0F),
            PartPose.offsetAndRotation(0.5F, -2.0F, -2.0F, 0.0F, 0.0F, 0.0F)
        );
        body .addOrReplaceChild(
            "leg_front_right_inner",
            CubeListBuilder.create().texOffs(8, 11).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 2.0F, 1.0F),
            PartPose.offsetAndRotation(2.0F, -2.0F, -2.0F, 0.0F, 0.0F, 0.0F)
        );
        body .addOrReplaceChild(
            "leg_front_right_outer",
            CubeListBuilder.create().texOffs(4, 11).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 2.0F, 1.0F),
            PartPose.offsetAndRotation(3.5F, -2.0F, -2.0F, 0.0F, 0.0F, 0.0F)
        );
        body .addOrReplaceChild(
            "leg_back_left_outer",
            CubeListBuilder.create().texOffs(0, 14).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 2.0F, 1.0F),
            PartPose.offsetAndRotation(-1.0F, -2.0F, 1.0F, 0.0F, 0.0F, 0.0F)
        );
        body .addOrReplaceChild(
            "leg_back_left_inner",
            CubeListBuilder.create().texOffs(12, 14).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 2.0F, 1.0F),
            PartPose.offsetAndRotation(0.5F, -2.0F, 2.0F, 0.0F, 0.0F, 0.0F)
        );
        body .addOrReplaceChild(
            "leg_back_right_inner",
            CubeListBuilder.create().texOffs(8, 14).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 2.0F, 1.0F),
            PartPose.offsetAndRotation(2.0F, -2.0F, 2.0F, 0.0F, 0.0F, 0.0F)
        );
        body .addOrReplaceChild(
            "leg_back_right_outer",
            CubeListBuilder.create().texOffs(4, 14).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 2.0F, 1.0F),
            PartPose.offsetAndRotation(3.5F, -2.0F, 2.0F, 0.0F, 0.0F, 0.0F)
        );

        return LayerDefinition.create(modelData, 32, 32);
    }

    @Override
    public void setupAnim(VoidTardigradeRenderState state) {
        for (ModelPart part : this.root.getAllParts()) {
            part.resetPose();
        }

        float speed = Mth.clamp(state.horizontalSpeed * 2.5F + 0.25F, 0.25F, 1.6F);
        float cycle = state.ageInTicks * 0.6F + state.animationSeed * (float)Math.PI * 2.0F;

        this.body.zRot = Mth.sin(cycle * 0.5F) * 0.1F;
        this.body.xRot = Mth.cos(cycle * 0.4F) * 0.05F;

        for (int i = 0; i < this.legs.length; i++) {
            ModelPart leg = this.legs[i];
            float phase = cycle + this.legPhaseOffsets[i];
            leg.xRot = Mth.sin(phase) * 0.6F * speed + 0.2F;
            leg.zRot = Mth.cos(phase) * 0.2F * speed;
        }
    }
}

