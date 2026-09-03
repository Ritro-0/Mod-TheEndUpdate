package com.theendupdate.network;

import com.theendupdate.TheEndUpdate;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record FlashEndedPayload() implements CustomPacketPayload {
    public static final Type<FlashEndedPayload> ID = new Type<>(Identifier.fromNamespaceAndPath(TheEndUpdate.MOD_ID, "flash_ended"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FlashEndedPayload> CODEC = StreamCodec.ofMember(
        (payload, buf) -> {
            // no data, just a signal
        },
        buf -> new FlashEndedPayload()
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}

