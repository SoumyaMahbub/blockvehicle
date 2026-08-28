package com.blockvehicle.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record PlaneSyncPayload(
    int entityId, int sequence,
    double x, double y, double z,
    float velocityX, float velocityY, float velocityZ,
    float qx, float qy, float qz, float qw,
    float throttle, float engineRpm, float pitchRate, float rollRate, float yawRate,
    float stallAmount, float angleOfAttack, int flightState, float propellerAngle
) implements CustomPayload {
    public static final CustomPayload.Id<PlaneSyncPayload> ID = new CustomPayload.Id<>(Identifier.of("blockvehicle", "plane_sync"));
    public static final PacketCodec<PacketByteBuf, PlaneSyncPayload> CODEC = PacketCodec.of(PlaneSyncPayload::write, PlaneSyncPayload::read);

    private static void write(PlaneSyncPayload p, PacketByteBuf buf) {
        buf.writeVarInt(p.entityId);
        buf.writeVarInt(p.sequence);
        buf.writeDouble(p.x);
        buf.writeDouble(p.y);
        buf.writeDouble(p.z);
        buf.writeFloat(p.velocityX);
        buf.writeFloat(p.velocityY);
        buf.writeFloat(p.velocityZ);
        buf.writeFloat(p.qx);
        buf.writeFloat(p.qy);
        buf.writeFloat(p.qz);
        buf.writeFloat(p.qw);
        buf.writeFloat(p.throttle);
        buf.writeFloat(p.engineRpm);
        buf.writeFloat(p.pitchRate);
        buf.writeFloat(p.rollRate);
        buf.writeFloat(p.yawRate);
        buf.writeFloat(p.stallAmount);
        buf.writeFloat(p.angleOfAttack);
        buf.writeVarInt(p.flightState);
        buf.writeFloat(p.propellerAngle);
    }

    private static PlaneSyncPayload read(PacketByteBuf buf) {
        return new PlaneSyncPayload(buf.readVarInt(), buf.readVarInt(), buf.readDouble(), buf.readDouble(), buf.readDouble(),
            buf.readFloat(), buf.readFloat(), buf.readFloat(),
            buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(),
            buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readVarInt(), buf.readFloat());
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
