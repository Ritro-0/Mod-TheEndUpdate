package com.theendupdate.network;

import com.theendupdate.TheEndUpdate;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record EndFlashPayload(int durationTicks, int radius, BlockPos center) implements CustomPacketPayload {
    public static final Type<EndFlashPayload> ID = new Type<>(Identifier.fromNamespaceAndPath(TheEndUpdate.MOD_ID, "start_flash"));

    public static final StreamCodec<RegistryFriendlyByteBuf, EndFlashPayload> CODEC = StreamCodec.ofMember(
        (payload, buf) -> {
            buf.writeVarInt(payload.durationTicks);
            buf.writeVarInt(payload.radius);
            buf.writeBlockPos(payload.center);
        },
        buf -> new EndFlashPayload(
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readBlockPos()
        )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}


