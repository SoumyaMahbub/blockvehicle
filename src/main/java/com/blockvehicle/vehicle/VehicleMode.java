package com.blockvehicle.vehicle;

public enum VehicleMode {
    GROUND,
    PLANE;

    public static VehicleMode byName(String name) {
        if (name == null) {
            return GROUND;
        }
        try {
            return valueOf(name.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return GROUND;
        }
    }
}
