package com.blockvehicle.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record VehicleHornPayload(int vehicleId) implements CustomPayload
{
    public static final CustomPayload.Id<VehicleHornPayload> ID = new CustomPayload.Id(Identifier.of("blockvehicle", "vehicle_horn"));
    public static final PacketCodec<PacketByteBuf, VehicleHornPayload> CODEC = PacketCodec.of(VehicleHornPayload::write, VehicleHornPayload::read);

    private static void write(VehicleHornPayload payload, PacketByteBuf buf) {
        buf.writeInt(payload.vehicleId);
    }

    private static VehicleHornPayload read(PacketByteBuf buf) {
        return new VehicleHornPayload(buf.readInt());
    }

    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}

