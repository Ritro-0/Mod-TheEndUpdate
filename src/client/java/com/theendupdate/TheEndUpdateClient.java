package com.theendupdate;

import com.theendupdate.client.particle.NebulaVentSmokeParticle;
import com.theendupdate.client.particle.QuantumSparkParticle;
import com.theendupdate.client.particle.TetherTrailParticle;
import com.theendupdate.client.render.NebulaVentBlockEntityRenderer;
import com.theendupdate.client.render.QuantumGatewayBlockEntityRenderer;
import com.theendupdate.client.render.SpectralBlockGlowRenderer;
import com.theendupdate.entity.model.EyesEntityModel;
import com.theendupdate.entity.model.EtherealOrbEntityModel;
import com.theendupdate.entity.model.KingPhantomEntityModel;
import com.theendupdate.entity.model.ShadowCreakingModel;
import com.theendupdate.entity.model.TetherlingEntityModel;
import com.theendupdate.entity.model.VoidTardigradeEntityModel;
import com.theendupdate.entity.renderer.EyesEntityRenderer;
import com.theendupdate.entity.renderer.EtherealOrbEntityRenderer;
import com.theendupdate.entity.renderer.KingPhantomEntityRenderer;
import com.theendupdate.entity.renderer.ShadowCreakingRenderer;
import com.theendupdate.entity.renderer.TetherlingEntityRenderer;
import com.theendupdate.entity.renderer.VoidTardigradeEntityRenderer;
import com.theendupdate.registry.ModBlockEntities;
import com.theendupdate.registry.ModEntities;
import com.theendupdate.registry.ModParticles;
import com.theendupdate.registry.ModScreenHandlers;
import com.theendupdate.screen.GatewayScreen;
import com.theendupdate.world.ShadowlandsBiomeIdentity;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.trim.ArmorTrim;

public class TheEndUpdateClient implements ClientModInitializer {
    private static int spectralTicker = 0;

    @Override
    public void onInitializeClient() {
        MenuScreens.register(ModScreenHandlers.GATEWAY, GatewayScreen::new);

        ModelLayerRegistry.registerModelLayer(EtherealOrbEntityModel.ETHEREAL_ORB_LAYER, EtherealOrbEntityModel::getTexturedModelData);
        ModelLayerRegistry.registerModelLayer(KingPhantomEntityModel.LAYER_LOCATION, KingPhantomEntityModel::getTexturedModelData);
        ModelLayerRegistry.registerModelLayer(VoidTardigradeEntityModel.LAYER_LOCATION, VoidTardigradeEntityModel::getLayerDefinition);
        ModelLayerRegistry.registerModelLayer(TetherlingEntityModel.LAYER_LOCATION, TetherlingEntityModel::getTexturedModelData);
        ModelLayerRegistry.registerModelLayer(EyesEntityModel.LAYER_LOCATION, EyesEntityModel::createBodyLayer);
        ModelLayerRegistry.registerModelLayer(ShadowCreakingModel.LAYER_LOCATION, ShadowCreakingModel::createBodyLayer);
        ModelLayerRegistry.registerModelLayer(ShadowCreakingModel.SHOULDERS_LAYER, ShadowCreakingModel::createShouldersLayer);

        ParticleProviderRegistry.getInstance().register(ModParticles.NEBULA_VENT_SMOKE, NebulaVentSmokeParticle.Factory::new);
        ParticleProviderRegistry.getInstance().register(ModParticles.TETHER_TRAIL, TetherTrailParticle.Factory::new);
        ParticleProviderRegistry.getInstance().register(ModParticles.QUANTUM_SPARK, QuantumSparkParticle.Factory::new);

        EntityRendererRegistry.register(ModEntities.ETHEREAL_ORB, EtherealOrbEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.KING_PHANTOM, KingPhantomEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.SHADOW_CREAKING, ShadowCreakingRenderer::new);
        EntityRendererRegistry.register(ModEntities.MINI_SHADOW_CREAKING, ShadowCreakingRenderer::new);
        EntityRendererRegistry.register(ModEntities.TINY_SHADOW_CREAKING, ShadowCreakingRenderer::new);
        EntityRendererRegistry.register(ModEntities.VOID_TARDIGRADE, VoidTardigradeEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.TETHERLING, TetherlingEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.EYES, EyesEntityRenderer::new);

        BlockEntityRenderers.register(ModBlockEntities.NEBULA_VENT, NebulaVentBlockEntityRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.QUANTUM_GATEWAY, QuantumGatewayBlockEntityRenderer::new);

        SpectralBlockGlowRenderer.register();
        SoundHooks.register();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client == null || client.level == null || client.player == null) return;
            if (client.isPaused()) return;

