package com.blockvehicle.network;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public record WandSyncPayload(int modeOrdinal, boolean hasCorner1, BlockPos corner1, boolean hasCorner2, BlockPos corner2,
                              boolean hasDriverSeat, BlockPos driverSeat, Direction driverFacing,
                              List<BlockPos> passengerSeats, List<BlockPos> customWheels,
                              int vehicleModeOrdinal, BlockPos planeNose, BlockPos leftWingTip, BlockPos rightWingTip,
                              List<BlockPos> propellerHubs, List<BlockPos> propellerBlades) implements CustomPayload
{
    public static final CustomPayload.Id<WandSyncPayload> ID = new CustomPayload.Id(Identifier.of("blockvehicle", "wand_sync"));
    public static final PacketCodec<PacketByteBuf, WandSyncPayload> CODEC = PacketCodec.of(WandSyncPayload::write, WandSyncPayload::read);

    private static void write(WandSyncPayload p, PacketByteBuf buf) {
        buf.writeVarInt(p.modeOrdinal);
        buf.writeBoolean(p.hasCorner1);
        if (p.hasCorner1) {
            buf.writeBlockPos(p.corner1);
        }
        buf.writeBoolean(p.hasCorner2);
        if (p.hasCorner2) {
            buf.writeBlockPos(p.corner2);
        }
        buf.writeBoolean(p.hasDriverSeat);
        if (p.hasDriverSeat) {
            buf.writeBlockPos(p.driverSeat);
            buf.writeVarInt(p.driverFacing != null ? p.driverFacing.getId() : Direction.SOUTH.getId());
        }
        writePositions(buf, p.passengerSeats);
        writePositions(buf, p.customWheels);
        buf.writeVarInt(p.vehicleModeOrdinal);
        writeOptionalPos(buf, p.planeNose);
        writeOptionalPos(buf, p.leftWingTip);
        writeOptionalPos(buf, p.rightWingTip);
        writePositions(buf, p.propellerHubs);
        writePositions(buf, p.propellerBlades);
    }

    private static WandSyncPayload read(PacketByteBuf buf) {
        int modeOrdinal = buf.readVarInt();
        boolean hasC1 = buf.readBoolean();
        BlockPos c1 = hasC1 ? buf.readBlockPos() : null;
        boolean hasC2 = buf.readBoolean();
        BlockPos c2 = hasC2 ? buf.readBlockPos() : null;
        boolean hasDriver = buf.readBoolean();
        BlockPos driver = null;
        Direction facing = Direction.SOUTH;
        if (hasDriver) {
            driver = buf.readBlockPos();
            facing = Direction.byId((int)buf.readVarInt());
        }
        List<BlockPos> passList = readPositions(buf);
        List<BlockPos> wheelList = readPositions(buf);
        int vehicleMode = buf.readVarInt();
        BlockPos nose = readOptionalPos(buf);
        BlockPos leftWing = readOptionalPos(buf);
        BlockPos rightWing = readOptionalPos(buf);
        List<BlockPos> hubs = readPositions(buf);
        List<BlockPos> blades = readPositions(buf);
        return new WandSyncPayload(modeOrdinal, hasC1, c1, hasC2, c2, hasDriver, driver, facing,
            passList, wheelList, vehicleMode, nose, leftWing, rightWing, hubs, blades);
    }

    private static void writeOptionalPos(PacketByteBuf buf, BlockPos pos) {
        buf.writeBoolean(pos != null);
        if (pos != null) buf.writeBlockPos(pos);
    }

    private static BlockPos readOptionalPos(PacketByteBuf buf) {
        return buf.readBoolean() ? buf.readBlockPos() : null;
    }

    private static void writePositions(PacketByteBuf buf, List<BlockPos> positions) {
        int count = Math.min(positions.size(), 2048);
        buf.writeVarInt(count);
        for (int i = 0; i < count; ++i) buf.writeBlockPos(positions.get(i));
    }

    private static List<BlockPos> readPositions(PacketByteBuf buf) {
        int declaredCount = Math.max(0, buf.readVarInt());
        int keptCount = Math.min(declaredCount, 2048);
        ArrayList<BlockPos> result = new ArrayList<>(keptCount);
        for (int i = 0; i < declaredCount; ++i) {
            BlockPos pos = buf.readBlockPos();
            if (i < keptCount) result.add(pos);
        }
        return result;
    }

    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
