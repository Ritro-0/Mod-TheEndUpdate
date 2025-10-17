package com.theendupdate;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;

import com.theendupdate.entity.model.EtherealOrbEntityModel;
import com.theendupdate.entity.model.KingPhantomEntityModel;
import com.theendupdate.entity.renderer.EtherealOrbEntityRenderer;
import com.theendupdate.entity.renderer.KingPhantomEntityRenderer;
import com.theendupdate.entity.renderer.ShadowCreakingEntityRenderer;
import com.theendupdate.registry.ModBlocks;
import com.theendupdate.registry.ModBlockEntities;
import com.theendupdate.registry.ModEntities;

// The correct imports for Fabric 1.21.8
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.BlockRenderLayer; // This was the key!
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.minecraft.client.render.block.entity.SignBlockEntityRenderer;
import net.minecraft.client.render.block.entity.HangingSignBlockEntityRenderer;
import net.minecraft.client.render.block.entity.ShelfBlockEntityRenderer;
import net.minecraft.client.render.block.entity.BedBlockEntityRenderer;
import net.minecraft.util.Identifier;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
// predicate registration not needed since models use built-in trim_type predicate
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.equipment.trim.ArmorTrim;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.Difficulty;
// predicate registration removed; compass logic handled via mixins

@Environment(EnvType.CLIENT)  
public class TemplateModClient implements ClientModInitializer {
    
    public static final EntityModelLayer MODEL_ETHEREAL_ORB_LAYER = new EntityModelLayer(Identifier.of(TemplateMod.MOD_ID, "ethereal_orb"), "main");

    @Override
    public void onInitializeClient() {
        // Register transparent blocks
        BlockRenderLayerMap.putBlock(ModBlocks.VOID_BLOOM, BlockRenderLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModBlocks.ENDER_CHRYSANTHEMUM, BlockRenderLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModBlocks.VOID_SAP, BlockRenderLayer.CUTOUT);
        // Potted variants must also be cutout
        BlockRenderLayerMap.putBlock(ModBlocks.POTTED_SHADOW_CLAW, BlockRenderLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModBlocks.POTTED_VOID_BLOOM, BlockRenderLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModBlocks.POTTED_ENDER_CHRYSANTHEMUM, BlockRenderLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModBlocks.POTTED_TENDRIL_SPROUT, BlockRenderLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModBlocks.POTTED_TENDRIL_THREAD, BlockRenderLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModBlocks.POTTED_TENDRIL_CORE, BlockRenderLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModBlocks.POTTED_MOLD_SPORE, BlockRenderLayer.CUTOUT);
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
        // Shadow altar uses cutout layer for spawner-like interior visibility
        BlockRenderLayerMap.putBlock(ModBlocks.SHADOW_ALTAR, BlockRenderLayer.CUTOUT);
        // Shelves need cutout for proper rendering
        BlockRenderLayerMap.putBlock(ModBlocks.ETHEREAL_SHELF, BlockRenderLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModBlocks.SHADOW_SHELF, BlockRenderLayer.CUTOUT);
        
        // Register tooltip callback for spawn eggs to show "Disabled in Peaceful"
        ItemTooltipCallback.EVENT.register((stack, tooltipContext, tooltipType, lines) -> {
            if (stack.getItem() instanceof com.theendupdate.item.CustomSpawnEggItem spawnEgg) {
                // Check if it's a hostile mob spawn egg
                if (spawnEgg.getEntityType().getSpawnGroup() == SpawnGroup.MONSTER) {
                    // Check if world is in peaceful mode using MinecraftClient
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client != null && client.world != null && 
                        client.world.getDifficulty() == Difficulty.PEACEFUL) {
                        lines.add(Text.translatable("item.disabled_in_peaceful").formatted(Formatting.RED));
                    }
                }
            }
        });
        
        // Quantum Gateway wavy beacon beam is handled via BeaconBlockEntityRendererMixin
        
