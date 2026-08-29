package com.blockvehicle.item;

import com.blockvehicle.network.WandSyncPayload;
import com.blockvehicle.vehicle.PlaneSetup;
import com.blockvehicle.vehicle.VehicleMode;
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
    private static final Map<UUID, VehicleMode> vehicleModeMap = new HashMap<>();
    private static final Map<UUID, BlockPos> planeNoseMap = new HashMap<>();
    private static final Map<UUID, BlockPos> leftWingMap = new HashMap<>();
    private static final Map<UUID, BlockPos> rightWingMap = new HashMap<>();
    private static final Map<UUID, Set<BlockPos>> propellerHubsMap = new HashMap<>();
    private static final Map<UUID, Set<BlockPos>> propellerBladesMap = new HashMap<>();
    private static final Map<UUID, Map<BlockPos, Direction>> propellerAxesMap = new HashMap<>();
    private static final Map<UUID, Set<BlockPos>> counterClockwiseHubsMap = new HashMap<>();

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

    public static VehicleMode getVehicleMode(UUID uuid) {
        return vehicleModeMap.getOrDefault(uuid, VehicleMode.GROUND);
    }

    public static void setPlaneNose(UUID uuid, BlockPos pos) {
        vehicleModeMap.put(uuid, VehicleMode.PLANE);
        planeNoseMap.put(uuid, pos);
    }

    public static void setHelicopterNose(UUID uuid, BlockPos pos) {
        vehicleModeMap.put(uuid, VehicleMode.HELICOPTER);
        planeNoseMap.put(uuid, pos);
        leftWingMap.remove(uuid);
        rightWingMap.remove(uuid);
    }

    public static void setVehicleMode(UUID uuid, VehicleMode mode) {
        vehicleModeMap.put(uuid, mode != null ? mode : VehicleMode.GROUND);
    }

    public static void setLeftWingTip(UUID uuid, BlockPos pos) {
        vehicleModeMap.put(uuid, VehicleMode.PLANE);
        leftWingMap.put(uuid, pos);
    }

    public static void setRightWingTip(UUID uuid, BlockPos pos) {
        vehicleModeMap.put(uuid, VehicleMode.PLANE);
        rightWingMap.put(uuid, pos);
    }

    public static BlockPos getPlaneNose(UUID uuid) { return planeNoseMap.get(uuid); }
    public static BlockPos getLeftWingTip(UUID uuid) { return leftWingMap.get(uuid); }
    public static BlockPos getRightWingTip(UUID uuid) { return rightWingMap.get(uuid); }

    public static boolean togglePropellerHub(UUID uuid, BlockPos pos) {
        return togglePropellerHub(uuid, pos, Direction.SOUTH);
    }

    public static boolean togglePropellerHub(UUID uuid, BlockPos pos, Direction axis) {
        vehicleModeMap.putIfAbsent(uuid, VehicleMode.PLANE);
        Set<BlockPos> set = propellerHubsMap.computeIfAbsent(uuid, ignored -> new HashSet<>());
        if (set.remove(pos)) {
            propellerAxesMap.computeIfAbsent(uuid, ignored -> new HashMap<>()).remove(pos);
            counterClockwiseHubsMap.computeIfAbsent(uuid, ignored -> new HashSet<>()).remove(pos);
            return false;
        }
        set.add(pos);
        propellerAxesMap.computeIfAbsent(uuid, ignored -> new HashMap<>()).put(pos, axis != null ? axis : Direction.SOUTH);
        return true;
    }

    public static void setPropellerHub(UUID uuid, BlockPos pos, Direction axis, boolean clockwise) {
        vehicleModeMap.putIfAbsent(uuid, VehicleMode.PLANE);
        propellerHubsMap.computeIfAbsent(uuid, ignored -> new HashSet<>()).add(pos);
        propellerAxesMap.computeIfAbsent(uuid, ignored -> new HashMap<>()).put(pos, axis != null ? axis : Direction.SOUTH);
        Set<BlockPos> reversed = counterClockwiseHubsMap.computeIfAbsent(uuid, ignored -> new HashSet<>());
        if (clockwise) reversed.remove(pos); else reversed.add(pos);
    }

    public static boolean togglePropellerSpin(UUID uuid, BlockPos pos) {
        if (!getPropellerHubs(uuid).contains(pos)) return false;
        Set<BlockPos> reversed = counterClockwiseHubsMap.computeIfAbsent(uuid, ignored -> new HashSet<>());
        if (reversed.remove(pos)) return true;
        reversed.add(pos);
        return false;
    }

    public static boolean togglePropellerBlade(UUID uuid, BlockPos pos) {
        vehicleModeMap.putIfAbsent(uuid, VehicleMode.PLANE);
        Set<BlockPos> set = propellerBladesMap.computeIfAbsent(uuid, ignored -> new HashSet<>());
        return set.remove(pos) ? false : set.add(pos);
    }

    public static Set<BlockPos> getPropellerHubs(UUID uuid) {
        return propellerHubsMap.getOrDefault(uuid, Set.of());
    }

    public static Set<BlockPos> getPropellerBlades(UUID uuid) {
        return propellerBladesMap.getOrDefault(uuid, Set.of());
    }

    public static Map<BlockPos, Direction> getPropellerAxes(UUID uuid) {
        return propellerAxesMap.getOrDefault(uuid, Map.of());
    }

    public static Set<BlockPos> getCounterClockwiseHubs(UUID uuid) {
        return counterClockwiseHubsMap.getOrDefault(uuid, Set.of());
    }

    public static PlaneSetup getPlaneSetup(UUID uuid) {
        return new PlaneSetup(getVehicleMode(uuid), getPlaneNose(uuid), getLeftWingTip(uuid), getRightWingTip(uuid),
            getPropellerHubs(uuid), getPropellerBlades(uuid), getPropellerAxes(uuid), getCounterClockwiseHubs(uuid));
    }

    public static void clearPlaneSetup(UUID uuid) {
        vehicleModeMap.put(uuid, VehicleMode.GROUND);
        planeNoseMap.remove(uuid);
        leftWingMap.remove(uuid);
        rightWingMap.remove(uuid);
        propellerHubsMap.remove(uuid);
        propellerBladesMap.remove(uuid);
        propellerAxesMap.remove(uuid);
        counterClockwiseHubsMap.remove(uuid);
    }

    public static void clear(UUID uuid) {
        corner1Map.remove(uuid);
        corner2Map.remove(uuid);
        driverSeatMap.remove(uuid);
        driverFacingMap.remove(uuid);
        passengerSeatsMap.remove(uuid);
        customWheelsMap.remove(uuid);
        vehicleModeMap.remove(uuid);
        planeNoseMap.remove(uuid);
        leftWingMap.remove(uuid);
        rightWingMap.remove(uuid);
        propellerHubsMap.remove(uuid);
        propellerBladesMap.remove(uuid);
        propellerAxesMap.remove(uuid);
        counterClockwiseHubsMap.remove(uuid);
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
        PlaneSetup plane = getPlaneSetup(uuid);
        WandSyncPayload pkt = new WandSyncPayload(mode.ordinal(), c1 != null, c1, c2 != null, c2, ds != null, ds, df,
            passList, wheelList, plane.mode().ordinal(), plane.nose(), plane.leftWingTip(), plane.rightWingTip(),
            new ArrayList<>(plane.propellerHubs()), new ArrayList<>(plane.propellerBlades()));
        ServerPlayNetworking.send((ServerPlayerEntity)player, (CustomPayload)pkt);
    }

    public static enum WandMode {
        SELECT_REGION("\u00a7b\ud83d\udcd0 Select Region", "Right-click = Corner 1 | Sneak+Right-click = Corner 2"),
        SET_DRIVER_SEAT("\u00a76\ud83d\udcba Set Driver Seat", "Right-click any block to set as Driver Seat"),
        SET_PASSENGER_SEAT("\u00a7d\ud83d\udc65 Add/Remove Passenger Seat", "Right-click any block to toggle Passenger Seat"),
        SET_WHEEL("\u00a7e\ud83d\ude97 Add/Remove Wheel", "Right-click any block to toggle as rotating Wheel"),
        SET_PLANE_NOSE("\u00a7b\u2708 Plane / Nose", "Right-click nose = Plane Mode | Sneak+Right-click = Ground Mode"),
        SET_HELICOPTER_NOSE("\u00a7a Helicopter / Nose", "Right-click nose = Helicopter Mode | main rotor axis must face UP/DOWN"),
        SET_WING_TIPS("\u00a7d\u2194 Plane Wing Tips", "Right-click = Left Wing | Sneak+Right-click = Right Wing"),
        SET_PROPELLER("\u00a7e\u2699 Plane Propeller", "Right-click hub face = Axis | Sneak hub = Reverse spin | Sneak blade = Toggle"),
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
