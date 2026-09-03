package com.theendupdate.entity.model;

import com.theendupdate.TheEndUpdate;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.monster.phantom.PhantomModel;
import net.minecraft.client.renderer.entity.state.PhantomRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class KingPhantomEntityModel extends PhantomModel {
    
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(TheEndUpdate.MOD_ID, "king_phantom"), "main");
    
    private final ModelPart leftWingBase;
    private final ModelPart leftWingTip;
    private final ModelPart rightWingBase;
    private final ModelPart rightWingTip;
    private final ModelPart tailBase;
    private final ModelPart tailTip;
    private final ModelPart body;
    
    public KingPhantomEntityModel(ModelPart root) {
        super(root);
        
        this.body = root.getChild("body");
        this.leftWingBase = body.getChild("left_wing_base");
        this.leftWingTip = leftWingBase.getChild("left_wing_tip");
        this.rightWingBase = body.getChild("right_wing_base");
        this.rightWingTip = rightWingBase.getChild("right_wing_tip");
        this.tailBase = body.getChild("tail_base");
        this.tailTip = tailBase.getChild("tail_tip");
    }
    
    @Override
    public void setupAnim(PhantomRenderState state) {
        // vanilla PhantomEntityModel handles pitch here; we override wings/tail below
        super.setupAnim(state);
        
        float ageInTicks = state.ageInTicks;
        
        float flapCycle = ageInTicks * 2.0f; // 4x vanilla flap speed
        float flapAngle = Mth.cos(flapCycle * 0.35f) * (float) Math.PI * 0.15f;
        
        this.leftWingBase.zRot = 0.1f + flapAngle;
        this.rightWingBase.zRot = -0.1f - flapAngle;
        
        // tips follow base with a slight phase offset for a more natural flap
        float tipOffset = Mth.cos(flapCycle * 0.35f + 0.3f) * (float) Math.PI * 0.1f;
        this.leftWingTip.zRot = 0.1f + tipOffset;
        this.rightWingTip.zRot = -0.1f - tipOffset;
        
        float tailCycle = ageInTicks * 1.2f;
        this.tailBase.yRot = Mth.cos(tailCycle * 0.3f) * 0.1f;
        this.tailTip.yRot = Mth.cos(tailCycle * 0.3f + 0.5f) * 0.15f;
        
        // pitch is applied in the renderer via MatrixStack instead - more reliable than
        // rotating model parts here
    }
    
    /** Textured model data for the King Phantom, builds on the vanilla phantom model */
    public static LayerDefinition getTexturedModelData() {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition modelPartData = meshDefinition.getRoot();
        
        PartDefinition body = modelPartData.addOrReplaceChild("body", 
            CubeListBuilder.create().texOffs(0, 8).addBox(-3.0F, -2.0F, -8.0F, 5.0F, 3.0F, 9.0F),
            PartPose.offsetAndRotation(0.0F, 1.0F, 0.0F, 0.1F, 0.0F, 0.0F));
        
        PartDefinition leftWingBase = body.addOrReplaceChild("left_wing_base",
            CubeListBuilder.create().texOffs(23, 12).addBox(0.0F, 0.0F, 0.0F, 6.0F, 2.0F, 9.0F),
            PartPose.offsetAndRotation(2.0F, -2.0F, -8.0F, 0.0F, 0.0F, 0.1F));
        
        leftWingBase.addOrReplaceChild("left_wing_tip",
            CubeListBuilder.create().texOffs(16, 24).addBox(0.0F, 0.0F, 0.0F, 13.0F, 1.0F, 9.0F),
            PartPose.offsetAndRotation(6.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.1F));
        
        PartDefinition rightWingBase = body.addOrReplaceChild("right_wing_base",
            CubeListBuilder.create().texOffs(23, 12).mirror().addBox(-6.0F, 0.0F, 0.0F, 6.0F, 2.0F, 9.0F),
            PartPose.offsetAndRotation(-3.0F, -2.0F, -8.0F, 0.0F, 0.0F, -0.1F));
        
        rightWingBase.addOrReplaceChild("right_wing_tip",
            CubeListBuilder.create().texOffs(16, 24).mirror().addBox(-13.0F, 0.0F, 0.0F, 13.0F, 1.0F, 9.0F),
            PartPose.offsetAndRotation(-6.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.1F));
        
        PartDefinition tailBase = body.addOrReplaceChild("tail_base",
            CubeListBuilder.create().texOffs(3, 20).addBox(-2.0F, 0.0F, 0.0F, 3.0F, 2.0F, 6.0F),
            PartPose.offsetAndRotation(-0.5F, -2.0F, 1.0F, 0.1F, 0.0F, 0.0F));
        tailBase.addOrReplaceChild("tail_tip",
                CubeListBuilder.create().texOffs(4, 29).addBox(-1.0F, 0.0F, 0.0F, 1.0F, 1.0F, 6.0F),
                PartPose.offsetAndRotation(0.0F, 0.5F, 6.0F, 0.1F, 0.0F, 0.0F));
        
        PartDefinition head = body.addOrReplaceChild("head",
            CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -2.0F, -5.0F, 7.0F, 3.0F, 5.0F),
            PartPose.offsetAndRotation(0.0F, 1.0F, -7.0F, 0.2F, 0.0F, 0.0F));
        
        return LayerDefinition.create(meshDefinition, 64, 64);
    }
    
}