            theendupdate$spawnShadowlandsAshParticles(client);

            // lower cadence keeps the visual subtle
            if (spectralTicker > 0) {
                spectralTicker--;
                return;
            }
            spectralTicker = 7;

            // hide self-particles in first-person, show third-person only
            CameraType cameraType = client.options.getCameraType();
            if (cameraType == null || cameraType.isFirstPerson()) return;

            theendupdate$spawnLocalSpectralTrimParticles(client);
        });
    }

    private static void theendupdate$spawnLocalSpectralTrimParticles(Minecraft client) {
        var player = client.player;
        var level = client.level;
        if (player == null || level == null) return;

        double baseX = player.getX();
        double baseY = player.getY();
        double baseZ = player.getZ();
        double yawRad = Math.toRadians(player.getYRot());
        double forwardX = -Math.sin(yawRad);
        double forwardZ = Math.cos(yawRad);
        double forwardOffset = 0.28;

        for (EquipmentSlot slot : new EquipmentSlot[] { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET }) {
            ItemStack armor = player.getItemBySlot(slot);
            if (armor == null || armor.isEmpty()) continue;
            ArmorTrim trim = armor.get(DataComponents.TRIM);
            if (trim == null) continue;
            var matId = trim.material().unwrapKey().map(ResourceKey::identifier).orElse(null);
            if (matId == null) continue;
            String path = matId.getPath();
            if (!"spectral".equals(path) && !"spectral_cluster".equals(path)) continue;

            double yOffset = switch (slot) {
                case HEAD -> 1.55;
                case CHEST -> 1.10;
                case LEGS -> 0.75;
                case FEET -> 0.25;
                default -> 1.0;
            };

            double particleY = baseY + yOffset;
            double frontX = baseX + forwardX * forwardOffset;
            double frontZ = baseZ + forwardZ * forwardOffset;
            double backX = baseX - forwardX * forwardOffset;
            double backZ = baseZ - forwardZ * forwardOffset;

            level.addParticle(ParticleTypes.END_ROD, frontX, particleY, frontZ, 0.0, 0.0, 0.0);
            level.addParticle(ParticleTypes.END_ROD, backX, particleY, backZ, 0.0, 0.0, 0.0);
        }
    }

    private static void theendupdate$spawnShadowlandsAshParticles(Minecraft client) {
        var player = client.player;
        var level = client.level;
        if (player == null || level == null) {
            return;
        }
        if (!ShadowlandsBiomeIdentity.isShadowlands(level.getBiome(player.blockPosition()))) {
            return;
        }

        var random = level.getRandom();
        for (int i = 0; i < 14; i++) {
            if (random.nextFloat() > 0.55f) {
                continue;
            }
            double x = player.getX() + (random.nextDouble() - 0.5) * 28.0;
            double y = player.getY() + random.nextDouble() * 14.0;
            double z = player.getZ() + (random.nextDouble() - 0.5) * 28.0;
            level.addParticle(ParticleTypes.WHITE_ASH, x, y, z, 0.0, 0.0, 0.0);
        }
    }
}
