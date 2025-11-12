package com.theendupdate.network;

import com.theendupdate.TemplateMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record EndFlashPayload(int durationTicks, int radius, BlockPos center) implements CustomPayload {
    public static final Id<EndFlashPayload> ID = new Id<>(Identifier.of(TemplateMod.MOD_ID, "start_flash"));

    public static final PacketCodec<RegistryByteBuf, EndFlashPayload> CODEC = PacketCodec.of(
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
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}


