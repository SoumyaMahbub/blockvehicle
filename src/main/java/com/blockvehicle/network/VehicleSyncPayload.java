package com.blockvehicle.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record VehicleSyncPayload(int entityId, double x, double y, double z, float yaw, float speed, float pitch, float roll, float angularVelocity) implements CustomPayload
{
    public static final CustomPayload.Id<VehicleSyncPayload> ID = new CustomPayload.Id(Identifier.of("blockvehicle", "vehicle_sync"));
    public static final PacketCodec<PacketByteBuf, VehicleSyncPayload> CODEC = PacketCodec.of(VehicleSyncPayload::write, VehicleSyncPayload::read);

    private static void write(VehicleSyncPayload p, PacketByteBuf buf) {
        buf.writeInt(p.entityId);
        buf.writeDouble(p.x);
        buf.writeDouble(p.y);
        buf.writeDouble(p.z);
        buf.writeFloat(p.yaw);
        buf.writeFloat(p.speed);
        buf.writeFloat(p.pitch);
        buf.writeFloat(p.roll);
        buf.writeFloat(p.angularVelocity);
    }

    private static VehicleSyncPayload read(PacketByteBuf buf) {
        return new VehicleSyncPayload(buf.readInt(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat());
    }

    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}

