package com.blockvehicle.vehicle;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;

/**
 * Fixed-tick, server-authoritative arcade rotorcraft model. The main rotor's
 * thrust follows aircraft-up, so cyclic tilt trades vertical lift for travel.
 */
public final class HelicopterPhysics {
    public static final float GRAVITY = 0.08f;
    public static final float GEAR_REACH = 0.68f;

    private HelicopterPhysics() {}

    public static Result step(State state, VehicleInputState input, PlaneDefinition definition,
                              boolean grounded, boolean nearGround, boolean hasPilot, float weightFactor) {
        Quaternionf orientation = AircraftOrientation.sanitize(state.orientation, state.yawDegrees);
        Vec3d velocity = finite(state.velocity) ? state.velocity : Vec3d.ZERO;
        float collective = finite(state.collective) ? MathHelper.clamp(state.collective, 0.0f, 1.0f) : 0.0f;
        float safeRpm = finite(state.rotorRpm) ? MathHelper.clamp(state.rotorRpm, 0.0f, 1.08f) : 0.0f;

        // Space is collective-up, Left Ctrl/down-arrow is collective-down. With
        // neither held, the neutral setting can hover only while nearly level.
        float collectiveTarget = !hasPilot ? 0.10f : input.brake && !input.pitchDown ? 0.94f
            : input.pitchDown && !input.brake ? 0.13f : 0.555f;
        collective = approach(collective, collectiveTarget, input.brake || input.pitchDown ? 0.040f : 0.025f);
        float autorotation = !hasPilot && velocity.y < -0.10
            ? MathHelper.clamp((float)(-velocity.y - 0.10) * 1.6f, 0.0f, 0.42f) : 0.0f;
        float rpmTarget = definition.hasEngines() ? (hasPilot ? 1.0f : autorotation) : 0.0f;
        float rotorRpm = approach(safeRpm, rpmTarget, rpmTarget > safeRpm ? 0.028f : 0.016f);

        float pitchInput = bool(input.backward) - bool(input.forward);
        float rollInput = bool(input.right) - bool(input.left);
        float yawInput = bool(input.yawRight) - bool(input.yawLeft);
        float pitch = AircraftOrientation.pitchDegrees(orientation);
        float roll = AircraftOrientation.rollDegrees(orientation);
        boolean precision = input.stunt;
        float response = precision ? 0.68f : 1.0f;
        float maxTiltRate = grounded ? 0.75f : 3.25f * response;
        float pitchTarget = pitchInput * maxTiltRate;
        float rollTarget = rollInput * (grounded ? 0.65f : 3.55f * response);

        // A little rate damping makes a keyboard-controlled helicopter readable,
        // but it deliberately does not auto-hover or erase a pilot's bank.
        if (Math.abs(pitchInput) < 0.01f) pitchTarget += MathHelper.clamp(-pitch * 0.045f, -0.82f, 0.82f);
        if (Math.abs(rollInput) < 0.01f) rollTarget += MathHelper.clamp(-roll * 0.050f, -0.92f, 0.92f);
        float tailAuthority = definition.takeoffSpeed(); // geometry-derived: tail rotor or torque-only yaw
        float yawTarget = yawInput * (1.45f + tailAuthority * 1.55f) * response;
        if (!hasPilot) yawTarget = 0.0f;
        if (tailAuthority < 0.60f && rotorRpm > 0.45f) {
            // A rotorcraft without a marked tail rotor remains flyable, but main
            // rotor torque has to be actively corrected by the pilot.
            yawTarget += (definition.propellers().get(0).clockwise() ? -1.0f : 1.0f)
                * collective * rotorRpm * 0.34f;
        }

        float pitchRate = clampFinite(state.pitchRate, -4.2f, 4.2f);
        float rollRate = clampFinite(state.rollRate, -4.6f, 4.6f);
        float yawRate = clampFinite(state.yawRate, -3.2f, 3.2f);
        pitchRate += (pitchTarget - pitchRate) * 0.20f;
        rollRate += (rollTarget - rollRate) * 0.20f;
        yawRate += (yawTarget - yawRate) * 0.18f;
        pitchRate *= grounded ? 0.70f : 0.965f;
        rollRate *= grounded ? 0.70f : 0.965f;
        yawRate *= grounded ? 0.76f : 0.972f;
        orientation.rotateX((float)Math.toRadians(-pitchRate));
        orientation.rotateY((float)Math.toRadians(-yawRate));
        orientation.rotateZ((float)Math.toRadians(rollRate));
        orientation.normalize();

        Vec3d up = AircraftOrientation.up(orientation);
        Vec3d forward = AircraftOrientation.forward(orientation);
        Vec3d right = AircraftOrientation.right(orientation);
        double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        float translationalLift = MathHelper.clamp((float)(horizontalSpeed - 0.12) / 0.65f, 0.0f, 1.0f) * 0.12f;
        float groundEffect = nearGround && !grounded ? MathHelper.clamp(1.0f - (float)Math.abs(velocity.y) * 2.5f, 0.0f, 1.0f) * 0.11f : 0.0f;
        float powerToWeight = MathHelper.clamp(definition.liftScale() * (float)Math.sqrt(definition.enginePower())
            / Math.max(0.76f, weightFactor * 0.82f), 0.78f, 1.28f);
        float liftAcceleration = 0.145f * collective * rotorRpm * rotorRpm * powerToWeight
            * (1.0f + translationalLift + groundEffect);

        // Settling with power/vortex-ring state: a steep, almost vertical descent
        // through the rotor's own wake makes adding collective less effective.
        float vortexTarget = !grounded && velocity.y < -0.16 && horizontalSpeed < 0.30 && collective > 0.62f
            ? MathHelper.clamp((float)((-velocity.y - 0.16) / 0.30) * (float)((0.30 - horizontalSpeed) / 0.30), 0.0f, 1.0f)
            : 0.0f;
        float vortex = approach(MathHelper.clamp(state.vortexAmount, 0.0f, 1.0f), vortexTarget,
            vortexTarget > state.vortexAmount ? 0.055f : 0.085f);
        liftAcceleration *= 1.0f - vortex * 0.58f;
        velocity = velocity.add(up.multiply(liftAcceleration)).add(0.0, -GRAVITY, 0.0);

        // Fuselage drag and rotor damping. Local side-slip is damped more than
        // forward travel, giving GTA-like bank-and-go motion without ice skating.
        double forwardSpeed = velocity.dotProduct(forward);
        double sideSpeed = velocity.dotProduct(right);
        velocity = velocity.subtract(right.multiply(sideSpeed * 0.052));
        velocity = velocity.subtract(forward.multiply(forwardSpeed * 0.009));
        velocity = new Vec3d(velocity.x * 0.994, velocity.y * 0.997, velocity.z * 0.994);
        if (vortex > 0.2f) {
            pitchRate += (float)Math.sin(state.age * 0.73f) * vortex * 0.16f;
            rollRate += (float)Math.cos(state.age * 0.61f) * vortex * 0.18f;
        }

        if (grounded) {
            velocity = new Vec3d(velocity.x * 0.76, Math.max(0.0, velocity.y), velocity.z * 0.76);
            if (liftAcceleration < GRAVITY * 1.03f) velocity = new Vec3d(velocity.x, 0.0, velocity.z);
        }
        double maxSpeed = 1.38 * MathHelper.clamp(1.0f / Math.max(0.78f, weightFactor), 0.72f, 1.12f);
        if (velocity.lengthSquared() > maxSpeed * maxSpeed) velocity = velocity.normalize().multiply(maxSpeed);
        if (!finite(velocity)) velocity = new Vec3d(0.0, -GRAVITY, 0.0);

        float speed = (float)velocity.length();
        PlaneFlightState flightState = grounded
            ? rotorRpm < 0.22f ? PlaneFlightState.PARKED : PlaneFlightState.TAXIING
            : vortex > 0.52f ? PlaneFlightState.STALLING
            : velocity.y < -0.075 && nearGround ? PlaneFlightState.LANDING : PlaneFlightState.AIRBORNE;
        return new Result(orientation, velocity, collective, rotorRpm, pitchRate, rollRate, yawRate,
            vortex, (float)Math.toDegrees(Math.acos(MathHelper.clamp(up.y, -1.0, 1.0))), speed, flightState);
    }

    private static float bool(boolean value) { return value ? 1.0f : 0.0f; }
    private static float approach(float value, float target, float amount) {
        return value < target ? Math.min(value + amount, target) : Math.max(value - amount, target);
    }
    private static float clampFinite(float value, float min, float max) {
        return Float.isFinite(value) ? MathHelper.clamp(value, min, max) : 0.0f;
    }
    private static boolean finite(float value) { return Float.isFinite(value); }
    private static boolean finite(Vec3d v) {
        return Double.isFinite(v.x) && Double.isFinite(v.y) && Double.isFinite(v.z);
    }

    public record State(Quaternionf orientation, Vec3d velocity, float collective, float rotorRpm,
                        float pitchRate, float rollRate, float yawRate, float vortexAmount,
                        PlaneFlightState flightState, float yawDegrees, int age) {}

    public record Result(Quaternionf orientation, Vec3d velocity, float collective, float rotorRpm,
                         float pitchRate, float rollRate, float yawRate, float vortexAmount,
                         float diskTilt, float speed, PlaneFlightState flightState) {}
}
