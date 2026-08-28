package com.blockvehicle.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Compact pilot controls. Plane movement is simulated authoritatively by the server. */
public record PlaneInputPayload(int sequence, int flags, float lookYaw, float lookPitch) implements CustomPayload {
    public static final CustomPayload.Id<PlaneInputPayload> ID = new CustomPayload.Id<>(Identifier.of("blockvehicle", "plane_input"));
    public static final PacketCodec<PacketByteBuf, PlaneInputPayload> CODEC = PacketCodec.of(PlaneInputPayload::write, PlaneInputPayload::read);

    private static void write(PlaneInputPayload p, PacketByteBuf buf) {
        buf.writeVarInt(p.sequence);
        buf.writeVarInt(p.flags);
        buf.writeFloat(p.lookYaw);
        buf.writeFloat(p.lookPitch);
    }

    private static PlaneInputPayload read(PacketByteBuf buf) {
        return new PlaneInputPayload(buf.readVarInt(), buf.readVarInt(), buf.readFloat(), buf.readFloat());
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