        // Entity Initialization
        // Entity renderers updated for 1.21.10 rendering API (OrderedRenderCommandQueue)
        EntityModelLayerRegistry.registerModelLayer(MODEL_ETHEREAL_ORB_LAYER, EtherealOrbEntityModel :: getTexturedModelData);
        EntityRendererRegistry.register(ModEntities.ETHEREAL_ORB, (context) -> new EtherealOrbEntityRenderer(context));
        // King Phantom renderer (custom phantom model, scaled 4x)
        EntityModelLayerRegistry.registerModelLayer(KingPhantomEntityModel.LAYER_LOCATION, KingPhantomEntityModel::getTexturedModelData);
        EntityRendererRegistry.register(ModEntities.KING_PHANTOM, KingPhantomEntityRenderer::new);
        // Shadow Creaking renderer (uses vanilla creaking model layer, inverted)
        EntityRendererRegistry.register(ModEntities.SHADOW_CREAKING, ShadowCreakingEntityRenderer::new);
        // Reuse same renderer for mini/tiny (uses scaled dimensions)
        EntityRendererRegistry.register(ModEntities.MINI_SHADOW_CREAKING, ShadowCreakingEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.TINY_SHADOW_CREAKING, ShadowCreakingEntityRenderer::new);
        // Register custom screen for Quantum Gateway
        HandledScreens.register(com.theendupdate.registry.ModScreenHandlers.GATEWAY, com.theendupdate.screen.GatewayScreen::new);

        // Hanging signs use vanilla renderer and block entity type - no custom registration needed
        
        // Register shelf block entity renderers - uses vanilla renderer
        BlockEntityRendererFactories.register(ModBlockEntities.ETHEREAL_SHELF, ShelfBlockEntityRenderer::new);
        BlockEntityRendererFactories.register(ModBlockEntities.SHADOW_SHELF, ShelfBlockEntityRenderer::new);

        // Visual-only top handled via model geometry extending into y+1 (no extra block used)

        // Client init complete
        // cleaned debug log

