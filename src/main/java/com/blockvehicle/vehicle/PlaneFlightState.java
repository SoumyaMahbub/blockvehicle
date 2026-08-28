package com.blockvehicle.vehicle;

public enum PlaneFlightState {
    PARKED,
    TAXIING,
    TAKEOFF,
    AIRBORNE,
    STALLING,
    LANDING,
    HARD_LANDING,
    CRASHED;

    public static PlaneFlightState byOrdinal(int ordinal) {
        PlaneFlightState[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : PARKED;
    }
}
