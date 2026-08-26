package com.blockvehicle.item;

import com.blockvehicle.network.WandSyncPayload;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public final class PlayerDataStore {
    private static final Map<UUID, WandMode> modeMap = new HashMap<UUID, WandMode>();
    private static final Map<UUID, BlockPos> corner1Map = new HashMap<UUID, BlockPos>();
    private static final Map<UUID, BlockPos> corner2Map = new HashMap<UUID, BlockPos>();
    private static final Map<UUID, BlockPos> driverSeatMap = new HashMap<UUID, BlockPos>();
    private static final Map<UUID, Direction> driverFacingMap = new HashMap<UUID, Direction>();
    private static final Map<UUID, Set<BlockPos>> passengerSeatsMap = new HashMap<UUID, Set<BlockPos>>();
    private static final Map<UUID, Set<BlockPos>> customWheelsMap = new HashMap<UUID, Set<BlockPos>>();

    private PlayerDataStore() {
    }

    public static WandMode getMode(UUID uuid) {
        return modeMap.getOrDefault(uuid, WandMode.SELECT_REGION);
    }

    public static WandMode cycleMode(UUID uuid) {
        WandMode next = PlayerDataStore.getMode(uuid).next();
        modeMap.put(uuid, next);
        return next;
    }

    public static void setCorner1(UUID uuid, BlockPos pos) {
        corner1Map.put(uuid, pos);
    }

    public static void setCorner2(UUID uuid, BlockPos pos) {
        corner2Map.put(uuid, pos);
    }

    public static BlockPos getCorner1(UUID uuid) {
        return corner1Map.get(uuid);
    }

    public static BlockPos getCorner2(UUID uuid) {
        return corner2Map.get(uuid);
    }

    public static void setDriverSeat(UUID uuid, BlockPos pos, Direction facing) {
        driverSeatMap.put(uuid, pos);
        driverFacingMap.put(uuid, facing);
    }

    public static BlockPos getDriverSeat(UUID uuid) {
        return driverSeatMap.get(uuid);
    }

    public static Direction getDriverFacing(UUID uuid) {
        return driverFacingMap.get(uuid);
    }

    public static boolean togglePassengerSeat(UUID uuid, BlockPos pos) {
        Set set = passengerSeatsMap.computeIfAbsent(uuid, k -> new HashSet());
        if (set.contains(pos)) {
            set.remove(pos);
            return false;
        }
        set.add(pos);
        return true;
    }

    public static Set<BlockPos> getPassengerSeats(UUID uuid) {
        return passengerSeatsMap.getOrDefault(uuid, Set.of());
    }

    public static boolean toggleWheel(UUID uuid, BlockPos pos) {
        Set set = customWheelsMap.computeIfAbsent(uuid, k -> new HashSet());
        if (set.contains(pos)) {
            set.remove(pos);
            return false;
        }
        set.add(pos);
        return true;
    }

    public static Set<BlockPos> getCustomWheels(UUID uuid) {
        return customWheelsMap.getOrDefault(uuid, Set.of());
    }

    public static void clear(UUID uuid) {
        corner1Map.remove(uuid);
        corner2Map.remove(uuid);
        driverSeatMap.remove(uuid);
        driverFacingMap.remove(uuid);
        passengerSeatsMap.remove(uuid);
        customWheelsMap.remove(uuid);
    }

    public static void syncToPlayer(ServerPlayerEntity player) {
        if (player == null) {
            return;
        }
        UUID uuid = player.getUuid();
        WandMode mode = PlayerDataStore.getMode(uuid);
        BlockPos c1 = PlayerDataStore.getCorner1(uuid);
        BlockPos c2 = PlayerDataStore.getCorner2(uuid);
        BlockPos ds = PlayerDataStore.getDriverSeat(uuid);
        Direction df = PlayerDataStore.getDriverFacing(uuid);
        ArrayList<BlockPos> passList = new ArrayList<BlockPos>(PlayerDataStore.getPassengerSeats(uuid));
        ArrayList<BlockPos> wheelList = new ArrayList<BlockPos>(PlayerDataStore.getCustomWheels(uuid));
        WandSyncPayload pkt = new WandSyncPayload(mode.ordinal(), c1 != null, c1, c2 != null, c2, ds != null, ds, df, passList, wheelList);
        ServerPlayNetworking.send((ServerPlayerEntity)player, (CustomPayload)pkt);
    }

    public static enum WandMode {
        SELECT_REGION("\u00a7b\ud83d\udcd0 Select Region", "Right-click = Corner 1 | Sneak+Right-click = Corner 2"),
        SET_DRIVER_SEAT("\u00a76\ud83d\udcba Set Driver Seat", "Right-click any block to set as Driver Seat"),
        SET_PASSENGER_SEAT("\u00a7d\ud83d\udc65 Add/Remove Passenger Seat", "Right-click any block to toggle Passenger Seat"),
        SET_WHEEL("\u00a7e\ud83d\ude97 Add/Remove Wheel", "Right-click any block to toggle as rotating Wheel"),
        ACTIVATE("\u00a7a\u26a1 Activate Vehicle", "Right-click in air or on build to activate!");

        public final String title;
        public final String hint;

        private WandMode(String title, String hint) {
            this.title = title;
            this.hint = hint;
        }

        public WandMode next() {
            WandMode[] vals = WandMode.values();
            return vals[(this.ordinal() + 1) % vals.length];
        }
    }
}

