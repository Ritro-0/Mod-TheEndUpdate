package com.theendupdate.client.render;

import com.theendupdate.TheEndUpdate;
import com.theendupdate.block.entity.NebulaVentBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
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

/** Draws the center vent cube and spins it, the rim stays in the normal block model */
public class NebulaVentBlockEntityRenderer implements BlockEntityRenderer<NebulaVentBlockEntity, NebulaVentRenderState> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
        TheEndUpdate.MOD_ID, "textures/block/nebula_vent.png"
    );
    private static final RenderType LAYER = RenderTypes.entityCutout(TEXTURE);
    // matches the original JSON cube, which started rotated 45 degrees in the well
    private static final float BASE_ANGLE = -45.0f;
    private static final ModelPart VENT = createVentCube();

    public NebulaVentBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    private static ModelPart createVentCube() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild(
            "vent",
            CubeListBuilder.create()
                .texOffs(4, 4)
                .addBox(4.0F, 0.0F, 4.0F, 8.0F, 3.0F, 8.0F),
            PartPose.ZERO
        );
        return LayerDefinition.create(mesh, 16, 16).bakeRoot();
    }

    @Override
    public NebulaVentRenderState createRenderState() {
        return new NebulaVentRenderState();
    }

    @Override
    public void extractRenderState(
        NebulaVentBlockEntity blockEntity,
        NebulaVentRenderState state,
        float tickProgress,
        Vec3 cameraPos,
        @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, tickProgress, cameraPos, crumblingOverlay);
        state.spinAngle = blockEntity.getSpinAngle(tickProgress);
    }

    @Override
    public void submit(
        NebulaVentRenderState state,
        PoseStack matrices,
        SubmitNodeCollector queue,
        CameraRenderState cameraState
    ) {
        matrices.pushPose();
        matrices.translate(0.5f, 1.5f / 16.0f, 0.5f);
        matrices.mulPose(Axis.YP.rotationDegrees(BASE_ANGLE + state.spinAngle));
        matrices.translate(-0.5f, -1.5f / 16.0f, -0.5f);
        queue.submitModelPart(
            VENT,
            matrices,
            LAYER,
            state.lightCoords,
            OverlayTexture.NO_OVERLAY,
            null,
            -1,
            state.breakProgress
        );
        matrices.popPose();
    }
}
