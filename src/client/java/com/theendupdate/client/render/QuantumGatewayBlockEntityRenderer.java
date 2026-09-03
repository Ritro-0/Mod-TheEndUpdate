package com.theendupdate.client.render;

import com.theendupdate.TheEndUpdate;
import com.theendupdate.block.QuantumGatewayBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Inner sparkle cubes tumble and bob together, the roof sparkle stays still
 * and only gets the spectral-style glow overlay
 */
public class QuantumGatewayBlockEntityRenderer implements BlockEntityRenderer<QuantumGatewayBlockEntity, QuantumGatewayRenderState> {
    private static final Identifier SPECTRAL_TEXTURE = Identifier.fromNamespaceAndPath(
        TheEndUpdate.MOD_ID, "textures/block/spectral_block.png"
    );
    private static final RenderType SOLID_LAYER = RenderTypes.entityCutout(SPECTRAL_TEXTURE);
    private static final RenderType GLOW_LAYER = RenderTypes.eyes(SPECTRAL_TEXTURE);
    private static final int FULL_BRIGHT = 0xF000F0;

    // pixel-space centers / sizes from the Blockbench model
    private static final float INNER_Y = 6.0f / 16.0f;
    private static final float ROOF_Y = 13.5f / 16.0f;
    private static final float[][] INNER_BASE_ROT = {
        { -42.5f, 0.0f, -45.0f },
        { 45.0f, 0.0f, 45.0f }
    };

    private static final ModelPart INNER_CUBE = createCube(-1.0f, -1.0f, -1.0f, 2.0f, 2.0f, 2.0f, 0.0f, 0, 0);
    private static final ModelPart INNER_GLOW = createCube(-1.0f, -1.0f, -1.0f, 2.0f, 2.0f, 2.0f, 0.15f, 0, 0);
    private static final ModelPart ROOF_GLOW = createCube(-2.0f, -0.5f, -2.0f, 4.0f, 1.0f, 4.0f, 0.2f, 10, 10);

    public QuantumGatewayBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    private static ModelPart createCube(
        float x,
        float y,
        float z,
        float sizeX,
        float sizeY,
        float sizeZ,
        float inflate,
        int u,
        int v
    ) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild(
            "cube",
            CubeListBuilder.create()
                .texOffs(u, v)
                .addBox(x, y, z, sizeX, sizeY, sizeZ, new CubeDeformation(inflate)),
            PartPose.ZERO
        );
        return LayerDefinition.create(mesh, 16, 16).bakeRoot();
    }

    @Override
    public QuantumGatewayRenderState createRenderState() {
        return new QuantumGatewayRenderState();
    }

    @Override
    public void extractRenderState(
        QuantumGatewayBlockEntity blockEntity,
        QuantumGatewayRenderState state,
        float tickProgress,
        Vec3 cameraPos,
        @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, tickProgress, cameraPos, crumblingOverlay);
        state.bob = blockEntity.getBob(tickProgress);
        for (int i = 0; i < QuantumGatewayBlockEntity.MOVING_SPARKLES; i++) {
            state.rotX[i] = blockEntity.getSparkleRotX(i, tickProgress);
            state.rotY[i] = blockEntity.getSparkleRotY(i, tickProgress);
            state.rotZ[i] = blockEntity.getSparkleRotZ(i, tickProgress);
        }
    }

    @Override
    public void submit(
        QuantumGatewayRenderState state,
        PoseStack matrices,
        SubmitNodeCollector queue,
        CameraRenderState cameraState
    ) {
        for (int i = 0; i < QuantumGatewayBlockEntity.MOVING_SPARKLES; i++) {
            matrices.pushPose();
            matrices.translate(0.5f, INNER_Y + state.bob, 0.5f);
            matrices.mulPose(Axis.XP.rotationDegrees(INNER_BASE_ROT[i][0] + state.rotX[i]));
            matrices.mulPose(Axis.YP.rotationDegrees(INNER_BASE_ROT[i][1] + state.rotY[i]));
            matrices.mulPose(Axis.ZP.rotationDegrees(INNER_BASE_ROT[i][2] + state.rotZ[i]));
            submitCube(queue, matrices, INNER_CUBE, SOLID_LAYER, FULL_BRIGHT, state);
            submitCube(queue, matrices, INNER_GLOW, GLOW_LAYER, FULL_BRIGHT, state);
            matrices.popPose();
        }

        matrices.pushPose();
        matrices.translate(0.5f, ROOF_Y, 0.5f);
        submitCube(queue, matrices, ROOF_GLOW, GLOW_LAYER, FULL_BRIGHT, state);
        matrices.popPose();
    }

    private static void submitCube(
        SubmitNodeCollector queue,
        PoseStack matrices,
        ModelPart part,
        RenderType layer,
        int light,
        QuantumGatewayRenderState state
    ) {
        queue.submitModelPart(
            part,
            matrices,
            layer,
            light,
            OverlayTexture.NO_OVERLAY,
            null,
            -1,
            state.breakProgress
        );
    }
}
