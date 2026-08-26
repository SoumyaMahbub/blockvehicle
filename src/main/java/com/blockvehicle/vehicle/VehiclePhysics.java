package com.blockvehicle.vehicle;

public final class VehiclePhysics {
    public static final float ACCELERATION = 0.016f;
    public static final float REVERSE_ACCELERATION = 0.008f;
    public static final float MAX_SPEED = 0.85f;
    public static final float MAX_REVERSE_SPEED = 0.35f;
    public static final float FRICTION = 0.975f;
    public static final float CORNERING_DRAG = 0.99f;
    public static final float BRAKE_FRICTION = 0.86f;
    public static final float TURN_SPEED = 6.5f;
    public static final float GRAVITY = 0.08f;
    public static final float MAX_FALL_SPEED = 3.92f;
    public static final float MINECRAFT_STEP_HEIGHT = 0.25f;
    public static final float MAX_CLIMB_HEIGHT = 1.25f;
    public static final float STEP_HEIGHT = 1.25f;
    public static final float STOP_THRESHOLD = 0.003f;
    public static final float SUSPENSION_SPRING_STIFFNESS = 0.42f;
    public static final float SUSPENSION_DAMPER = 0.55f;
    public static final float SUSPENSION_MAX_REACH = 1.25f;
    public static final float CLIMB_COUPLING = 0.85f;
    public static final float SLOPE_GRAVITY_FACTOR = 0.6f;
    public static final float MAX_TILT_DEGREES = 20.0f;
    public static final float TILT_LERP_SPEED = 0.18f;
    public static final float TILT_PROBE_DEPTH = 2.5f;
    public static final float MIN_RAMMING_SPEED = 0.04f;
    public static final float MIN_DAMAGE_SPEED = 0.1f;
    public static final float RAM_DAMAGE_MULTIPLIER = 24.0f;
    public static final float CAR_COLLISION_ELASTICITY = 0.35f;
    public static final float WALL_SLIDE_FRICTION = 0.94f;
    public static final double MAX_PUSHOUT_PER_TICK = 0.4;
    public static final float STEERING_TORQUE = 2.4f;
    public static final float ANGULAR_DAMPING = 0.74f;
    public static final float DRIFT_ANGULAR_DAMPING = 0.92f;
    public static final float MAX_ANGULAR_VELOCITY = 10.5f;
    public static final float IMPACT_TORQUE_FACTOR = 16.0f;

    private VehiclePhysics() {
    }
}

