package com.blockvehicle.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record VehicleInputPayload(boolean forward, boolean backward, boolean left, boolean right, boolean brake, double x, double y, double z, float yaw, float speed, float pitch, float roll) implements CustomPayload
{
    public static final CustomPayload.Id<VehicleInputPayload> ID = new CustomPayload.Id(Identifier.of("blockvehicle", "vehicle_input"));
    public static final PacketCodec<PacketByteBuf, VehicleInputPayload> CODEC = PacketCodec.of(VehicleInputPayload::write, VehicleInputPayload::read);

    private static void write(VehicleInputPayload payload, PacketByteBuf buf) {
        int flags = 0;
        if (payload.forward) {
            flags |= 1;
        }
        if (payload.backward) {
            flags |= 2;
        }
        if (payload.left) {
            flags |= 4;
        }
        if (payload.right) {
            flags |= 8;
        }
        if (payload.brake) {
            flags |= 0x10;
        }
        buf.writeByte(flags);
        buf.writeDouble(payload.x);
        buf.writeDouble(payload.y);
        buf.writeDouble(payload.z);
        buf.writeFloat(payload.yaw);
        buf.writeFloat(payload.speed);
        buf.writeFloat(payload.pitch);
        buf.writeFloat(payload.roll);
    }

    private static VehicleInputPayload read(PacketByteBuf buf) {
        int flags = buf.readByte() & 0xFF;
        return new VehicleInputPayload((flags & 1) != 0, (flags & 2) != 0, (flags & 4) != 0, (flags & 8) != 0, (flags & 0x10) != 0, buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat());
    }

    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
