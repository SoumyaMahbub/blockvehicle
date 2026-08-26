package com.blockvehicle.vehicle;

public final class VehicleInputState {
    public final boolean forward;
    public final boolean backward;
    public final boolean left;
    public final boolean right;
    public final boolean brake;
    public static final VehicleInputState EMPTY = new VehicleInputState(false, false, false, false, false);

    public VehicleInputState(boolean forward, boolean backward, boolean left, boolean right, boolean brake) {
        this.forward = forward;
        this.backward = backward;
        this.left = left;
        this.right = right;
        this.brake = brake;
    }

    public String toString() {
        return "VehicleInputState{F=" + this.forward + ",B=" + this.backward + ",L=" + this.left + ",R=" + this.right + ",brake=" + this.brake + "}";
    }
}

