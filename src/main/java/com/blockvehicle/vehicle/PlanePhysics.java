package com.blockvehicle.vehicle;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;

/**
 * Fixed-tick arcade flight model. It models energy and control authority without
 * trying to reproduce a full aerodynamic simulator in Minecraft units.
 */
public final class PlanePhysics {
    public static final float GRAVITY = 0.08f;
    public static final float THROTTLE_STEP = 0.025f;
    public static final float RPM_RESPONSE_UP = 0.065f;
    public static final float RPM_RESPONSE_DOWN = 0.035f;
    public static final float BASE_THRUST = 0.031f;
    public static final float BASE_LIFT = 0.42f;
    public static final float STALL_ANGLE_DEGREES = 28.0f;
    public static final float MAX_AIRSPEED = 1.8f;
    public static final float GEAR_REACH = 0.62f;
    public static final int MAX_COLLISION_SUBSTEPS = 6;

    private PlanePhysics() {
    }

    public static Result step(State state, VehicleInputState input, PlaneDefinition definition,
                              boolean grounded, boolean nearGround, boolean hasPilot, float weightFactor) {
        Quaternionf orientation = AircraftOrientation.sanitize(state.orientation, state.yawDegrees);
        Vec3d velocity = finite(state.velocity) ? state.velocity : Vec3d.ZERO;
        float throttle = Float.isFinite(state.throttle) ? MathHelper.clamp(state.throttle, 0.0f, 1.0f) : 0.0f;
        if (hasPilot) {
            if (input.forward && !input.backward) throttle += THROTTLE_STEP;
            if (input.backward && !input.forward) throttle -= THROTTLE_STEP;
        } else {
            throttle -= THROTTLE_STEP * 0.25f;
        }
        throttle = MathHelper.clamp(throttle, 0.0f, 1.0f);
        float rpmTarget = definition.hasEngines() ? (hasPilot ? 0.12f + throttle * 0.88f : throttle) : 0.0f;
        float rpmResponse = rpmTarget > state.engineRpm ? RPM_RESPONSE_UP : RPM_RESPONSE_DOWN;
        float safeRpm = Float.isFinite(state.engineRpm) ? MathHelper.clamp(state.engineRpm, 0.0f, 1.0f) : 0.0f;
        float rpm = approach(safeRpm, rpmTarget, rpmResponse);

        float airspeed = (float)velocity.length();
        double controlForwardSpeed = Math.max(0.0, velocity.dotProduct(AircraftOrientation.forward(orientation)));
        float oldStall = Float.isFinite(state.stallAmount) ? MathHelper.clamp(state.stallAmount, 0.0f, 1.0f) : 0.0f;
        float controlAuthority = MathHelper.clamp(0.18f + (float)controlForwardSpeed * 1.55f, 0.18f, 1.0f)
            * (1.0f - oldStall * 0.48f);

        float rollInput = bool(input.right) - bool(input.left);
        float pitchInput = bool(input.pitchUp) - bool(input.pitchDown);
        float yawInput = bool(input.yawRight) - bool(input.yawLeft);
        float assistance = input.stunt ? 0.15f : 1.0f;
        if (grounded) {
            yawInput += rollInput;
            rollInput = 0.0f;
        } else if (Math.abs(pitchInput) < 0.01f && hasPilot) {
            float targetPitch = MathHelper.clamp(-input.lookPitch, -62.0f, 62.0f);
            float pitchError = MathHelper.wrapDegrees(targetPitch - AircraftOrientation.pitchDegrees(orientation));
            pitchInput = MathHelper.clamp(pitchError / 38.0f, -0.65f, 0.65f) * definition.controlAssist() * assistance;
        }
        if (!grounded && Math.abs(yawInput) < 0.01f && hasPilot) {
            float yawError = MathHelper.wrapDegrees(input.lookYaw - AircraftOrientation.yawDegrees(orientation));
            yawInput = MathHelper.clamp(yawError / 75.0f, -0.38f, 0.38f) * definition.controlAssist() * assistance;
        }

        float pitchRate = Float.isFinite(state.pitchRate) ? MathHelper.clamp(state.pitchRate, -3.3f, 3.3f) : 0.0f;
        float rollRate = Float.isFinite(state.rollRate) ? MathHelper.clamp(state.rollRate, -4.8f, 4.8f) : 0.0f;
        float yawRate = Float.isFinite(state.yawRate) ? MathHelper.clamp(state.yawRate, -2.8f, 2.8f) : 0.0f;
        float pitchTarget = pitchInput * (grounded ? 0.7f : 2.8f) * controlAuthority;
        float rollTarget = rollInput * 4.2f * controlAuthority;
        float yawTarget = yawInput * (grounded ? 2.3f : 1.35f) * controlAuthority;

        // Banking naturally supplies a small coordinated yaw, but never rotates
        // the velocity vector directly.
        if (!grounded && Math.abs(rollInput) < 0.01f) {
            float rollAngle = AircraftOrientation.rollDegrees(orientation);
            rollTarget += MathHelper.clamp(-rollAngle * 0.012f, -0.75f, 0.75f) * definition.controlAssist() * assistance;
        }
        if (!grounded) {
            yawTarget += MathHelper.clamp(rollRate * 0.10f, -0.42f, 0.42f) * definition.controlAssist() * assistance;
            if (!input.pitchUp && !input.pitchDown) {
                pitchTarget += MathHelper.clamp(-definition.balanceOffset() * 0.035f, -0.30f, 0.30f)
                    * definition.controlAssist() * assistance;
            }
        }
        pitchRate += (pitchTarget - pitchRate) * 0.18f;
        rollRate += (rollTarget - rollRate) * 0.20f;
        yawRate += (yawTarget - yawRate) * (grounded ? 0.22f : 0.14f);
        pitchRate *= grounded ? 0.72f : 0.985f;
        rollRate *= grounded ? 0.68f : 0.982f;
        yawRate *= grounded ? 0.78f : 0.985f;
        pitchRate = MathHelper.clamp(pitchRate, -3.3f, 3.3f);
        rollRate = MathHelper.clamp(rollRate, -4.8f, 4.8f);
        yawRate = MathHelper.clamp(yawRate, -2.8f, 2.8f);

        orientation.rotateX((float)Math.toRadians(-pitchRate));
        orientation.rotateY((float)Math.toRadians(-yawRate));
        orientation.rotateZ((float)Math.toRadians(rollRate));
        orientation.normalize();

        Vec3d forward = AircraftOrientation.forward(orientation);
        Vec3d right = AircraftOrientation.right(orientation);
        Vec3d up = AircraftOrientation.up(orientation);
        double forwardSpeed = velocity.dotProduct(forward);
        double lateralSpeed = velocity.dotProduct(right);
        double localVerticalSpeed = velocity.dotProduct(up);
        float angleOfAttack = (float)Math.toDegrees(Math.atan2(-localVerticalSpeed, Math.max(0.04, forwardSpeed)));
        float speedStall = 1.0f - MathHelper.clamp((float)(Math.max(0.0, forwardSpeed) / Math.max(0.12f, definition.takeoffSpeed())), 0.0f, 1.0f);
        float angleStall = MathHelper.clamp((Math.abs(angleOfAttack) - STALL_ANGLE_DEGREES * 0.72f) / (STALL_ANGLE_DEGREES * 0.75f), 0.0f, 1.0f);
        float stallTarget = Math.max(speedStall * (grounded ? 0.25f : 1.0f), angleStall);
        float stall = approach(oldStall, stallTarget, stallTarget > oldStall ? 0.09f : 0.055f);

        float massThrust = MathHelper.clamp(1.0f / Math.max(0.75f, weightFactor), 0.55f, 1.25f);
        double thrust = BASE_THRUST * rpm * massThrust * definition.enginePower();
        velocity = velocity.add(forward.multiply(thrust));

        double usefulForwardSpeed = Math.max(0.0, forwardSpeed);
        float aoaLift = MathHelper.clamp(1.0f + angleOfAttack * 0.018f, 0.15f, 1.45f);
        double liftAcceleration = BASE_LIFT * usefulForwardSpeed * usefulForwardSpeed
            * definition.liftScale() * aoaLift * (1.0 - stall * 0.88f);
        if (grounded) {
            float rotationLift = MathHelper.clamp((AircraftOrientation.pitchDegrees(orientation) - 1.0f) / 8.0f, 0.0f, 1.0f);
            liftAcceleration *= rotationLift;
        }
        liftAcceleration = Math.min(liftAcceleration, 0.22);
        Vec3d liftDirection = up;
        if (velocity.lengthSquared() > 1.0E-6) {
            Vec3d flow = velocity.normalize();
            Vec3d projectedUp = up.subtract(flow.multiply(up.dotProduct(flow)));
            if (projectedUp.lengthSquared() > 1.0E-6) liftDirection = projectedUp.normalize();
        }
        velocity = velocity.add(liftDirection.multiply(liftAcceleration));
        velocity = velocity.add(0.0, -GRAVITY, 0.0);

        // Air friction, turn/angle drag, and lateral slip damping. These preserve
        // inertia while making aggressive maneuvers consume energy like Elytra.
        double drag = (0.0025 + velocity.lengthSquared() * 0.0045 + Math.abs(angleOfAttack) * 0.00012)
            * definition.dragScale();
        if (input.brake) drag += 0.035;
        velocity = velocity.multiply(MathHelper.clamp(1.0 - drag, 0.90, 0.999));
        velocity = velocity.subtract(right.multiply(lateralSpeed * (grounded ? 0.30 : 0.035)));

        if (stall > 0.35f && !grounded) {
            // Smooth nose-drop tendency makes recovery intuitive: lower the nose,
            // regain speed, then pull out.
            pitchRate -= stall * 0.16f;
        }

        if (grounded) {
            double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
            double groundFriction = input.brake ? 0.72 : 0.965;
            velocity = new Vec3d(velocity.x * groundFriction, Math.max(-0.08, velocity.y), velocity.z * groundFriction);
            if (horizontalSpeed < 0.015 && throttle < 0.02f) velocity = new Vec3d(0.0, velocity.y, 0.0);
        }

        double maxSpeed = MAX_AIRSPEED * MathHelper.clamp(1.1f / Math.max(0.8f, weightFactor), 0.72f, 1.15f);
        if (velocity.lengthSquared() > maxSpeed * maxSpeed) velocity = velocity.normalize().multiply(maxSpeed);

        if (!finite(velocity)) velocity = new Vec3d(0.0, -GRAVITY, 0.0);
        float finalAirspeed = (float)velocity.length();
        PlaneFlightState flightState = chooseState(state.flightState, grounded, finalAirspeed,
            nearGround, definition.takeoffSpeed(), stall, velocity.y, throttle);
        return new Result(orientation, velocity, throttle, rpm, pitchRate, rollRate, yawRate,
            stall, angleOfAttack, finalAirspeed, flightState);
    }

