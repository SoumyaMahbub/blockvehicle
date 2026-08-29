package com.blockvehicle.vehicle;

import java.util.Set;
import java.util.Map;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/** World-space markers collected by the wand before a structure is captured. */
public record PlaneSetup(
    VehicleMode mode,
    BlockPos nose,
    BlockPos leftWingTip,
    BlockPos rightWingTip,
    Set<BlockPos> propellerHubs,
    Set<BlockPos> propellerBlades,
    Map<BlockPos, Direction> propellerAxes,
    Set<BlockPos> counterClockwiseHubs
) {
    public static final PlaneSetup GROUND = new PlaneSetup(VehicleMode.GROUND, null, null, null,
        Set.of(), Set.of(), Map.of(), Set.of());

    public PlaneSetup {
        mode = mode != null ? mode : VehicleMode.GROUND;
        propellerHubs = Set.copyOf(propellerHubs != null ? propellerHubs : Set.of());
        propellerBlades = Set.copyOf(propellerBlades != null ? propellerBlades : Set.of());
        propellerAxes = Map.copyOf(propellerAxes != null ? propellerAxes : Map.of());
        counterClockwiseHubs = Set.copyOf(counterClockwiseHubs != null ? counterClockwiseHubs : Set.of());
    }

    public boolean isCompletePlane() {
        return mode == VehicleMode.PLANE && leftWingTip != null && rightWingTip != null;
    }

    public boolean isCompleteHelicopter() {
        if (mode != VehicleMode.HELICOPTER || propellerHubs.isEmpty() || propellerBlades.isEmpty()) return false;
        return propellerHubs.stream().anyMatch(hub -> {
            Direction axis = propellerAxes.getOrDefault(hub, Direction.UP);
            return axis.getAxis() == Direction.Axis.Y;
        });
    }

    public boolean isAircraft() {
        return mode == VehicleMode.PLANE || mode == VehicleMode.HELICOPTER;
    }
}
