package com.theendupdate.network;

import com.theendupdate.TemplateMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record FlashEndedPayload() implements CustomPayload {
    public static final Id<FlashEndedPayload> ID = new Id<>(Identifier.of(TemplateMod.MOD_ID, "flash_ended"));

    public static final PacketCodec<RegistryByteBuf, FlashEndedPayload> CODEC = PacketCodec.of(
        (payload, buf) -> {
            // No data needed - just a signal
        },
        buf -> new FlashEndedPayload()
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