    private static PlaneFlightState chooseState(PlaneFlightState previous, boolean grounded, float airspeed, boolean nearGround,
                                                float takeoffSpeed, float stall, double verticalSpeed, float throttle) {
        if (grounded) {
            if (previous == PlaneFlightState.HARD_LANDING) return PlaneFlightState.TAXIING;
            if (airspeed < 0.035f && throttle < 0.04f) return PlaneFlightState.PARKED;
            if (airspeed > takeoffSpeed * 0.78f) return PlaneFlightState.TAKEOFF;
            return PlaneFlightState.TAXIING;
        }
        if (stall > 0.58f) return PlaneFlightState.STALLING;
        if (nearGround && verticalSpeed < -0.08 && previous != PlaneFlightState.TAKEOFF) return PlaneFlightState.LANDING;
        return PlaneFlightState.AIRBORNE;
    }

    private static float bool(boolean value) {
        return value ? 1.0f : 0.0f;
    }

    private static float approach(float value, float target, float step) {
        if (value < target) return Math.min(value + step, target);
        return Math.max(value - step, target);
    }

    private static boolean finite(Vec3d vector) {
        return Double.isFinite(vector.x) && Double.isFinite(vector.y) && Double.isFinite(vector.z);
    }

    public record State(Quaternionf orientation, Vec3d velocity, float throttle, float engineRpm,
                        float pitchRate, float rollRate, float yawRate, float stallAmount,
                        PlaneFlightState flightState, float yawDegrees) {
    }

    public record Result(Quaternionf orientation, Vec3d velocity, float throttle, float engineRpm,
                         float pitchRate, float rollRate, float yawRate, float stallAmount,
                         float angleOfAttack, float airspeed, PlaneFlightState flightState) {
    }
}
