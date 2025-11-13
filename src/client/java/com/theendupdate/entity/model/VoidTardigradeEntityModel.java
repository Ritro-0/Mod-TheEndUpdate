package com.theendupdate.entity.model;

import com.theendupdate.TemplateMod;
import com.theendupdate.entity.state.VoidTardigradeRenderState;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public class VoidTardigradeEntityModel extends EntityModel<VoidTardigradeRenderState> {
    public static final EntityModelLayer LAYER_LOCATION = new EntityModelLayer(
        Identifier.of(TemplateMod.MOD_ID, "void_tardigrade"),
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

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();

        ModelPartData body = root.addChild(
            "body",
            ModelPartBuilder.create()
                .uv(0, 0).cuboid(-0.5F, -4.0F, -2.0F, 5.0F, 3.0F, 4.0F)
                .uv(0, 7).cuboid(-2.0F, -3.0F, -1.0F, 7.0F, 2.0F, 2.0F)
                .uv(16, 11).cuboid(-2.5F, -2.25F, -0.5F, 1.0F, 1.0F, 1.0F),
            ModelTransform.of(0.0F, 24.0F, 0.0F, 0.0F, 0.0F, 0.0F)
        );

        body.addChild(
            "leg_front_left_outer",
            ModelPartBuilder.create().uv(0, 11).cuboid(-0.5F, 0.0F, -0.5F, 1.0F, 2.0F, 1.0F),
            ModelTransform.of(-1.0F, -2.0F, -1.0F, 0.0F, 0.0F, 0.0F)
        );
        body.addChild(
            "leg_front_left_inner",
            ModelPartBuilder.create().uv(12, 11).cuboid(-0.5F, 0.0F, -0.5F, 1.0F, 2.0F, 1.0F),
            ModelTransform.of(0.5F, -2.0F, -2.0F, 0.0F, 0.0F, 0.0F)
        );
        body.addChild(
            "leg_front_right_inner",
            ModelPartBuilder.create().uv(8, 11).cuboid(-0.5F, 0.0F, -0.5F, 1.0F, 2.0F, 1.0F),
            ModelTransform.of(2.0F, -2.0F, -2.0F, 0.0F, 0.0F, 0.0F)
        );
        body.addChild(
            "leg_front_right_outer",
            ModelPartBuilder.create().uv(4, 11).cuboid(-0.5F, 0.0F, -0.5F, 1.0F, 2.0F, 1.0F),
            ModelTransform.of(3.5F, -2.0F, -2.0F, 0.0F, 0.0F, 0.0F)
        );
        body.addChild(
            "leg_back_left_outer",
            ModelPartBuilder.create().uv(0, 14).cuboid(-0.5F, 0.0F, -0.5F, 1.0F, 2.0F, 1.0F),
            ModelTransform.of(-1.0F, -2.0F, 1.0F, 0.0F, 0.0F, 0.0F)
        );
        body.addChild(
            "leg_back_left_inner",
            ModelPartBuilder.create().uv(12, 14).cuboid(-0.5F, 0.0F, -0.5F, 1.0F, 2.0F, 1.0F),
            ModelTransform.of(0.5F, -2.0F, 2.0F, 0.0F, 0.0F, 0.0F)
        );
        body.addChild(
            "leg_back_right_inner",
            ModelPartBuilder.create().uv(8, 14).cuboid(-0.5F, 0.0F, -0.5F, 1.0F, 2.0F, 1.0F),
            ModelTransform.of(2.0F, -2.0F, 2.0F, 0.0F, 0.0F, 0.0F)
        );
        body.addChild(
            "leg_back_right_outer",
            ModelPartBuilder.create().uv(4, 14).cuboid(-0.5F, 0.0F, -0.5F, 1.0F, 2.0F, 1.0F),
            ModelTransform.of(3.5F, -2.0F, 2.0F, 0.0F, 0.0F, 0.0F)
        );

        return TexturedModelData.of(modelData, 32, 32);
    }

    @Override
    public void setAngles(VoidTardigradeRenderState state) {
        for (ModelPart part : this.root.traverse()) {
            part.resetTransform();
        }

        float speed = MathHelper.clamp(state.horizontalSpeed * 2.5F + 0.25F, 0.25F, 1.6F);
        float cycle = state.age * 0.6F + state.animationSeed * (float)Math.PI * 2.0F;

        this.body.roll = MathHelper.sin(cycle * 0.5F) * 0.1F;
        this.body.pitch = MathHelper.cos(cycle * 0.4F) * 0.05F;

        for (int i = 0; i < this.legs.length; i++) {
            ModelPart leg = this.legs[i];
            float phase = cycle + this.legPhaseOffsets[i];
            leg.pitch = MathHelper.sin(phase) * 0.6F * speed + 0.2F;
            leg.roll = MathHelper.cos(phase) * 0.2F * speed;
        }
    }
}

