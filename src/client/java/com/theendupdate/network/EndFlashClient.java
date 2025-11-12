package com.theendupdate.network;

import com.theendupdate.TemplateMod;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.util.math.BlockPos;

public final class EndFlashClient {
    private EndFlashClient() {}

    public static void sendStartFlash(int durationTicks, int radius, BlockPos center) {
        try {
            int finalDuration = Math.max(1, durationTicks);
            int finalRadius = Math.max(1, radius);
            TemplateMod.LOGGER.info("[EndUpdate] Preparing to send START_FLASH packet: duration={}, radius={}, center={}", 
                finalDuration, finalRadius, center);
            
            EndFlashPayload payload = new EndFlashPayload(finalDuration, finalRadius, center);
            
            if (!ClientPlayNetworking.canSend(EndFlashPayload.ID)) {
                TemplateMod.LOGGER.error("[EndUpdate] ========== NETWORK ERROR ==========");
                TemplateMod.LOGGER.error("[EndUpdate] Cannot send START_FLASH packet - channel not available!");
                TemplateMod.LOGGER.error("[EndUpdate] This usually means the server doesn't have the mod installed or network registration failed");
                TemplateMod.LOGGER.error("[EndUpdate] ===================================");
                return;
            }
            
            ClientPlayNetworking.send(payload);
            TemplateMod.LOGGER.info("[EndUpdate] START_FLASH packet queued for transmission: duration={}, radius={}, center={}", 
                finalDuration, finalRadius, center);
        } catch (Exception e) {
            TemplateMod.LOGGER.error("[EndUpdate] ========== PACKET SEND ERROR ==========");
            TemplateMod.LOGGER.error("[EndUpdate] Failed to send START_FLASH packet:", e);
            TemplateMod.LOGGER.error("[EndUpdate] ======================================");
        }
    }
    
    public static void sendFlashEnded() {
        try {
            TemplateMod.LOGGER.info("[EndUpdate] Preparing to send FLASH_ENDED packet...");
            
            if (!ClientPlayNetworking.canSend(FlashEndedPayload.ID)) {
                TemplateMod.LOGGER.error("[EndUpdate] ========== NETWORK ERROR ==========");
                TemplateMod.LOGGER.error("[EndUpdate] Cannot send FLASH_ENDED packet - channel not available!");
                TemplateMod.LOGGER.error("[EndUpdate] This usually means the server doesn't have the mod installed or network registration failed");
                TemplateMod.LOGGER.error("[EndUpdate] ===================================");
                return;
            }
            
            ClientPlayNetworking.send(new FlashEndedPayload());
            TemplateMod.LOGGER.info("[EndUpdate] FLASH_ENDED packet queued for transmission");
        } catch (Exception e) {
            TemplateMod.LOGGER.error("[EndUpdate] ========== PACKET SEND ERROR ==========");
            TemplateMod.LOGGER.error("[EndUpdate] Failed to send FLASH_ENDED packet:", e);
            TemplateMod.LOGGER.error("[EndUpdate] ======================================");
        }
    }
}


