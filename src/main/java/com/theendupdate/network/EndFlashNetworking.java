package com.theendupdate.network;

import com.theendupdate.TheEndUpdate;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public final class EndFlashNetworking {
    public static final Identifier START_FLASH = Identifier.fromNamespaceAndPath(TheEndUpdate.MOD_ID, "start_flash");

    private EndFlashNetworking() {}

    public static void registerServerReceiver() {
        TheEndUpdate.LOGGER.info("[EndUpdate] Registering server-side network receivers...");

        try {
            PayloadTypeRegistry.serverboundPlay().register(EndFlashPayload.ID, EndFlashPayload.CODEC);
            TheEndUpdate.LOGGER.info("[EndUpdate] Registered START_FLASH payload type: {}", EndFlashPayload.ID);
        } catch (Exception e) {
            TheEndUpdate.LOGGER.error("[EndUpdate] Failed to register START_FLASH payload type:", e);
        }
        
        ServerPlayNetworking.registerGlobalReceiver(EndFlashPayload.ID, (payload, context) -> {
            try {
                ServerPlayer player = context.player();
                if (player == null) {
                    TheEndUpdate.LOGGER.warn("[EndUpdate] Received START_FLASH packet but player is null");
                    return;
                }
                net.minecraft.server.level.ServerLevel world = (net.minecraft.server.level.ServerLevel) player.level();
                if (world == null) {
                    TheEndUpdate.LOGGER.warn("[EndUpdate] Received START_FLASH packet but world is null for player {}", player.getName().getString());
                    return;
                }
                
                long dur = Math.max(20, Math.min(400, payload.durationTicks())); // clamp 1s..20s (buffer time)
                int rad = Math.max(4, Math.min(64, payload.radius()));
                var center = payload.center();
                
                TheEndUpdate.LOGGER.info("[EndUpdate] ========== SERVER RECEIVED START_FLASH ==========");
                TheEndUpdate.LOGGER.info("[EndUpdate] Player: {} at {}", player.getName().getString(), player.blockPosition());
                TheEndUpdate.LOGGER.info("[EndUpdate] Packet data: center={}, radius={} (clamped to {}), duration={} (clamped to {})", 
                    center, payload.radius(), rad, payload.durationTicks(), dur);
                TheEndUpdate.LOGGER.info("[EndUpdate] World: {} (dimension: {})", 
                    world.dimension().identifier(), world.dimension());
                TheEndUpdate.LOGGER.info("[EndUpdate] Scheduling flower close operation...");
                
                world.getServer().execute(() -> {
                    TheEndUpdate.LOGGER.info("[EndUpdate] Executing flower close operation on server thread");
                    EnderChrysanthemumCloser.closeLoadedAroundPlayers(world, dur);
                });
            } catch (Exception e) {
                TheEndUpdate.LOGGER.error("[EndUpdate] ========== PACKET HANDLER ERROR ==========");
                TheEndUpdate.LOGGER.error("[EndUpdate] Error handling START_FLASH packet:", e);
                TheEndUpdate.LOGGER.error("[EndUpdate] =========================================");
            }
        });

        try {
            PayloadTypeRegistry.serverboundPlay().register(FlashEndedPayload.ID, FlashEndedPayload.CODEC);
            TheEndUpdate.LOGGER.info("[EndUpdate] Registered FLASH_ENDED payload type: {}", FlashEndedPayload.ID);
        } catch (Exception e) {
            TheEndUpdate.LOGGER.error("[EndUpdate] Failed to register FLASH_ENDED payload type:", e);
        }
        
        ServerPlayNetworking.registerGlobalReceiver(FlashEndedPayload.ID, (payload, context) -> {
            try {
                ServerPlayer player = context.player();
                if (player == null) {
                    TheEndUpdate.LOGGER.warn("[EndUpdate] Received FLASH_ENDED packet but player is null");
                    return;
                }
                net.minecraft.server.level.ServerLevel world = (net.minecraft.server.level.ServerLevel) player.level();
                if (world == null) {
                    TheEndUpdate.LOGGER.warn("[EndUpdate] Received FLASH_ENDED packet but world is null for player {}", player.getName().getString());
                    return;
                }

                if (world.dimension() != net.minecraft.world.level.Level.END) {
                    TheEndUpdate.LOGGER.debug("[EndUpdate] Received FLASH_ENDED packet outside The End (dimension: {}), ignoring", 
                        world.dimension().identifier());
                    return;
                }
                
                TheEndUpdate.LOGGER.info("[EndUpdate] ========== SERVER RECEIVED FLASH_ENDED ==========");
                TheEndUpdate.LOGGER.info("[EndUpdate] Player: {} at {}", player.getName().getString(), player.blockPosition());
                TheEndUpdate.LOGGER.info("[EndUpdate] World: {} (dimension: {})", 
                    world.dimension().identifier(), world.dimension());
                TheEndUpdate.LOGGER.info("[EndUpdate] Scheduling flower reopen operation...");
                
                world.getServer().execute(() -> {
                    TheEndUpdate.LOGGER.info("[EndUpdate] Executing flower reopen operation on server thread");
                    EnderChrysanthemumCloser.forceReopenAll(world);
                });
            } catch (Exception e) {
                TheEndUpdate.LOGGER.error("[EndUpdate] ========== PACKET HANDLER ERROR ==========");
                TheEndUpdate.LOGGER.error("[EndUpdate] Error handling FLASH_ENDED packet:", e);
                TheEndUpdate.LOGGER.error("[EndUpdate] =========================================");
            }
        });
        
        TheEndUpdate.LOGGER.info("[EndUpdate] Server-side network receivers registered successfully");
    }
}


