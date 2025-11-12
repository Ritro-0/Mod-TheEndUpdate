package com.theendupdate.entity.model;

import com.theendupdate.TemplateMod;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.PhantomEntityModel;
import net.minecraft.client.render.entity.state.PhantomEntityRenderState;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

/**
 * Custom model for the King Phantom entity that extends the vanilla phantom model
 * and adds a crown on top of the head.
 */
public class KingPhantomEntityModel extends PhantomEntityModel {
    
    public static final EntityModelLayer LAYER_LOCATION = new EntityModelLayer(
        Identifier.of(TemplateMod.MOD_ID, "king_phantom"), "main");
    
    // Store references to wing parts for manual animation
    private final ModelPart leftWingBase;
    private final ModelPart leftWingTip;
    private final ModelPart rightWingBase;
    private final ModelPart rightWingTip;
    private final ModelPart tailBase;
    private final ModelPart tailTip;
    private final ModelPart body;
    
    public KingPhantomEntityModel(ModelPart root) {
        super(root);
        
        // Get references to wing parts for animation
        this.body = root.getChild("body");
        this.leftWingBase = body.getChild("left_wing_base");
        this.leftWingTip = leftWingBase.getChild("left_wing_tip");
        this.rightWingBase = body.getChild("right_wing_base");
        this.rightWingTip = rightWingBase.getChild("right_wing_tip");
        this.tailBase = body.getChild("tail_base");
        this.tailTip = tailBase.getChild("tail_tip");
    }
    
    @Override
    public void setAngles(PhantomEntityRenderState state) {
        // Call parent - vanilla PhantomEntityModel handles pitch rotation
        super.setAngles(state);
        
        // Manually animate wings to ensure they always flap
        // Use the age field from render state for smooth time-based animation
        float ageInTicks = state.age;
        
        // Wing flapping animation (similar to vanilla phantom)
        // The wings flap in a sine wave pattern
        float flapCycle = ageInTicks * 2.0f; // Speed of wing flap (4x faster)
        float flapAngle = MathHelper.cos(flapCycle * 0.35f) * (float) Math.PI * 0.15f;
        
        // Animate wing base (main wing movement)
        this.leftWingBase.roll = 0.1f + flapAngle;
        this.rightWingBase.roll = -0.1f - flapAngle;
        
        // Animate wing tips (follow the base with slight offset for more natural movement)
        float tipOffset = MathHelper.cos(flapCycle * 0.35f + 0.3f) * (float) Math.PI * 0.1f;
        this.leftWingTip.roll = 0.1f + tipOffset;
        this.rightWingTip.roll = -0.1f - tipOffset;
        
        // Add subtle tail movement for more liveliness (also faster)
        float tailCycle = ageInTicks * 1.2f;
        this.tailBase.yaw = MathHelper.cos(tailCycle * 0.3f) * 0.1f;
        this.tailTip.yaw = MathHelper.cos(tailCycle * 0.3f + 0.5f) * 0.15f;
        
        // Pitch rotation is now handled in the renderer via MatrixStack
        // This is more reliable than trying to modify model parts
    }
    
    /**
     * Creates the textured model data for the King Phantom, including the crown.
     * This builds on top of the vanilla phantom model.
     */
    public static TexturedModelData getTexturedModelData() {
        // Start with the vanilla phantom model data
        ModelData modelData = new ModelData();
        ModelPartData modelPartData = modelData.getRoot();
        
        // Create the phantom body parts (same as vanilla)
        ModelPartData body = modelPartData.addChild("body", 
            ModelPartBuilder.create().uv(0, 8).cuboid(-3.0F, -2.0F, -8.0F, 5.0F, 3.0F, 9.0F),
            ModelTransform.of(0.0F, 1.0F, 0.0F, 0.1F, 0.0F, 0.0F));
        
        ModelPartData leftWingBase = body.addChild("left_wing_base",
            ModelPartBuilder.create().uv(23, 12).cuboid(0.0F, 0.0F, 0.0F, 6.0F, 2.0F, 9.0F),
            ModelTransform.of(2.0F, -2.0F, -8.0F, 0.0F, 0.0F, 0.1F));
        
        leftWingBase.addChild("left_wing_tip",
            ModelPartBuilder.create().uv(16, 24).cuboid(0.0F, 0.0F, 0.0F, 13.0F, 1.0F, 9.0F),
            ModelTransform.of(6.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.1F));
        
        ModelPartData rightWingBase = body.addChild("right_wing_base",
            ModelPartBuilder.create().uv(23, 12).mirrored().cuboid(-6.0F, 0.0F, 0.0F, 6.0F, 2.0F, 9.0F),
            ModelTransform.of(-3.0F, -2.0F, -8.0F, 0.0F, 0.0F, -0.1F));
        
        rightWingBase.addChild("right_wing_tip",
            ModelPartBuilder.create().uv(16, 24).mirrored().cuboid(-13.0F, 0.0F, 0.0F, 13.0F, 1.0F, 9.0F),
            ModelTransform.of(-6.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.1F));
        
        body.addChild("tail_base",
            ModelPartBuilder.create().uv(3, 20).cuboid(-2.0F, 0.0F, 0.0F, 3.0F, 2.0F, 6.0F),
            ModelTransform.of(-0.5F, -2.0F, 1.0F, 0.1F, 0.0F, 0.0F))
            .addChild("tail_tip",
                ModelPartBuilder.create().uv(4, 29).cuboid(-1.0F, 0.0F, 0.0F, 1.0F, 1.0F, 6.0F),
                ModelTransform.of(0.0F, 0.5F, 6.0F, 0.1F, 0.0F, 0.0F));
        
        // Create the head (same as vanilla)
        ModelPartData head = body.addChild("head",
            ModelPartBuilder.create().uv(0, 0).cuboid(-4.0F, -2.0F, -5.0F, 7.0F, 3.0F, 5.0F),
            ModelTransform.of(0.0F, 1.0F, -7.0F, 0.2F, 0.0F, 0.0F));
        
        // No crown - removed as requested
        
        return TexturedModelData.of(modelData, 64, 64);
    }
    
}

