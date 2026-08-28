package com.blockvehicle.vehicle;

public final class VehicleInputState {
    public final boolean forward;
    public final boolean backward;
    public final boolean left;
    public final boolean right;
    public final boolean brake;
    public final boolean pitchUp;
    public final boolean pitchDown;
    public final boolean yawLeft;
    public final boolean yawRight;
    public final boolean stunt;
    public final float lookYaw;
    public final float lookPitch;
    public static final VehicleInputState EMPTY = new VehicleInputState(false, false, false, false, false);

    public VehicleInputState(boolean forward, boolean backward, boolean left, boolean right, boolean brake) {
        this(forward, backward, left, right, brake, false, false, false, false, false, 0.0f, 0.0f);
    }

    public VehicleInputState(boolean forward, boolean backward, boolean left, boolean right, boolean brake,
                             boolean pitchUp, boolean pitchDown, boolean yawLeft, boolean yawRight,
                             float lookYaw, float lookPitch) {
        this(forward, backward, left, right, brake, pitchUp, pitchDown, yawLeft, yawRight, false, lookYaw, lookPitch);
    }

    public VehicleInputState(boolean forward, boolean backward, boolean left, boolean right, boolean brake,
                             boolean pitchUp, boolean pitchDown, boolean yawLeft, boolean yawRight, boolean stunt,
                             float lookYaw, float lookPitch) {
        this.forward = forward;
        this.backward = backward;
        this.left = left;
        this.right = right;
        this.brake = brake;
        this.pitchUp = pitchUp;
        this.pitchDown = pitchDown;
        this.yawLeft = yawLeft;
        this.yawRight = yawRight;
        this.stunt = stunt;
        this.lookYaw = lookYaw;
        this.lookPitch = lookPitch;
    }

    public boolean sameControls(VehicleInputState other) {
        return other != null
            && this.forward == other.forward
            && this.backward == other.backward
            && this.left == other.left
            && this.right == other.right
            && this.brake == other.brake
            && this.pitchUp == other.pitchUp
            && this.pitchDown == other.pitchDown
            && this.yawLeft == other.yawLeft
            && this.yawRight == other.yawRight
            && this.stunt == other.stunt
            && Math.abs(this.lookYaw - other.lookYaw) < 0.5f
            && Math.abs(this.lookPitch - other.lookPitch) < 0.5f;
    }

    public boolean hasAnyInput() {
        return this.forward || this.backward || this.left || this.right || this.brake
            || this.pitchUp || this.pitchDown || this.yawLeft || this.yawRight || this.stunt;
    }

    public String toString() {
        return "VehicleInputState{F=" + this.forward + ",B=" + this.backward + ",L=" + this.left + ",R=" + this.right + ",brake=" + this.brake + "}";
    }
}
