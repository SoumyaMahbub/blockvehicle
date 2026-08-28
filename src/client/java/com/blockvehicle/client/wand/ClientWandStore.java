package com.blockvehicle.client.wand;

import com.blockvehicle.item.PlayerDataStore;
import com.blockvehicle.network.WandSyncPayload;
import com.blockvehicle.vehicle.VehicleMode;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

@Environment(value=EnvType.CLIENT)
public final class ClientWandStore {
    private static PlayerDataStore.WandMode mode = PlayerDataStore.WandMode.SELECT_REGION;
    private static BlockPos corner1 = null;
    private static BlockPos corner2 = null;
    private static BlockPos driverSeat = null;
    private static Direction driverFacing = Direction.SOUTH;
    private static List<BlockPos> passengerSeats = List.of();
    private static List<BlockPos> customWheels = List.of();
    private static VehicleMode vehicleMode = VehicleMode.GROUND;
    private static BlockPos planeNose;
    private static BlockPos leftWingTip;
    private static BlockPos rightWingTip;
    private static List<BlockPos> propellerHubs = List.of();
    private static List<BlockPos> propellerBlades = List.of();

    private ClientWandStore() {
    }

    public static void applySync(WandSyncPayload payload) {
        PlayerDataStore.WandMode[] modes = PlayerDataStore.WandMode.values();
        int ord = payload.modeOrdinal();
        mode = ord >= 0 && ord < modes.length ? modes[ord] : PlayerDataStore.WandMode.SELECT_REGION;
        corner1 = payload.hasCorner1() ? payload.corner1() : null;
        corner2 = payload.hasCorner2() ? payload.corner2() : null;
        driverSeat = payload.hasDriverSeat() ? payload.driverSeat() : null;
        driverFacing = payload.driverFacing() != null ? payload.driverFacing() : Direction.SOUTH;
        passengerSeats = new ArrayList<BlockPos>(payload.passengerSeats());
        customWheels = new ArrayList<BlockPos>(payload.customWheels());
        VehicleMode[] vehicleModes = VehicleMode.values();
        vehicleMode = payload.vehicleModeOrdinal() >= 0 && payload.vehicleModeOrdinal() < vehicleModes.length
            ? vehicleModes[payload.vehicleModeOrdinal()] : VehicleMode.GROUND;
        planeNose = payload.planeNose();
        leftWingTip = payload.leftWingTip();
        rightWingTip = payload.rightWingTip();
        propellerHubs = new ArrayList<>(payload.propellerHubs());
        propellerBlades = new ArrayList<>(payload.propellerBlades());
    }

    public static PlayerDataStore.WandMode getMode() {
        return mode;
    }

    public static BlockPos getCorner1() {
        return corner1;
    }

    public static BlockPos getCorner2() {
        return corner2;
    }

    public static BlockPos getDriverSeat() {
        return driverSeat;
    }

    public static Direction getDriverFacing() {
        return driverFacing;
    }

    public static List<BlockPos> getPassengerSeats() {
        return passengerSeats;
    }

    public static List<BlockPos> getCustomWheels() {
        return customWheels;
    }

    public static VehicleMode getVehicleMode() { return vehicleMode; }
    public static BlockPos getPlaneNose() { return planeNose; }
    public static BlockPos getLeftWingTip() { return leftWingTip; }
    public static BlockPos getRightWingTip() { return rightWingTip; }
    public static List<BlockPos> getPropellerHubs() { return propellerHubs; }
    public static List<BlockPos> getPropellerBlades() { return propellerBlades; }

    public static boolean isReadyToActivate() {
        return corner1 != null && corner2 != null
            && (vehicleMode != VehicleMode.PLANE || driverSeat != null && leftWingTip != null && rightWingTip != null);
    }

    public static void reset() {
        mode = PlayerDataStore.WandMode.SELECT_REGION;
        corner1 = null;
        corner2 = null;
        driverSeat = null;
        driverFacing = Direction.SOUTH;
        passengerSeats = List.of();
        customWheels = List.of();
        vehicleMode = VehicleMode.GROUND;
        planeNose = null;
        leftWingTip = null;
        rightWingTip = null;
        propellerHubs = List.of();
        propellerBlades = List.of();
    }

    public static Box getSelectionBox() {
        if (corner1 == null || corner2 == null) {
            return null;
        }
        int x0 = Math.min(corner1.getX(), corner2.getX());
        int y0 = Math.min(corner1.getY(), corner2.getY());
        int z0 = Math.min(corner1.getZ(), corner2.getZ());
        int x1 = Math.max(corner1.getX(), corner2.getX()) + 1;
        int y1 = Math.max(corner1.getY(), corner2.getY()) + 1;
        int z1 = Math.max(corner1.getZ(), corner2.getZ()) + 1;
        return new Box((double)x0, (double)y0, (double)z0, (double)x1, (double)y1, (double)z1);
    }

    public static int getWidth() {
        if (corner1 == null || corner2 == null) {
            return 0;
        }
        return Math.abs(corner1.getX() - corner2.getX()) + 1;
    }

    public static int getHeight() {
        if (corner1 == null || corner2 == null) {
            return 0;
        }
        return Math.abs(corner1.getY() - corner2.getY()) + 1;
    }

    public static int getLength() {
        if (corner1 == null || corner2 == null) {
            return 0;
        }
        return Math.abs(corner1.getZ() - corner2.getZ()) + 1;
    }

    public static int getBlockVolume() {
        return ClientWandStore.getWidth() * ClientWandStore.getHeight() * ClientWandStore.getLength();
    }
}
