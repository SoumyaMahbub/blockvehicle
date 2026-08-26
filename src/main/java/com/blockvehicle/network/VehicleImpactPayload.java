package com.blockvehicle.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record VehicleImpactPayload(int entityId, float impulseX, float impulseY, float impulseZ, float angularImpulse, float resultingSpeed) implements CustomPayload {
    public static final CustomPayload.Id<VehicleImpactPayload> ID = new CustomPayload.Id<>(Identifier.of("blockvehicle", "vehicle_impact"));
    public static final PacketCodec<PacketByteBuf, VehicleImpactPayload> CODEC = PacketCodec.of(VehicleImpactPayload::write, VehicleImpactPayload::read);

    private static void write(VehicleImpactPayload payload, PacketByteBuf buf) {
        buf.writeInt(payload.entityId);
        buf.writeFloat(payload.impulseX);
        buf.writeFloat(payload.impulseY);
        buf.writeFloat(payload.impulseZ);
        buf.writeFloat(payload.angularImpulse);
        buf.writeFloat(payload.resultingSpeed);
    }

    private static VehicleImpactPayload read(PacketByteBuf buf) {
        return new VehicleImpactPayload(buf.readInt(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat());
    }

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