        // Item models override using built-in trim_type; no explicit predicate registration required

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            try {
                MinecraftClient mc = client;
                if (mc == null || mc.world == null || mc.player == null) return;
                if (mc.world.getTime() % 10 != 0) return; // match server cadence

                // Handle player spectral armor particles (hide in first-person)
                if (mc.options.getPerspective() != Perspective.FIRST_PERSON) {
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

                    if (spectralPieces > 0) {
                        float spawnChance = switch (spectralPieces) {
                            case 1 -> 0.5f;
                            case 2 -> 0.7f;
                            case 3 -> 0.85f;
                            default -> 1.0f;
                        };
                        if (mc.world.random.nextFloat() <= spawnChance) {
                            theendupdate$spawnParticlesForEntity(mc, player, hasHead, hasChest, hasLegs, hasFeet, spectralPieces);
                        }
                    }
                }

                // Handle armor stand spectral armor particles
                for (net.minecraft.entity.decoration.ArmorStandEntity armorStand : mc.world.getEntitiesByClass(
                        net.minecraft.entity.decoration.ArmorStandEntity.class,
                        mc.player.getBoundingBox().expand(32.0),
                        entity -> entity != null && entity.isAlive())) {
                    
                    boolean hasHead = theendupdate$isSpectralTrimClient(armorStand.getEquippedStack(EquipmentSlot.HEAD));
                    boolean hasChest = theendupdate$isSpectralTrimClient(armorStand.getEquippedStack(EquipmentSlot.CHEST));
                    boolean hasLegs = theendupdate$isSpectralTrimClient(armorStand.getEquippedStack(EquipmentSlot.LEGS));
                    boolean hasFeet = theendupdate$isSpectralTrimClient(armorStand.getEquippedStack(EquipmentSlot.FEET));

                    int spectralPieces = 0;
                    if (hasHead) spectralPieces++;
                    if (hasChest) spectralPieces++;
                    if (hasLegs) spectralPieces++;
                    if (hasFeet) spectralPieces++;

                    if (spectralPieces > 0) {
                        float spawnChance = switch (spectralPieces) {
                            case 1 -> 0.5f;
                            case 2 -> 0.7f;
                            case 3 -> 0.85f;
                            default -> 1.0f;
                        };
                        if (mc.world.random.nextFloat() <= spawnChance) {
                            theendupdate$spawnParticlesForEntity(mc, armorStand, hasHead, hasChest, hasLegs, hasFeet, spectralPieces);
                        }
                    }
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

    private static void theendupdate$spawnParticlesForEntity(MinecraftClient mc, net.minecraft.entity.LivingEntity entity, 
            boolean hasHead, boolean hasChest, boolean hasLegs, boolean hasFeet, int spectralPieces) {
        try {
            double yawRad = Math.toRadians(entity.getYaw());
            double fwdX = -Math.sin(yawRad);
            double fwdZ =  Math.cos(yawRad);
            double rightX =  Math.cos(yawRad);
            double rightZ =  Math.sin(yawRad);
            boolean phase = ((mc.world.getTime() / 20) % 2) == 0;

            if (hasChest) {
                double baseY = entity.getY() + 1.30;
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
                double x = entity.getX() + offX + latX + sprX + (mc.world.random.nextDouble() - 0.5) * 0.14;
                double y = baseY + (mc.world.random.nextDouble() - 0.5) * yMag;
                double z = entity.getZ() + offZ + latZ + sprZ + (mc.world.random.nextDouble() - 0.5) * 0.14;
                mc.particleManager.addParticle(ParticleTypes.END_ROD, x, y, z, 0.0, 0.0, 0.0);
            }

            if (hasHead) {
                double baseY = entity.getEyeY() + 0.00;
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
                double x = entity.getX() + offX + latX + sprX + (mc.world.random.nextDouble() - 0.5) * 0.14;
                double y = baseY + (mc.world.random.nextDouble() - 0.5) * yMag;
                double z = entity.getZ() + offZ + latZ + sprZ + (mc.world.random.nextDouble() - 0.5) * 0.14;
                mc.particleManager.addParticle(ParticleTypes.END_ROD, x, y, z, 0.0, 0.0, 0.0);
            }

            if (hasLegs) {
                double baseY = entity.getY() + 0.95;
                double lrMag = 0.55;
                double fbMag = 0.35;
                double yMag  = 0.18;
                double offX = (phase ? rightX : -rightX) * lrMag;
                double offZ = (phase ? rightZ : -rightZ) * lrMag;
                boolean forwardSide = mc.world.random.nextBoolean();
                double swayScale = fbMag * mc.world.random.nextDouble();
                double swayX = (forwardSide ? fwdX : -fwdX) * swayScale;
                double swayZ = (forwardSide ? fwdZ : -fwdZ) * swayScale;
                double x = entity.getX() + offX + swayX + (mc.world.random.nextDouble() - 0.5) * 0.16;
                double y = baseY + (mc.world.random.nextDouble() - 0.5) * yMag;
                double z = entity.getZ() + offZ + swayZ + (mc.world.random.nextDouble() - 0.5) * 0.16;
                mc.particleManager.addParticle(ParticleTypes.END_ROD, x, y, z, 0.0, 0.0, 0.0);
            }

            if (hasFeet) {
                double baseY = entity.getY() + 0.25;
                double lrMag = 0.45;
                double fbMag = 0.28;
                double yMag  = 0.14;
                double offX = (phase ? rightX : -rightX) * lrMag;
                double offZ = (phase ? rightZ : -rightZ) * lrMag;
                boolean forwardSide = mc.world.random.nextBoolean();
                double swayScale = fbMag * mc.world.random.nextDouble();
                double swayX = (forwardSide ? fwdX : -fwdX) * swayScale;
                double swayZ = (forwardSide ? fwdZ : -fwdZ) * swayScale;
                double x = entity.getX() + offX + swayX + (mc.world.random.nextDouble() - 0.5) * 0.14;
                double y = baseY + (mc.world.random.nextDouble() - 0.5) * yMag;
                double z = entity.getZ() + offZ + swayZ + (mc.world.random.nextDouble() - 0.5) * 0.14;
                mc.particleManager.addParticle(ParticleTypes.END_ROD, x, y, z, 0.0, 0.0, 0.0);
            }
        } catch (Throwable ignored) {}
    }

}


