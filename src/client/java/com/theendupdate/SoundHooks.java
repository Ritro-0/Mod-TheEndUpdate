package com.theendupdate;

import com.theendupdate.network.EndFlashClient;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public final class SoundHooks {
    private static final int FLASH_DURATION_TICKS = 240;
    private static final int FLASH_RADIUS_BLOCKS = 48;
    private static final int MIN_RETRIGGER_TICKS = 8;

    private static long activeFlashEndTick = -1L;
    private static long lastFlashTriggerTick = -1L;
    private static boolean registered;

    private SoundHooks() {}

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level == null) {
                activeFlashEndTick = -1L;
                return;
            }
            if (activeFlashEndTick < 0L) {
                return;
            }

            long now = client.level.getGameTime();
            if (now >= activeFlashEndTick) {
                EndFlashClient.sendFlashEnded();
                activeFlashEndTick = -1L;
            }
        });
    }

    public static void onSoundPlayed(SoundInstance sound) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || sound == null) {
            return;
        }
        if (minecraft.level.dimension() != Level.END) {
            return;
        }

        if (!isEndFlashTrigger(sound)) {
            return;
        }

        long now = minecraft.level.getGameTime();
        if (lastFlashTriggerTick >= 0L && (now - lastFlashTriggerTick) < MIN_RETRIGGER_TICKS) {
            activeFlashEndTick = Math.max(activeFlashEndTick, now + FLASH_DURATION_TICKS);
            return;
        }

        lastFlashTriggerTick = now;
        activeFlashEndTick = now + FLASH_DURATION_TICKS;

        BlockPos center = BlockPos.containing(sound.getX(), sound.getY(), sound.getZ());
        if (center.equals(BlockPos.ZERO)) {
            center = minecraft.player.blockPosition();
        }

        EndFlashClient.sendStartFlash(FLASH_DURATION_TICKS, FLASH_RADIUS_BLOCKS, center);
    }

    private static boolean isEndFlashTrigger(SoundInstance sound) {
        String soundKey = resolveSoundKey(sound).toLowerCase(java.util.Locale.ROOT);
        String subtitleKey = resolveSubtitleKey(sound).toLowerCase(java.util.Locale.ROOT);
        String fallbackKey = String.valueOf(sound).toLowerCase(java.util.Locale.ROOT);
        String combined = soundKey + " | " + subtitleKey + " | " + fallbackKey;

        boolean idMatch = combined.contains("weather.end_flash");
        boolean subtitleMatch = combined.contains("subtitles.weather.end_flash");
        boolean genericEndFlashMatch = combined.contains("end_flash");
        return idMatch || subtitleMatch || genericEndFlashMatch;
    }

    private static String resolveSoundKey(SoundInstance sound) {
        Object snd = null;
        try {
            snd = sound.getSound();
        } catch (Exception ignored) {
        }
        if (snd != null) {
            String viaLocation = tryInvokeStringMethod(snd, "location");
            if (!viaLocation.isEmpty()) return viaLocation;
            viaLocation = tryInvokeStringMethod(snd, "getLocation");
            if (!viaLocation.isEmpty()) return viaLocation;
            String viaPath = tryInvokeStringMethod(snd, "path");
            if (!viaPath.isEmpty()) return viaPath;
            viaPath = tryInvokeStringMethod(snd, "getPath");
            if (!viaPath.isEmpty()) return viaPath;
            String sndStr = String.valueOf(snd);
            if (sndStr != null && !sndStr.isBlank()) return sndStr;
        }
        return String.valueOf(sound);
    }

    private static String tryInvokeStringMethod(Object target, String methodName) {
        try {
            java.lang.reflect.Method method = target.getClass().getMethod(methodName);
            Object result = method.invoke(target);
            return result == null ? "" : String.valueOf(result);
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String resolveSubtitleKey(SoundInstance sound) {
        Object snd = null;
        try {
            snd = sound.getSound();
        } catch (Exception ignored) {
        }
        if (snd == null) {
            return "";
        }

        try {
            java.lang.reflect.Method subtitleMethod = snd.getClass().getMethod("getSubtitle");
            Object subtitleComponent = subtitleMethod.invoke(snd);
            if (subtitleComponent != null) {
                String key = tryInvokeStringMethod(subtitleComponent, "getKey");
                if (!key.isEmpty()) {
                    return key;
                }
                String contents = tryInvokeStringMethod(subtitleComponent, "getContents");
                if (!contents.isEmpty()) {
                    return contents;
                }
                return String.valueOf(subtitleComponent);
            }
        } catch (Exception ignored) {
            // subtitle API differs across versions/mappings
        }
        return "";
    }
}


