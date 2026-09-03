package com.theendupdate.network;

import com.theendupdate.TheEndUpdate;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.core.BlockPos;

public final class EndFlashClient {
    private EndFlashClient() {}

    public static void sendStartFlash(int durationTicks, int radius, BlockPos center) {
        try {
            int finalDuration = Math.max(1, durationTicks);
            int finalRadius = Math.max(1, radius);
            TheEndUpdate.LOGGER.info("[EndUpdate] Preparing to send START_FLASH packet: duration={}, radius={}, center={}", 
                finalDuration, finalRadius, center);
            
            EndFlashPayload payload = new EndFlashPayload(finalDuration, finalRadius, center);
            
            if (!ClientPlayNetworking.canSend(EndFlashPayload.ID)) {
                TheEndUpdate.LOGGER.error("[EndUpdate] ========== NETWORK ERROR ==========");
                TheEndUpdate.LOGGER.error("[EndUpdate] Cannot send START_FLASH packet - channel not available!");
                TheEndUpdate.LOGGER.error("[EndUpdate] This usually means the server doesn't have the mod installed or network registration failed");
                TheEndUpdate.LOGGER.error("[EndUpdate] ===================================");
                return;
            }
            
            ClientPlayNetworking.send(payload);
            TheEndUpdate.LOGGER.info("[EndUpdate] START_FLASH packet queued for transmission: duration={}, radius={}, center={}", 
                finalDuration, finalRadius, center);
        } catch (Exception e) {
            TheEndUpdate.LOGGER.error("[EndUpdate] ========== PACKET SEND ERROR ==========");
            TheEndUpdate.LOGGER.error("[EndUpdate] Failed to send START_FLASH packet:", e);
            TheEndUpdate.LOGGER.error("[EndUpdate] ======================================");
        }
    }
    
    public static void sendFlashEnded() {
        try {
            TheEndUpdate.LOGGER.info("[EndUpdate] Preparing to send FLASH_ENDED packet...");
            
            if (!ClientPlayNetworking.canSend(FlashEndedPayload.ID)) {
                TheEndUpdate.LOGGER.error("[EndUpdate] ========== NETWORK ERROR ==========");
                TheEndUpdate.LOGGER.error("[EndUpdate] Cannot send FLASH_ENDED packet - channel not available!");
                TheEndUpdate.LOGGER.error("[EndUpdate] This usually means the server doesn't have the mod installed or network registration failed");
                TheEndUpdate.LOGGER.error("[EndUpdate] ===================================");
                return;
            }
            
            ClientPlayNetworking.send(new FlashEndedPayload());
            TheEndUpdate.LOGGER.info("[EndUpdate] FLASH_ENDED packet queued for transmission");
        } catch (Exception e) {
            TheEndUpdate.LOGGER.error("[EndUpdate] ========== PACKET SEND ERROR ==========");
            TheEndUpdate.LOGGER.error("[EndUpdate] Failed to send FLASH_ENDED packet:", e);
            TheEndUpdate.LOGGER.error("[EndUpdate] ======================================");
        }
    }
}


