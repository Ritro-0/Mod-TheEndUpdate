package com.theendupdate;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;

import com.theendupdate.entity.model.EtherealOrbEntityModel;
import com.theendupdate.entity.model.KingPhantomEntityModel;
import com.theendupdate.entity.model.VoidTardigradeEntityModel;
import com.theendupdate.entity.renderer.EtherealOrbEntityRenderer;
import com.theendupdate.entity.renderer.KingPhantomEntityRenderer;
import com.theendupdate.entity.renderer.ShadowCreakingEntityRenderer;
import com.theendupdate.entity.renderer.VoidTardigradeEntityRenderer;
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

@Environment(EnvType.CLIENT)  
public class TemplateModClient implements ClientModInitializer {
    
    public static final EntityModelLayer MODEL_ETHEREAL_ORB_LAYER = new EntityModelLayer(Identifier.of(TemplateMod.MOD_ID, "ethereal_orb"), "main");

    @Override
    public void onInitializeClient() {
        // Register transparent blocks
        BlockRenderLayerMap.putBlock(ModBlocks.VOID_BLOOM, BlockRenderLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModBlocks.ENDER_CHRYSANTHEMUM, BlockRenderLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModBlocks.CLOSED_ENDER_CHRYSANTHEMUM, BlockRenderLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModBlocks.VOID_SAP, BlockRenderLayer.CUTOUT);
        // Potted variants must also be cutout
        BlockRenderLayerMap.putBlock(ModBlocks.POTTED_SHADOW_CLAW, BlockRenderLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModBlocks.POTTED_VOID_BLOOM, BlockRenderLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModBlocks.POTTED_ENDER_CHRYSANTHEMUM, BlockRenderLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModBlocks.POTTED_CLOSED_ENDER_CHRYSANTHEMUM, BlockRenderLayer.CUTOUT);
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
        // Nebula vent block has non-opaque tube extending upward
        BlockRenderLayerMap.putBlock(ModBlocks.NEBULA_VENT_BLOCK, BlockRenderLayer.CUTOUT);
        // Register tooltip callback for spawn eggs to show "Disabled in Peaceful" (only for hostile mobs)
        ItemTooltipCallback.EVENT.register((stack, tooltipContext, tooltipType, lines) -> {
            if (stack.getItem() instanceof com.theendupdate.item.CustomSpawnEggItem spawnEgg) {
                // Only show for hostile mobs (MONSTER), explicitly exclude peaceful mobs (AMBIENT)
                SpawnGroup group = spawnEgg.getEntityType().getSpawnGroup();
                if (group == SpawnGroup.MONSTER) {
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
        // Entity renderers use OrderedRenderCommandQueue API
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
        // Void tardigrade renderer
        EntityModelLayerRegistry.registerModelLayer(VoidTardigradeEntityModel.LAYER_LOCATION, VoidTardigradeEntityModel::getTexturedModelData);
        EntityRendererRegistry.register(ModEntities.VOID_TARDIGRADE, VoidTardigradeEntityRenderer::new);
        // Register custom screen for Quantum Gateway
        HandledScreens.register(com.theendupdate.registry.ModScreenHandlers.GATEWAY, com.theendupdate.screen.GatewayScreen::new);

        // Hanging signs use vanilla renderer and block entity type - no custom registration needed
        
        // Register shelf block entity renderers - uses vanilla renderer
        BlockEntityRendererFactories.register(ModBlockEntities.ETHEREAL_SHELF, ShelfBlockEntityRenderer::new);
        BlockEntityRendererFactories.register(ModBlockEntities.SHADOW_SHELF, ShelfBlockEntityRenderer::new);

        // Visual-only top handled via model geometry extending into y+1 (no extra block used)

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            try {
                MinecraftClient mc = client;
                if (mc == null || mc.world == null || mc.player == null) return;
                
                // Check for End flash sounds every tick using reflection
                // Log once when we first start checking (only in The End)
                if (mc.world.getRegistryKey() == net.minecraft.world.World.END && !theendupdate$fieldSearchDone) {
                    TemplateMod.LOGGER.info("[EndUpdate] ========== CLIENT SOUND DETECTION STARTING ==========");
                    TemplateMod.LOGGER.info("[EndUpdate] Client entered The End dimension - initializing sound detection");
                    TemplateMod.LOGGER.info("[EndUpdate] Player: {} at {}", mc.player.getName().getString(), mc.player.getBlockPos());
                }
                theendupdate$checkEndFlashSound(mc);
                
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

        // End flash sound hook
        com.theendupdate.SoundHooks.register();

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

    private static final java.util.Map<net.minecraft.util.Identifier, Long> theendupdate$detectedSounds = new java.util.HashMap<>();
    private static java.lang.reflect.Field theendupdate$soundSystemField = null;
    private static java.lang.reflect.Field theendupdate$soundsField = null;
    private static boolean theendupdate$fieldSearchDone = false;
    private static boolean theendupdate$soundDebugLogged = false;
    private static final net.minecraft.util.Identifier END_FLASH_SOUND = net.minecraft.util.Identifier.of("minecraft", "weather.end_flash");
    private static final boolean theendupdate$debugMode = false; // Toggle this for debug logging
    private static final java.util.Set<net.minecraft.util.Identifier> theendupdate$previousTickSounds = new java.util.HashSet<>();
    private static int theendupdate$activeEndFlashSounds = 0;
    private static final int theendupdate$chrysanthemumCloseBufferTicks = 100; // Extra buffer time (5 seconds max fallback)
    private static final int theendupdate$gracePeriodTicks = 40; // Grace period after sound ends (40 ticks = 2s to match visual flash fade)
    private static final boolean theendupdate$visualFadeEnabled = true; // Enable grace period for visual fade matching
    private static long theendupdate$lastFlashEndTime = 0; // Track when flash ended for grace period
    private static long theendupdate$reopenSignalTime = 0; // Track when reopen signal was sent
    
    private static void theendupdate$checkEndFlashSound(MinecraftClient mc) {
        try {
            if (mc.getSoundManager() == null || mc.world == null || mc.player == null) {
                TemplateMod.LOGGER.debug("[EndUpdate] Sound check skipped: soundManager={}, world={}, player={}", 
                    mc.getSoundManager() != null, mc.world != null, mc.player != null);
                return;
            }
            
            // Only check in The End dimension
            var dim = mc.world.getRegistryKey();
            if (dim != net.minecraft.world.World.END) {
                TemplateMod.LOGGER.debug("[EndUpdate] Sound check skipped: not in The End dimension (current: {})", dim.getValue());
                return;
            }
            
            long currentTime = mc.world.getTime();
            
            // Clean up old detections (older than 200 ticks = 10 seconds)
            theendupdate$detectedSounds.entrySet().removeIf(entry -> currentTime - entry.getValue() > 200);
            
            // Track current tick's playing sounds to detect NEW starts
            java.util.Set<net.minecraft.util.Identifier> currentTickSounds = new java.util.HashSet<>();
            
            // Find soundSystem field from SoundManager once and cache it
            var soundManager = mc.getSoundManager();
            if (!theendupdate$fieldSearchDone) {
                try {
                    // Step 1: Find soundSystem field in SoundManager
                    String[] soundSystemNames = {"soundSystem", "system", "audioSystem"};
                    for (String fieldName : soundSystemNames) {
                        try {
                            java.lang.reflect.Field f = soundManager.getClass().getDeclaredField(fieldName);
                            f.setAccessible(true);
                            Object val = f.get(soundManager);
                            if (val != null && val.getClass().getName().contains("SoundSystem")) {
                                theendupdate$soundSystemField = f;
                                break;
                            }
                        } catch (NoSuchFieldException ignored) {}
                    }
                    
                    // Step 2: Get SoundSystem instance and find sounds/sources field
                    if (theendupdate$soundSystemField != null) {
                        Object soundSystem = theendupdate$soundSystemField.get(soundManager);
                        if (soundSystem != null) {
                            // Try "sounds" (Multimap) first, then "sources" (Map)
                            String[] soundsFieldNames = {"sounds", "sources", "playingSounds", "activeSounds"};
                            for (String fieldName : soundsFieldNames) {
                                try {
                                    java.lang.reflect.Field f = soundSystem.getClass().getDeclaredField(fieldName);
                                    f.setAccessible(true);
                                    Object val = f.get(soundSystem);
                                    if (val != null && (val instanceof java.util.Collection || val instanceof java.util.Map || val.getClass().getName().contains("Multimap"))) {
                                        theendupdate$soundsField = f;
                                        break;
                                    }
                                } catch (NoSuchFieldException ignored) {}
                            }
                        }
                    }
                    
                    // Fallback: search all fields if direct names didn't work
                    if (theendupdate$soundSystemField == null) {
                        TemplateMod.LOGGER.info("[EndUpdate] Direct field names failed, searching all SoundManager fields...");
                        
                        // Try known obfuscated field names for 1.21.10 (from logs: field_5590 is class_1140 which is likely SoundSystem)
                        String[] obfuscatedFieldNames = {"field_5590", "field_42935", "field_5592"};
                        for (String fieldName : obfuscatedFieldNames) {
                            try {
                                java.lang.reflect.Field f = soundManager.getClass().getDeclaredField(fieldName);
                                f.setAccessible(true);
                                Object val = f.get(soundManager);
                                if (val != null) {
                                    String className = val.getClass().getName();
                                    // class_1140 is likely SoundSystem in obfuscated code
                                    if (className.contains("class_1140") || className.contains("class_1146")) {
                                        theendupdate$soundSystemField = f;
                                        TemplateMod.LOGGER.info("[EndUpdate] Found SoundSystem field using obfuscated name: {} -> {}", 
                                            fieldName, className);
                                        break;
                                    }
                                }
                            } catch (NoSuchFieldException | IllegalAccessException ignored) {
                                // Try next field
                            }
                        }
                        
                        // If still not found, search all fields by type
                        if (theendupdate$soundSystemField == null) {
                            java.util.List<String> allFieldNames = new java.util.ArrayList<>();
                            java.util.List<String> candidateFields = new java.util.ArrayList<>();
                            
                            for (java.lang.reflect.Field f : soundManager.getClass().getDeclaredFields()) {
                                try {
                                    f.setAccessible(true);
                                    String fieldName = f.getName();
                                    String fieldType = f.getType().getName();
                                    allFieldNames.add(fieldName + " (" + f.getType().getSimpleName() + ")");
                                    Object val = f.get(soundManager);
                                    if (val != null) {
                                        String className = val.getClass().getName();
                                        
                                        // Check if it's class_1140 or class_1146 (likely SoundSystem variants)
                                        boolean isSoundSystem = className.contains("class_1140") || 
                                                               className.contains("class_1146") ||
                                                               className.contains("SoundSystem") ||
                                                               (className.contains("sound") && className.contains("system"));
                                        
                                        // Check interfaces
                                        if (!isSoundSystem) {
                                            Class<?> valClass = val.getClass();
                                            for (Class<?> iface : valClass.getInterfaces()) {
                                                if (iface.getName().toLowerCase().contains("sound")) {
                                                    isSoundSystem = true;
                                                    break;
                                                }
                                            }
                                        }
                                        
                                        if (isSoundSystem) {
                                            candidateFields.add(fieldName + " -> " + className);
                                            if (theendupdate$soundSystemField == null) {
                                                theendupdate$soundSystemField = f;
                                                TemplateMod.LOGGER.info("[EndUpdate] Found potential SoundSystem field: {} -> {}", 
                                                    fieldName, className);
                                            }
                                        }
                                    }
                                } catch (Throwable t) {
                                    // Skip fields we can't access
                                }
                            }
                            
                            TemplateMod.LOGGER.info("[EndUpdate] SoundManager has {} total fields", allFieldNames.size());
                            if (!candidateFields.isEmpty()) {
                                TemplateMod.LOGGER.info("[EndUpdate] Found {} candidate SoundSystem fields: {}", 
                                    candidateFields.size(), String.join(", ", candidateFields));
                            }
                        }
                    }
                    
                    if (theendupdate$soundSystemField != null && theendupdate$soundsField == null) {
                        Object soundSystem = theendupdate$soundSystemField.get(soundManager);
                        if (soundSystem != null) {
                            TemplateMod.LOGGER.info("[EndUpdate] Searching for sounds collection in SoundSystem (type: {})...", 
                                soundSystem.getClass().getName());
                            
                            // First, try known obfuscated field names for 1.21.10 (from analysis)
                            // field_18950: Map with SoundInstance keys (primary collection)
                            // field_18951: Multimap grouping by SoundCategory
                            // field_5557: List of TickableSoundInstances
                            String[] knownSoundsFieldNames = {"field_18950", "field_18951", "field_5557", 
                                                              "sounds", "sources", "playingSounds", "activeSounds"};
                            for (String fieldName : knownSoundsFieldNames) {
                                try {
                                    java.lang.reflect.Field f = soundSystem.getClass().getDeclaredField(fieldName);
                                    f.setAccessible(true);
                                    Object val = f.get(soundSystem);
                                    if (val != null) {
                                        String valType = val.getClass().getName();
                                        boolean isValid = false;
                                        
                                        // Check if it contains SoundInstances
                                        if (val instanceof java.util.Map) {
                                            java.util.Map<?,?> map = (java.util.Map<?,?>) val;
                                            if (!map.isEmpty()) {
                                                Object firstKey = map.keySet().iterator().next();
                                                Object firstValue = map.values().iterator().next();
                                                if ((firstKey != null && firstKey.getClass().getName().contains("SoundInstance")) ||
                                                    (firstValue != null && firstValue.getClass().getName().contains("SoundInstance"))) {
                                                    isValid = true;
                                                    TemplateMod.LOGGER.info("[EndUpdate] Found sounds collection using known field name: {} -> {} (Map, size: {})", 
                                                        fieldName, valType, map.size());
                                                }
                                            }
                                        } else if (val instanceof java.util.Collection) {
                                            java.util.Collection<?> coll = (java.util.Collection<?>) val;
                                            if (!coll.isEmpty()) {
                                                Object first = coll.iterator().next();
                                                if (first != null && first.getClass().getName().contains("SoundInstance")) {
                                                    isValid = true;
                                                    TemplateMod.LOGGER.info("[EndUpdate] Found sounds collection using known field name: {} -> {} (Collection, size: {})", 
                                                        fieldName, valType, coll.size());
                                                }
                                            }
                                        } else if (valType.contains("Multimap")) {
                                            // Multimap - try to get values
                                            try {
                                                java.lang.reflect.Method valuesMethod = val.getClass().getMethod("values");
                                                Object values = valuesMethod.invoke(val);
                                                if (values instanceof java.util.Collection) {
                                                    java.util.Collection<?> valuesColl = (java.util.Collection<?>) values;
                                                    if (!valuesColl.isEmpty()) {
                                                        Object first = valuesColl.iterator().next();
                                                        if (first != null && first.getClass().getName().contains("SoundInstance")) {
                                                            isValid = true;
                                                            TemplateMod.LOGGER.info("[EndUpdate] Found sounds collection using known field name: {} -> {} (Multimap, size: {})", 
                                                                fieldName, valType, valuesColl.size());
                                                        }
                                                    }
                                                }
                                            } catch (Throwable ignored) {}
                                        }
                                        
                                        if (isValid) {
                                            theendupdate$soundsField = f;
                                            break;
                                        }
                                    }
                                } catch (NoSuchFieldException | IllegalAccessException ignored) {
                                    // Try next field name
                                }
                            }
                            
                            // If still not found, try to find a method that returns playing sounds (common API pattern)
                            if (theendupdate$soundsField == null) {
                            try {
                                java.lang.reflect.Method[] methods = soundSystem.getClass().getDeclaredMethods();
                                for (java.lang.reflect.Method m : methods) {
                                    try {
                                        String methodName = m.getName().toLowerCase();
                                        if ((methodName.contains("sound") || methodName.contains("playing") || methodName.contains("active")) &&
                                            (java.util.Collection.class.isAssignableFrom(m.getReturnType()) || 
                                             java.util.Map.class.isAssignableFrom(m.getReturnType()))) {
                                            m.setAccessible(true);
                                            Object result = m.invoke(soundSystem);
                                            if (result != null && 
                                                (result instanceof java.util.Collection || result instanceof java.util.Map)) {
                                                // Check if it contains SoundInstance-like objects
                                                boolean containsSounds = false;
                                                if (result instanceof java.util.Collection) {
                                                    java.util.Collection<?> coll = (java.util.Collection<?>) result;
                                                    if (!coll.isEmpty()) {
                                                        Object first = coll.iterator().next();
                                                        if (first != null && first.getClass().getName().contains("SoundInstance")) {
                                                            containsSounds = true;
                                                        }
                                                    }
                                                } else if (result instanceof java.util.Map) {
                                                    java.util.Map<?,?> map = (java.util.Map<?,?>) result;
                                                    if (!map.isEmpty()) {
                                                        Object first = map.keySet().iterator().next();
                                                        if (first != null && first.getClass().getName().contains("SoundInstance")) {
                                                            containsSounds = true;
                                                        }
                                                    }
                                                }
                                                
                                                if (containsSounds) {
                                                    TemplateMod.LOGGER.info("[EndUpdate] Found sounds via method: {} -> {}", 
                                                        m.getName(), result.getClass().getName());
                                                    // Store method for later use - we'll need a different approach
                                                    // For now, continue searching fields
                                                }
                                            }
                                        }
                                    } catch (Throwable ignored) {}
                                }
                            } catch (Throwable t) {
                                TemplateMod.LOGGER.debug("[EndUpdate] Could not inspect SoundSystem methods: {}", t.getMessage());
                            }
                            }
                            
                            // If still not found, search all fields in SoundSystem as fallback
                            if (theendupdate$soundsField == null) {
                            java.util.List<String> soundSystemFields = new java.util.ArrayList<>();
                            java.util.List<String> candidateFields = new java.util.ArrayList<>();
                            
                            for (java.lang.reflect.Field f : soundSystem.getClass().getDeclaredFields()) {
                                try {
                                    f.setAccessible(true);
                                    String fieldName = f.getName();
                                    String fieldType = f.getType().getName();
                                    Object val = f.get(soundSystem);
                                    soundSystemFields.add(fieldName + " (" + f.getType().getSimpleName() + ")");
                                    
                                    if (val != null) {
                                        String valType = val.getClass().getName();
                                        boolean isSoundsCollection = false;
                                        
                                        // Check for Multimap
                                        if (valType.contains("Multimap")) {
                                            isSoundsCollection = true;
                                            TemplateMod.LOGGER.info("[EndUpdate] Found Multimap field: {} -> {}", fieldName, valType);
                                        }
                                        // Check for Map with SoundInstance keys/values
                                        else if (val instanceof java.util.Map) {
                                            java.util.Map<?,?> map = (java.util.Map<?,?>) val;
                                            if (!map.isEmpty()) {
                                                Object firstKey = map.keySet().iterator().next();
                                                Object firstValue = map.values().iterator().next();
                                                if ((firstKey != null && firstKey.getClass().getName().contains("SoundInstance")) ||
                                                    (firstValue != null && firstValue.getClass().getName().contains("SoundInstance"))) {
                                                    isSoundsCollection = true;
                                                    TemplateMod.LOGGER.info("[EndUpdate] Found Map with SoundInstances: {} -> {} (size: {})", 
                                                        fieldName, valType, map.size());
                                                } else {
                                                    TemplateMod.LOGGER.debug("[EndUpdate] Map field {} has first key: {}, first value: {}", 
                                                        fieldName, 
                                                        firstKey != null ? firstKey.getClass().getName() : "null",
                                                        firstValue != null ? firstValue.getClass().getName() : "null");
                                                }
                                            } else {
                                                TemplateMod.LOGGER.debug("[EndUpdate] Map field {} is empty", fieldName);
                                            }
                                        }
                                        // Check for Collection
                                        else if (val instanceof java.util.Collection) {
                                            java.util.Collection<?> coll = (java.util.Collection<?>) val;
                                            if (!coll.isEmpty()) {
                                                Object first = coll.iterator().next();
                                                if (first != null && first.getClass().getName().contains("SoundInstance")) {
                                                    isSoundsCollection = true;
                                                    TemplateMod.LOGGER.info("[EndUpdate] Found Collection with SoundInstances: {} -> {} (size: {})", 
                                                        fieldName, valType, coll.size());
                                                } else {
                                                    TemplateMod.LOGGER.debug("[EndUpdate] Collection field {} has first element: {}", 
                                                        fieldName, first != null ? first.getClass().getName() : "null");
                                                }
                                            } else {
                                                TemplateMod.LOGGER.debug("[EndUpdate] Collection field {} is empty", fieldName);
                                            }
                                        }
                                        
                                        if (isSoundsCollection) {
                                            candidateFields.add(fieldName + " -> " + valType);
                                            if (theendupdate$soundsField == null) {
                                                theendupdate$soundsField = f;
                                                TemplateMod.LOGGER.info("[EndUpdate] Selected sounds collection field: {} -> {}", 
                                                    fieldName, valType);
                                            }
                                        }
                                    } else {
                                        TemplateMod.LOGGER.debug("[EndUpdate] Field {} is null", fieldName);
                                    }
                                } catch (Throwable t) {
                                    TemplateMod.LOGGER.debug("[EndUpdate] Could not access field {}: {}", 
                                        f.getName(), t.getMessage());
                                }
                            }
                            
                            TemplateMod.LOGGER.info("[EndUpdate] SoundSystem (class_1140) has {} total fields", soundSystemFields.size());
                            if (!candidateFields.isEmpty()) {
                                TemplateMod.LOGGER.info("[EndUpdate] Found {} candidate sounds collection fields: {}", 
                                    candidateFields.size(), String.join(", ", candidateFields));
                            } else {
                                TemplateMod.LOGGER.warn("[EndUpdate] No candidate sounds collection fields found. All fields: {}", 
                                    String.join(", ", soundSystemFields));
                            }
                            }
                        }
                    }
                    
                    theendupdate$fieldSearchDone = true;
                    if (theendupdate$soundSystemField == null || theendupdate$soundsField == null) {
                        TemplateMod.LOGGER.error("[EndUpdate] ========== SOUND DETECTION SETUP FAILED ==========");
                        TemplateMod.LOGGER.error("[EndUpdate] Could not locate sound system fields - sound detection will NOT work");
                        TemplateMod.LOGGER.error("[EndUpdate] soundSystemField found: {}", theendupdate$soundSystemField != null);
                        TemplateMod.LOGGER.error("[EndUpdate] soundsField found: {}", theendupdate$soundsField != null);
                        TemplateMod.LOGGER.error("[EndUpdate] SoundManager class: {}", soundManager.getClass().getName());
                        
                        // Log ALL fields in SoundManager for debugging
                        TemplateMod.LOGGER.error("[EndUpdate] All SoundManager fields:");
                        try {
                            for (java.lang.reflect.Field f : soundManager.getClass().getDeclaredFields()) {
                                try {
                                    f.setAccessible(true);
                                    Object val = f.get(soundManager);
                                    String valInfo = val != null ? 
                                        val.getClass().getName() + (val instanceof java.util.Collection ? " (size: " + ((java.util.Collection<?>)val).size() + ")" : "") :
                                        "null";
                                    TemplateMod.LOGGER.error("[EndUpdate]   - {}: {} -> {}", 
                                        f.getName(), f.getType().getName(), valInfo);
                                } catch (Throwable t) {
                                    TemplateMod.LOGGER.error("[EndUpdate]   - {}: {} (could not access)", 
                                        f.getName(), f.getType().getName());
                                }
                            }
                        } catch (Throwable t) {
                            TemplateMod.LOGGER.error("[EndUpdate] Could not enumerate SoundManager fields: {}", t.getMessage());
                        }
                        
                        if (theendupdate$soundSystemField != null) {
                            try {
                                Object soundSystem = theendupdate$soundSystemField.get(soundManager);
                                if (soundSystem != null) {
                                    TemplateMod.LOGGER.error("[EndUpdate] SoundSystem class: {}", soundSystem.getClass().getName());
                                    TemplateMod.LOGGER.error("[EndUpdate] All SoundSystem fields:");
                                    for (java.lang.reflect.Field f : soundSystem.getClass().getDeclaredFields()) {
                                        try {
                                            f.setAccessible(true);
                                            Object val = f.get(soundSystem);
                                            String valInfo = val != null ? 
                                                val.getClass().getName() + (val instanceof java.util.Collection ? " (size: " + ((java.util.Collection<?>)val).size() + ")" : "") :
                                                "null";
                                            TemplateMod.LOGGER.error("[EndUpdate]   - {}: {} -> {}", 
                                                f.getName(), f.getType().getName(), valInfo);
                                        } catch (Throwable t) {
                                            TemplateMod.LOGGER.error("[EndUpdate]   - {}: {} (could not access)", 
                                                f.getName(), f.getType().getName());
                                        }
                                    }
                                }
                            } catch (Throwable t) {
                                TemplateMod.LOGGER.error("[EndUpdate] Could not access SoundSystem instance: {}", t.getMessage());
                            }
                        }
                        TemplateMod.LOGGER.error("[EndUpdate] =================================================");
                    } else {
                        TemplateMod.LOGGER.info("[EndUpdate] Sound detection setup SUCCESS - fields located:");
                        TemplateMod.LOGGER.info("[EndUpdate]   soundSystemField: {} in {}", 
                            theendupdate$soundSystemField.getName(), soundManager.getClass().getSimpleName());
                        try {
                            Object soundSystem = theendupdate$soundSystemField.get(soundManager);
                            if (soundSystem != null) {
                                TemplateMod.LOGGER.info("[EndUpdate]   soundsField: {} in {}", 
                                    theendupdate$soundsField.getName(), soundSystem.getClass().getSimpleName());
                            }
                        } catch (Throwable ignored) {}
                    }
                } catch (Throwable t) {
                    TemplateMod.LOGGER.error("[EndUpdate] ========== SOUND DETECTION SETUP ERROR ==========");
                    TemplateMod.LOGGER.error("[EndUpdate] Exception during sound system field discovery:", t);
                    TemplateMod.LOGGER.error("[EndUpdate] =================================================");
                    theendupdate$fieldSearchDone = true;
                }
            }
            
            if (theendupdate$soundSystemField == null || theendupdate$soundsField == null) {
                TemplateMod.LOGGER.debug("[EndUpdate] Sound check aborted: reflection fields not available");
                return;
            }
            
            // Get SoundSystem instance
            Object soundSystem = null;
            try {
                soundSystem = theendupdate$soundSystemField.get(soundManager);
            } catch (Throwable t) {
                TemplateMod.LOGGER.warn("[EndUpdate] Failed to get SoundSystem instance: {}", t.getMessage());
                return;
            }
            
            if (soundSystem == null) {
                TemplateMod.LOGGER.debug("[EndUpdate] SoundSystem instance is null");
                return;
            }
            
            // Get the sounds collection/map from SoundSystem
            Object soundsObj = null;
            try {
                soundsObj = theendupdate$soundsField.get(soundSystem);
            } catch (Throwable t) {
                TemplateMod.LOGGER.warn("[EndUpdate] Failed to get sounds collection from SoundSystem: {}", t.getMessage());
                return;
            }
            
            if (soundsObj == null) {
                TemplateMod.LOGGER.debug("[EndUpdate] Sounds collection is null");
                return;
            }
            
            // Extract SoundInstance collection from Multimap or Map
            java.util.Collection<?> playingSounds = null;
            if (soundsObj instanceof java.util.Collection<?> coll) {
                playingSounds = coll;
            } else if (soundsObj instanceof java.util.Map<?, ?> map) {
                // If it's a Map, check if keys or values are SoundInstances
                if (!map.isEmpty()) {
                    Object firstKey = map.keySet().iterator().next();
                    Object firstValue = map.values().iterator().next();
                    if (firstKey != null && firstKey.getClass().getName().contains("SoundInstance")) {
                        playingSounds = new java.util.ArrayList<>(map.keySet());
                    } else if (firstValue != null && firstValue.getClass().getName().contains("SoundInstance")) {
                        playingSounds = new java.util.ArrayList<>(map.values());
                    }
                }
            } else {
                // Try Multimap - has values() method
                try {
                    java.lang.reflect.Method valuesMethod = soundsObj.getClass().getMethod("values");
                    Object values = valuesMethod.invoke(soundsObj);
                    if (values instanceof java.util.Collection<?>) {
                        playingSounds = (java.util.Collection<?>) values;
                    }
                } catch (Throwable ignored) {}
            }
            
            if (playingSounds == null || playingSounds.isEmpty()) {
                TemplateMod.LOGGER.debug("[EndUpdate] No playing sounds detected (collection: {}, empty: {})", 
                    playingSounds != null, playingSounds != null && playingSounds.isEmpty());
                return;
            }
            
            TemplateMod.LOGGER.debug("[EndUpdate] Checking {} playing sound(s) for end flash...", playingSounds.size());
            
            // Check each playing sound and build current tick's set
            for (Object soundObj : playingSounds) {
                if (!(soundObj instanceof net.minecraft.client.sound.SoundInstance sound)) continue;
                
                net.minecraft.util.Identifier id = null;
                try {
                    id = sound.getId();
                } catch (Throwable ignored) {}
                
                if (id == null) {
                    try {
                        var snd = sound.getSound();
                        if (snd != null) id = snd.getIdentifier();
                    } catch (Throwable ignored) {}
                }
                
                if (id == null) continue;
                
                // Add to current tick's set
                currentTickSounds.add(id);
                
                // Check if this is the End flash sound event
                boolean isEndFlash = END_FLASH_SOUND.equals(id);
                
                if (isEndFlash) {
                    // Only trigger if this is a NEW start (wasn't playing last tick)
                    boolean isNewStart = !theendupdate$previousTickSounds.contains(id);
                    
                    // Also check cooldown: 200 ticks (10 seconds) to prevent rapid retriggering
                    Long lastDetected = theendupdate$detectedSounds.get(id);
                    boolean cooldownPassed = (lastDetected == null || currentTime - lastDetected > 200);
                    
                    if (isNewStart && cooldownPassed) {
                        // New sound start detected - trigger effect
                        theendupdate$activeEndFlashSounds++;
                        var center = mc.player.getBlockPos();
                        TemplateMod.LOGGER.info("[EndUpdate] ========== END FLASH SOUND DETECTED ==========");
                        TemplateMod.LOGGER.info("[EndUpdate] Client detected end flash sound at pos {} from client {}", 
                            center, mc.player != null ? mc.player.getName().getString() : "unknown");
                        TemplateMod.LOGGER.info("[EndUpdate] Active flash count: {}", theendupdate$activeEndFlashSounds);
                        TemplateMod.LOGGER.info("[EndUpdate] Sending START_FLASH packet to server...");
                        com.theendupdate.network.EndFlashClient.sendStartFlash(100 + theendupdate$chrysanthemumCloseBufferTicks, 32, center);
                        TemplateMod.LOGGER.info("[EndUpdate] START_FLASH packet sent successfully");
                        TemplateMod.LOGGER.info("[EndUpdate] ============================================");
                        theendupdate$detectedSounds.put(id, currentTime);
                    } else {
                        TemplateMod.LOGGER.debug("[EndUpdate] End flash sound detected but not triggering: isNewStart={}, cooldownPassed={}, lastDetected={}, timeSinceLast={}",
                            isNewStart, cooldownPassed, lastDetected, lastDetected != null ? (currentTime - lastDetected) : "null");
                    }
                }
            }
            
            // Count actual playing end flash sound instances (not just unique IDs)
            int currentEndFlashCount = 0;
            for (Object soundObj : playingSounds) {
                if (!(soundObj instanceof net.minecraft.client.sound.SoundInstance sound)) continue;
                net.minecraft.util.Identifier id = null;
                try {
                    id = sound.getId();
                } catch (Throwable ignored) {}
                if (id == null) {
                    try {
                        var snd = sound.getSound();
                        if (snd != null) id = snd.getIdentifier();
                    } catch (Throwable ignored) {}
                }
                if (id != null && END_FLASH_SOUND.equals(id)) {
                    currentEndFlashCount++;
                }
            }
            
            // If count decreased from previous, update and check if all ended
            if (currentEndFlashCount < theendupdate$activeEndFlashSounds) {
                int previousCount = theendupdate$activeEndFlashSounds;
                theendupdate$activeEndFlashSounds = currentEndFlashCount;
                if (currentEndFlashCount == 0) {
                    // All sounds ended - mark end time and schedule reopen after grace period
                    theendupdate$lastFlashEndTime = currentTime;
                }
            } else if (currentEndFlashCount > theendupdate$activeEndFlashSounds) {
                // Count increased (new instance detected outside our tracking)
                theendupdate$activeEndFlashSounds = currentEndFlashCount;
                theendupdate$lastFlashEndTime = 0; // Reset grace period if new sound starts
            }
            
            // Check grace period: if sounds ended and grace period has passed, signal reopen
            if (theendupdate$activeEndFlashSounds == 0 && theendupdate$lastFlashEndTime > 0) {
                int actualGracePeriod = theendupdate$visualFadeEnabled ? theendupdate$gracePeriodTicks : 0;
                long timeSinceEnd = currentTime - theendupdate$lastFlashEndTime;
                if (timeSinceEnd >= actualGracePeriod) {
                    TemplateMod.LOGGER.info("[EndUpdate] ========== END FLASH ENDED ==========");
                    TemplateMod.LOGGER.info("[EndUpdate] All end flash sounds have ended (grace period: {} ticks, elapsed: {} ticks)", 
                        actualGracePeriod, timeSinceEnd);
                    TemplateMod.LOGGER.info("[EndUpdate] Sending FLASH_ENDED packet to server...");
                    com.theendupdate.network.EndFlashClient.sendFlashEnded();
                    TemplateMod.LOGGER.info("[EndUpdate] FLASH_ENDED packet sent successfully");
                    TemplateMod.LOGGER.info("[EndUpdate] ====================================");
                    theendupdate$lastFlashEndTime = 0; // Reset to prevent duplicate signals
                } else {
                    TemplateMod.LOGGER.debug("[EndUpdate] Flash ended but grace period not yet passed: {}/{} ticks", 
                        timeSinceEnd, actualGracePeriod);
                }
            }
            
            // Update previous tick's sounds for next tick
            theendupdate$previousTickSounds.clear();
            theendupdate$previousTickSounds.addAll(currentTickSounds);
        } catch (Throwable t) {
            TemplateMod.LOGGER.error("[EndUpdate] ========== SOUND DETECTION ERROR ==========");
            TemplateMod.LOGGER.error("[EndUpdate] Exception in sound detection check:", t);
            TemplateMod.LOGGER.error("[EndUpdate] ===========================================");
        }
    }

}


