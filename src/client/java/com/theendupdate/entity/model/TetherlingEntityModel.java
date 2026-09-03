package com.theendupdate.entity.model;

import com.theendupdate.TheEndUpdate;
import com.theendupdate.entity.state.TetherlingRenderState;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Blockbench export (Tetherling.bbmodel), adapted for render states and tentacle posing */
public class TetherlingEntityModel extends EntityModel<TetherlingRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(TheEndUpdate.MOD_ID, "tetherling"),
        "main"
    );

    private final ModelPart bb_main;
    private final ModelPart tail;
    private final ModelPart frontLeftLeg;
    private final ModelPart frontRightLeg;
    private final ModelPart backLeftLeg;
    private final ModelPart backRightLeg;
    private final ModelPart frontMiddleLeg;
    private final ModelPart backMiddleLeg;

    public TetherlingEntityModel(ModelPart root) {
        super(root);
        this.bb_main = root.getChild("bb_main");
        this.tail = this.bb_main.getChild("Tail_r1");
        this.frontLeftLeg = this.bb_main.getChild("FrontLeftLeg_r1");
        this.frontRightLeg = this.bb_main.getChild("FrontRightLeg_r1");
        this.backLeftLeg = this.bb_main.getChild("BackLeftLeg_r1");
        this.backRightLeg = this.bb_main.getChild("BackRightLeg_r1");
        this.frontMiddleLeg = this.bb_main.getChild("FrontMiddleLeg_r1");
        this.backMiddleLeg = this.bb_main.getChild("BackMiddleLeg_r1");
    }

    public static LayerDefinition getTexturedModelData() {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition root = meshDefinition.getRoot();
        PartDefinition bb_main = root.addOrReplaceChild("bb_main", CubeListBuilder.create().texOffs(0, 20).addBox(-7.0F, -19.0F, -2.0F, 14.0F, 9.0F, 8.0F, new CubeDeformation(0.0F))
            .texOffs(0, 0).addBox(-6.0F, -17.0F, -7.0F, 12.0F, 5.0F, 15.0F, new CubeDeformation(0.0F))
            .texOffs(0, 37).addBox(-3.0F, -19.0F, -10.0F, 6.0F, 5.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, 0.0F, 0.0F, 0.0F));

        bb_main.addOrReplaceChild("Tail_r1", CubeListBuilder.create().texOffs(44, 29).addBox(-1.0F, -6.0F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -15.0F, 7.0F, -0.6981F, 0.0F, 0.0F));

        bb_main.addOrReplaceChild("BackMiddleLeg_r1", CubeListBuilder.create().texOffs(44, 42).addBox(-1.0016F, -0.0001F, 0.0242F, 2.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -11.0F, 4.0F, -0.0121F, 0.0072F, -0.0008F));

        bb_main.addOrReplaceChild("FrontMiddleLeg_r1", CubeListBuilder.create().texOffs(44, 37).addBox(-1.0016F, -0.0001F, 0.0242F, 2.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -11.0F, 0.0F, -0.0121F, 0.0072F, -0.0008F));

        bb_main.addOrReplaceChild("BackRightLeg_r1", CubeListBuilder.create().texOffs(44, 20).addBox(-1.5422F, -0.6939F, -0.9951F, 2.0F, 7.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.0F, -10.0F, 4.0F, -0.0084F, 0.0113F, 0.3919F));

        bb_main.addOrReplaceChild("FrontRightLeg_r1", CubeListBuilder.create().texOffs(36, 37).addBox(-1.5422F, -0.6939F, -0.9951F, 2.0F, 7.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.0F, -10.0F, 0.0F, -0.0084F, 0.0113F, 0.3919F));

        bb_main.addOrReplaceChild("BackLeftLeg_r1", CubeListBuilder.create().texOffs(28, 37).addBox(-0.4588F, -0.6934F, -1.0F, 2.0F, 7.0F, 2.0F, new CubeDeformation(0.0F))
            .texOffs(20, 37).addBox(-0.4588F, -0.6934F, -5.0F, 2.0F, 7.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(6.0F, -10.0F, 4.0F, 0.0F, 0.0F, -0.3927F));

        bb_main.addOrReplaceChild("FrontLeftLeg_r1", CubeListBuilder.create().texOffs(44, 20).addBox(-0.4588F, -0.6934F, -1.0F, 2.0F, 7.0F, 2.0F, new CubeDeformation(0.0F))
            .texOffs(20, 37).addBox(-0.4588F, -0.6934F, -5.0F, 2.0F, 7.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(6.0F, -10.0F, 0.0F, 0.0F, 0.0F, -0.3927F));

        return LayerDefinition.create(meshDefinition, 64, 64);
    }

    @Override
    public void setupAnim(TetherlingRenderState state) {
        for (ModelPart part : this.bb_main.getAllParts()) {
            part.resetPose();
        }
        
        float extend = state.tentacleExtend;
        float yeet = Mth.clamp(state.tentacleYeet, 0.0F, 1.0F);
        
        float rawStretch = 1.0F + extend * 2.0F; 
        float baseStretch = Math.min(rawStretch, 2.5F);
        float yeetBoost = Math.min(yeet * 1.2F, 0.5F);
        
        float frontLength = Math.min(baseStretch + yeetBoost, 2.5F);
        float backLength = Math.min(baseStretch * 0.9F + yeetBoost * 0.7F, 2.3F);
        float midLength = Math.min(baseStretch * 0.8F + yeetBoost * 0.5F, 2.1F);
        
        // positive pitch = down, negative = up - tentacles reach up/forward toward player
        float reachPitch = -extend * 1.5F - yeet * 0.8F;
        float reachRoll = extend * 0.5F + yeet * 0.3F;
        
        this.frontLeftLeg.xRot = reachPitch;
        this.frontRightLeg.xRot = reachPitch;
        this.frontLeftLeg.zRot = -0.3927F - reachRoll;
        this.frontRightLeg.zRot = 0.3919F + reachRoll;
        this.frontLeftLeg.yScale = frontLength;
        this.frontRightLeg.yScale = frontLength;
        
        this.backLeftLeg.xRot = reachPitch * 0.8F;
        this.backRightLeg.xRot = reachPitch * 0.8F;
        this.backLeftLeg.zRot = -0.3927F - reachRoll * 0.8F;
        this.backRightLeg.zRot = 0.3919F + reachRoll * 0.8F;
        this.backLeftLeg.yScale = backLength;
        this.backRightLeg.yScale = backLength;
        
        this.frontMiddleLeg.xRot = reachPitch * 0.9F;
        this.backMiddleLeg.xRot = reachPitch * 0.7F;
        this.frontMiddleLeg.yScale = midLength;
        this.backMiddleLeg.yScale = midLength;
        
        this.tail.xRot = -0.6981F - extend * 0.5F - yeet * 0.4F;
        
        // extend > 1.0 means reaching past normal range, lean the body into it
        if (extend > 1.0F) {
            this.bb_main.xRot = -extend * 0.1F;
        }
    }
}
