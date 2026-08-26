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

    public boolean sameControls(VehicleInputState other) {
        return other != null
            && this.forward == other.forward
            && this.backward == other.backward
            && this.left == other.left
            && this.right == other.right
            && this.brake == other.brake;
    }

    public boolean hasAnyInput() {
        return this.forward || this.backward || this.left || this.right || this.brake;
    }

    public String toString() {
        return "VehicleInputState{F=" + this.forward + ",B=" + this.backward + ",L=" + this.left + ",R=" + this.right + ",brake=" + this.brake + "}";
    }
}
