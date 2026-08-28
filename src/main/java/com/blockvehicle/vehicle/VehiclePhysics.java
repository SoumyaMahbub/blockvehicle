package com.blockvehicle.vehicle;

public final class VehiclePhysics {
    public static final float ACCELERATION = 0.016f;
    public static final float REVERSE_ACCELERATION = 0.008f;
    public static final float MAX_SPEED = 0.85f;
    public static final float MAX_REVERSE_SPEED = 0.35f;
    public static final float FRICTION = 0.992f;
    public static final float CORNERING_DRAG = 0.99f;
    public static final float BRAKE_BASE_DECELERATION = 0.045f;
    public static final float BRAKE_SPEED_FACTOR = 0.1f;
    public static final float TURN_SPEED = 6.5f;
    public static final float GRAVITY = 0.09f;
    public static final float MAX_FALL_SPEED = 3.8f;
    public static final float AIR_DRAG = 0.999f;
    public static final float AIR_ANGULAR_DAMPING = 0.90f;
    public static final float MAX_AIR_ANGULAR_VELOCITY = 0.85f;
    public static final float MINECRAFT_STEP_HEIGHT = 0.25f;
    public static final float MAX_CLIMB_HEIGHT = 1.25f;
    public static final float STEP_HEIGHT = 1.25f;
    public static final float STOP_THRESHOLD = 0.003f;
    public static final float SUSPENSION_SPRING_STIFFNESS = 0.42f;
    public static final float SUSPENSION_DAMPER = 0.55f;
    public static final float SUSPENSION_MAX_REACH = 1.05f;
    public static final float GROUND_SUPPORT_REACH = 0.32f;
    public static final float RAMP_LAUNCH_TRANSFER = 0.75f;
    public static final float RAMP_LAUNCH_SPEED_BONUS = 0.38f;
    public static final float MAX_RAMP_LAUNCH_SPEED = 0.95f;
    public static final float MIN_LANDING_IMPACT_SPEED = 0.34f;
    public static final float FULL_LANDING_IMPACT_SPEED = 1.65f;
    public static final float DRIFT_MIN_SPEED = 0.16f;
    public static final float DRIFT_STEERING_BOOST = 1.45f;
    public static final float DRIFT_LATERAL_RETENTION = 0.94f;
    public static final float DRIFT_FORWARD_RESPONSE = 0.2f;
    public static final float DRIFT_BRAKE_BASE_DECELERATION = 0.012f;
    public static final float DRIFT_BRAKE_SPEED_FACTOR = 0.018f;
    public static final float CLIMB_COUPLING = 0.85f;
    public static final float SLOPE_GRAVITY_FACTOR = 0.6f;
    public static final float MAX_TILT_DEGREES = 20.0f;
    public static final float TILT_LERP_SPEED = 0.18f;
    public static final float TILT_PROBE_DEPTH = 2.5f;
    public static final float MIN_RAMMING_SPEED = 0.04f;
    public static final float MIN_DAMAGE_SPEED = 0.1f;
    public static final float RAM_DAMAGE_MULTIPLIER = 24.0f;
    public static final float CAR_COLLISION_ELASTICITY = 0.35f;
    public static final float WALL_SLIDE_FRICTION = 0.28f;
    public static final float OFFROAD_COLLISION_SPEED_RETENTION = 0.58f;
    public static final float OFFROAD_ANGULAR_RETENTION = 0.82f;
    public static final float OFFROAD_STEERING_BOOST = 1.65f;
    public static final double MAX_PUSHOUT_PER_TICK = 0.4;
    public static final float STEERING_TORQUE = 1.85f;
    public static final float ANGULAR_DAMPING = 0.72f;
    public static final float BRAKE_ANGULAR_DAMPING = 0.58f;
    public static final float MAX_ANGULAR_VELOCITY = 7.5f;
    public static final float IMPACT_TORQUE_FACTOR = 16.0f;

    private VehiclePhysics() {
    }
}
