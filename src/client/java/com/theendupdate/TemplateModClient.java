package com.theendupdate;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;

import com.theendupdate.entity.model.EtherealOrbEntityModel;
import com.theendupdate.entity.renderer.EtherealOrbEntityRenderer;
import com.theendupdate.entity.renderer.ShadowCreakingEntityRenderer;
import com.theendupdate.registry.ModBlocks;
import com.theendupdate.registry.ModEntities;

// The correct imports for Fabric 1.21.8
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.BlockRenderLayer; // This was the key!
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.Identifier;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
// predicate registration not needed since models use built-in trim_type predicate
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.equipment.trim.ArmorTrim;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
// predicate registration removed; compass logic handled via mixins

@Environment(EnvType.CLIENT)  
public class TemplateModClient implements ClientModInitializer {
    public static final EntityModelLayer MODEL_ETHEREAL_ORB_LAYER = new EntityModelLayer(Identifier.of(TemplateMod.MOD_ID, "ethereal_orb"), "main");

    @Override
    public void onInitializeClient() 
    {
        // TODO: Add entity renderer when implementing custom renderer for 1.21.8
        // EntityRendererRegistry.register(ModEntities.ETHEREAL_ORB, EtherealOrbEntityRenderer::new);
        // EntityModelLayerRegistry.registerModelLayer(EtherealOrbEntityModel.ETHEREAL_ORB_LAYER, EtherealOrbEntityModel::getTexturedModelData);
        
        // Register transparent blocks
        BlockRenderLayerMap.putBlock(ModBlocks.VOID_BLOOM, BlockRenderLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModBlocks.ENDER_CHRYSANTHEMUM, BlockRenderLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModBlocks.VOID_SAP, BlockRenderLayer.CUTOUT);
        // Tendril plants (crossed planes)
        BlockRenderLayerMap.putBlock(ModBlocks.TENDRIL_SPROUT, BlockRenderLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModBlocks.TENDRIL_THREAD, BlockRenderLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModBlocks.TENDRIL_CORE, BlockRenderLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModBlocks.MOLD_CRAWL, BlockRenderLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModBlocks.MOLD_SPORE, BlockRenderLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModBlocks.MOLD_SPORE_TUFT, BlockRenderLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModBlocks.MOLD_SPORE_SPROUT, BlockRenderLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModBlocks.SHADOW_CLAW, BlockRenderLayer.CUTOUT);
        // Wooden transparent parts
        BlockRenderLayerMap.putBlock(ModBlocks.ETHEREAL_DOOR, BlockRenderLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModBlocks.ETHEREAL_TRAPDOOR, BlockRenderLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModBlocks.SHADOW_DOOR, BlockRenderLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModBlocks.SHADOW_TRAPDOOR, BlockRenderLayer.CUTOUT);
        // Quantum gateway uses glass-like rendering; translucent looks better for alpha
        BlockRenderLayerMap.putBlock(ModBlocks.QUANTUM_GATEWAY, BlockRenderLayer.TRANSLUCENT);
        // Entity Initialization
        EntityModelLayerRegistry.registerModelLayer(MODEL_ETHEREAL_ORB_LAYER, EtherealOrbEntityModel :: getTexturedModelData);
        EntityRendererRegistry.register(ModEntities.ETHEREAL_ORB, (context) -> new EtherealOrbEntityRenderer(context));
        // Shadow Creaking renderer (uses vanilla creaking model layer, inverted)
        EntityRendererRegistry.register(ModEntities.SHADOW_CREAKING, ShadowCreakingEntityRenderer::new);
        // Register custom screen for Quantum Gateway
        HandledScreens.register(com.theendupdate.registry.ModScreenHandlers.GATEWAY, com.theendupdate.screen.GatewayScreen::new);

        // Visual-only top handled via model geometry extending into y+1 (no extra block used)

        // Client init complete
        // cleaned debug log

        // Item models override using built-in trim_type; no explicit predicate registration required

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            try {
                MinecraftClient mc = client;
                if (mc == null || mc.world == null || mc.player == null) return;
                if (mc.options.getPerspective() == Perspective.FIRST_PERSON) return; // hide in first-person only
                if (mc.world.getTime() % 10 != 0) return; // match server cadence

                var player = mc.player;
                boolean hasHead = theendupdate$isSpectralTrimClient(player.getEquippedStack(EquipmentSlot.HEAD));
                boolean hasChest = theendupdate$isSpectralTrimClient(player.getEquippedStack(EquipmentSlot.CHEST));
                boolean hasLegs = theendupdate$isSpectralTrimClient(player.getEquippedStack(EquipmentSlot.LEGS));
                boolean hasFeet = theendupdate$isSpectralTrimClient(player.getEquippedStack(EquipmentSlot.FEET));

                int spectralPieces = 0;
                if (hasHead) spectralPieces++;
                if (hasChest) spectralPieces++;
                if (hasLegs) spectralPieces++;
                if (hasFeet) spectralPieces++;
                if (spectralPieces <= 0) return;

                float spawnChance = switch (spectralPieces) {
                    case 1 -> 0.5f;
                    case 2 -> 0.7f;
                    case 3 -> 0.85f;
                    default -> 1.0f;
                };
                if (mc.world.random.nextFloat() > spawnChance) return;

                double yawRad = Math.toRadians(player.getYaw());
                double fwdX = -Math.sin(yawRad);
                double fwdZ =  Math.cos(yawRad);
                double rightX =  Math.cos(yawRad);
                double rightZ =  Math.sin(yawRad);
                boolean phase = ((mc.world.getTime() / 20) % 2) == 0;

                if (hasChest) {
                    double baseY = player.getY() + 1.30;
                    double fbMag = 0.85;
                    double lrMag = 0.45;
                    double yMag  = 0.28;
                    double offX = (phase ? fwdX : -fwdX) * fbMag;
                    double offZ = (phase ? fwdZ : -fwdZ) * fbMag;
                    boolean lateralLeft = mc.world.random.nextBoolean();
                    double latScale = lrMag * mc.world.random.nextDouble();
                    double latX = (lateralLeft ? -rightX : rightX) * latScale;
                    double latZ = (lateralLeft ? -rightZ : rightZ) * latScale;
                    double spreadFB = (mc.world.random.nextDouble() - 0.5) * 0.6;
                    double sprX = fwdX * spreadFB;
                    double sprZ = fwdZ * spreadFB;
                    double x = player.getX() + offX + latX + sprX + (mc.world.random.nextDouble() - 0.5) * 0.14;
                    double y = baseY + (mc.world.random.nextDouble() - 0.5) * yMag;
                    double z = player.getZ() + offZ + latZ + sprZ + (mc.world.random.nextDouble() - 0.5) * 0.14;
                    mc.particleManager.addParticle(ParticleTypes.END_ROD, x, y, z, 0.0, 0.0, 0.0);
                }

                if (hasHead) {
                    double baseY = player.getEyeY() + 0.00;
                    double fbMag = 0.55;
                    double lrMag = 0.30;
                    double yMag  = 0.22;
                    double offX = (phase ? fwdX : -fwdX) * fbMag;
                    double offZ = (phase ? fwdZ : -fwdZ) * fbMag;
                    boolean lateralLeft = mc.world.random.nextBoolean();
                    double latScale = lrMag * mc.world.random.nextDouble();
                    double latX = (lateralLeft ? -rightX : rightX) * latScale;
                    double latZ = (lateralLeft ? -rightZ : rightZ) * latScale;
                    double spreadFB = (mc.world.random.nextDouble() - 0.5) * 0.5;
                    double sprX = fwdX * spreadFB;
                    double sprZ = fwdZ * spreadFB;
                    double x = player.getX() + offX + latX + sprX + (mc.world.random.nextDouble() - 0.5) * 0.14;
                    double y = baseY + (mc.world.random.nextDouble() - 0.5) * yMag;
                    double z = player.getZ() + offZ + latZ + sprZ + (mc.world.random.nextDouble() - 0.5) * 0.14;
                    mc.particleManager.addParticle(ParticleTypes.END_ROD, x, y, z, 0.0, 0.0, 0.0);
                }

                if (hasLegs) {
                    double baseY = player.getY() + 0.95;
                    double lrMag = 0.55;
                    double fbMag = 0.35;
                    double yMag  = 0.18;
                    double offX = (phase ? rightX : -rightX) * lrMag;
                    double offZ = (phase ? rightZ : -rightZ) * lrMag;
                    boolean forwardSide = mc.world.random.nextBoolean();
                    double swayScale = fbMag * mc.world.random.nextDouble();
                    double swayX = (forwardSide ? fwdX : -fwdX) * swayScale;
                    double swayZ = (forwardSide ? fwdZ : -fwdZ) * swayScale;
                    double x = player.getX() + offX + swayX + (mc.world.random.nextDouble() - 0.5) * 0.16;
                    double y = baseY + (mc.world.random.nextDouble() - 0.5) * yMag;
                    double z = player.getZ() + offZ + swayZ + (mc.world.random.nextDouble() - 0.5) * 0.16;
                    mc.particleManager.addParticle(ParticleTypes.END_ROD, x, y, z, 0.0, 0.0, 0.0);
                }

                if (hasFeet) {
                    double baseY = player.getY() + 0.25;
                    double lrMag = 0.45;
                    double fbMag = 0.28;
                    double yMag  = 0.14;
                    double offX = (phase ? rightX : -rightX) * lrMag;
                    double offZ = (phase ? rightZ : -rightZ) * lrMag;
                    boolean forwardSide = mc.world.random.nextBoolean();
                    double swayScale = fbMag * mc.world.random.nextDouble();
                    double swayX = (forwardSide ? fwdX : -fwdX) * swayScale;
                    double swayZ = (forwardSide ? fwdZ : -fwdZ) * swayScale;
                    double x = player.getX() + offX + swayX + (mc.world.random.nextDouble() - 0.5) * 0.14;
                    double y = baseY + (mc.world.random.nextDouble() - 0.5) * yMag;
                    double z = player.getZ() + offZ + swayZ + (mc.world.random.nextDouble() - 0.5) * 0.14;
                    mc.particleManager.addParticle(ParticleTypes.END_ROD, x, y, z, 0.0, 0.0, 0.0);
                }
            } catch (Throwable ignored) {}
        });

    }

    private static boolean theendupdate$isSpectralTrimClient(ItemStack armor) {
        try {
            if (armor == null || armor.isEmpty()) return false;
            ArmorTrim trim = armor.get(DataComponentTypes.TRIM);
            if (trim == null) return false;
            Identifier matId = trim.material().getKey().map(RegistryKey::getValue).orElse(null);
            if (matId == null) return false;
            String path = matId.getPath();
            return "spectral".equals(path) || "spectral_cluster".equals(path);
        } catch (Throwable ignored) {}
        return false;
    }

}


