package com.blockvehicle.network;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public record WandSyncPayload(int modeOrdinal, boolean hasCorner1, BlockPos corner1, boolean hasCorner2, BlockPos corner2, boolean hasDriverSeat, BlockPos driverSeat, Direction driverFacing, List<BlockPos> passengerSeats, List<BlockPos> customWheels) implements CustomPayload
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
        buf.writeVarInt(p.passengerSeats.size());
        for (BlockPos pos : p.passengerSeats) {
            buf.writeBlockPos(pos);
        }
        buf.writeVarInt(p.customWheels.size());
        for (BlockPos pos : p.customWheels) {
            buf.writeBlockPos(pos);
        }
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
        int passCount = buf.readVarInt();
        ArrayList<BlockPos> passList = new ArrayList<BlockPos>(passCount);
        for (int i = 0; i < passCount; ++i) {
            passList.add(buf.readBlockPos());
        }
        int wheelCount = buf.readVarInt();
        ArrayList<BlockPos> wheelList = new ArrayList<BlockPos>(wheelCount);
        for (int i = 0; i < wheelCount; ++i) {
            wheelList.add(buf.readBlockPos());
        }
        return new WandSyncPayload(modeOrdinal, hasC1, c1, hasC2, c2, hasDriver, driver, facing, passList, wheelList);
    }

    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}

