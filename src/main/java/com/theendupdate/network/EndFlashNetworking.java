package com.theendupdate.network;

import com.theendupdate.TemplateMod;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public final class EndFlashNetworking {
    public static final Identifier START_FLASH = Identifier.of(TemplateMod.MOD_ID, "start_flash");

    private EndFlashNetworking() {}

    public static void registerServerReceiver() {
        TemplateMod.LOGGER.info("[EndUpdate] Registering server-side network receivers...");
        
        // Register typed C2S payload and handler for flash start
        try {
            PayloadTypeRegistry.playC2S().register(EndFlashPayload.ID, EndFlashPayload.CODEC);
            TemplateMod.LOGGER.info("[EndUpdate] Registered START_FLASH payload type: {}", EndFlashPayload.ID);
        } catch (Exception e) {
            TemplateMod.LOGGER.error("[EndUpdate] Failed to register START_FLASH payload type:", e);
        }
        
        ServerPlayNetworking.registerGlobalReceiver(EndFlashPayload.ID, (payload, context) -> {
            try {
                ServerPlayerEntity player = context.player();
                if (player == null) {
                    TemplateMod.LOGGER.warn("[EndUpdate] Received START_FLASH packet but player is null");
                    return;
                }
                net.minecraft.server.world.ServerWorld world = (net.minecraft.server.world.ServerWorld) player.getEntityWorld();
                if (world == null) {
                    TemplateMod.LOGGER.warn("[EndUpdate] Received START_FLASH packet but world is null for player {}", player.getName().getString());
                    return;
                }
                
                long dur = Math.max(20, Math.min(300, payload.durationTicks())); // clamp 1s..15s (buffer time)
                int rad = Math.max(4, Math.min(64, payload.radius()));
                var center = payload.center();
                
                TemplateMod.LOGGER.info("[EndUpdate] ========== SERVER RECEIVED START_FLASH ==========");
                TemplateMod.LOGGER.info("[EndUpdate] Player: {} at {}", player.getName().getString(), player.getBlockPos());
                TemplateMod.LOGGER.info("[EndUpdate] Packet data: center={}, radius={} (clamped to {}), duration={} (clamped to {})", 
                    center, payload.radius(), rad, payload.durationTicks(), dur);
                TemplateMod.LOGGER.info("[EndUpdate] World: {} (dimension: {})", 
                    world.getRegistryKey().getValue(), world.getRegistryKey());
                TemplateMod.LOGGER.info("[EndUpdate] Scheduling flower close operation...");
                
                world.getServer().execute(() -> {
                    TemplateMod.LOGGER.info("[EndUpdate] Executing flower close operation on server thread");
                    EnderChrysanthemumCloser.closeNearby(world, center, rad, dur);
                });
            } catch (Exception e) {
                TemplateMod.LOGGER.error("[EndUpdate] ========== PACKET HANDLER ERROR ==========");
                TemplateMod.LOGGER.error("[EndUpdate] Error handling START_FLASH packet:", e);
                TemplateMod.LOGGER.error("[EndUpdate] =========================================");
            }
        });
        
        // Register handler for flash end signal
        try {
            PayloadTypeRegistry.playC2S().register(FlashEndedPayload.ID, FlashEndedPayload.CODEC);
            TemplateMod.LOGGER.info("[EndUpdate] Registered FLASH_ENDED payload type: {}", FlashEndedPayload.ID);
        } catch (Exception e) {
            TemplateMod.LOGGER.error("[EndUpdate] Failed to register FLASH_ENDED payload type:", e);
        }
        
        ServerPlayNetworking.registerGlobalReceiver(FlashEndedPayload.ID, (payload, context) -> {
            try {
                ServerPlayerEntity player = context.player();
                if (player == null) {
                    TemplateMod.LOGGER.warn("[EndUpdate] Received FLASH_ENDED packet but player is null");
                    return;
                }
                net.minecraft.server.world.ServerWorld world = (net.minecraft.server.world.ServerWorld) player.getEntityWorld();
                if (world == null) {
                    TemplateMod.LOGGER.warn("[EndUpdate] Received FLASH_ENDED packet but world is null for player {}", player.getName().getString());
                    return;
                }
                
                // Only process if in The End
                if (world.getRegistryKey() != net.minecraft.world.World.END) {
                    TemplateMod.LOGGER.debug("[EndUpdate] Received FLASH_ENDED packet outside The End (dimension: {}), ignoring", 
                        world.getRegistryKey().getValue());
                    return;
                }
                
                TemplateMod.LOGGER.info("[EndUpdate] ========== SERVER RECEIVED FLASH_ENDED ==========");
                TemplateMod.LOGGER.info("[EndUpdate] Player: {} at {}", player.getName().getString(), player.getBlockPos());
                TemplateMod.LOGGER.info("[EndUpdate] World: {} (dimension: {})", 
                    world.getRegistryKey().getValue(), world.getRegistryKey());
                TemplateMod.LOGGER.info("[EndUpdate] Scheduling flower reopen operation...");
                
                world.getServer().execute(() -> {
                    TemplateMod.LOGGER.info("[EndUpdate] Executing flower reopen operation on server thread");
                    EnderChrysanthemumCloser.forceReopenAll(world);
                });
            } catch (Exception e) {
                TemplateMod.LOGGER.error("[EndUpdate] ========== PACKET HANDLER ERROR ==========");
                TemplateMod.LOGGER.error("[EndUpdate] Error handling FLASH_ENDED packet:", e);
                TemplateMod.LOGGER.error("[EndUpdate] =========================================");
            }
        });
        
        TemplateMod.LOGGER.info("[EndUpdate] Server-side network receivers registered successfully");
    }
}


