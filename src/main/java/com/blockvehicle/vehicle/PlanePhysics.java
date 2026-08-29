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
    public static final float MAX_AIRSPEED = 2.0f;
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
        float controlAuthority = MathHelper.clamp(0.26f + (float)controlForwardSpeed * 1.48f, 0.26f, 1.0f)
            * (1.0f - oldStall * 0.48f);

        float rollInput = bool(input.right) - bool(input.left);
        float pitchInput = bool(input.pitchUp) - bool(input.pitchDown);
        float yawInput = bool(input.yawRight) - bool(input.yawLeft);
        float stabilityAssist = input.stunt ? 0.12f : 1.0f;
        float pilotCommandGain = input.stunt ? 1.16f : 1.0f;
        boolean directPitchInput = input.pitchUp || input.pitchDown;
        if (input.pitchUp || input.pitchDown || input.left || input.right) {
            controlAuthority = Math.max(controlAuthority, input.stunt ? 0.72f : 0.40f);
        }
        float runwaySpeedRatio = (float)(controlForwardSpeed / Math.max(0.12f, definition.takeoffSpeed()));
        float groundRotationBlend = MathHelper.clamp((runwaySpeedRatio - 0.52f) / 0.34f, 0.0f, 1.0f);
        float rawRollInput = rollInput;
        if (grounded) {
            // At runway speed A/D transitions gradually from wheel steering to a
            // small rotation bank. This avoids switching from full taxi yaw to
            // full roll on the exact tick that the landing gear leaves the road.
            float groundRollBlend = state.flightState == PlaneFlightState.TAKEOFF
                ? groundRotationBlend * 0.38f : 0.0f;
            yawInput += rawRollInput * (1.0f - groundRollBlend);
            rollInput = rawRollInput * groundRollBlend;
        }
        // A/D is a pure roll command in the air. World-relative mouse guidance
        // must pause during that roll or it turns into an unintended local pitch
        // command as the aircraft passes through 90/180 degrees.
        boolean allowMouseGuidance = grounded || Math.abs(rawRollInput) < 0.01f;
        if (!directPitchInput && hasPilot && allowMouseGuidance && (!grounded || groundRotationBlend > 0.0f)) {
            float targetPitch = grounded
                ? MathHelper.clamp(-input.lookPitch, -6.0f, 24.0f)
                : MathHelper.clamp(-input.lookPitch, -62.0f, 62.0f);
            float pitchError = MathHelper.wrapDegrees(targetPitch - AircraftOrientation.pitchDegrees(orientation));
            float response = grounded ? groundRotationBlend : 1.0f;
            pitchInput = MathHelper.clamp(pitchError / (grounded ? 26.0f : 38.0f), -0.65f, 0.65f)
                * definition.controlAssist() * pilotCommandGain * response;
        }
        if (!grounded && Math.abs(yawInput) < 0.01f && hasPilot && allowMouseGuidance) {
            float yawError = MathHelper.wrapDegrees(input.lookYaw - AircraftOrientation.yawDegrees(orientation));
            yawInput = MathHelper.clamp(yawError / 75.0f, -0.38f, 0.38f)
                * definition.controlAssist() * pilotCommandGain;
        }

        float pitchRate = Float.isFinite(state.pitchRate) ? MathHelper.clamp(state.pitchRate, -5.4f, 5.4f) : 0.0f;
        float rollRate = Float.isFinite(state.rollRate) ? MathHelper.clamp(state.rollRate, -7.2f, 7.2f) : 0.0f;
        float yawRate = Float.isFinite(state.yawRate) ? MathHelper.clamp(state.yawRate, -3.0f, 3.0f) : 0.0f;
        float pitchTarget = pitchInput * (grounded ? 0.8f : input.stunt ? 5.20f : 4.10f) * controlAuthority;
        float airborneRollBlend = !grounded && nearGround && state.flightState == PlaneFlightState.TAKEOFF ? 0.68f : 1.0f;
        float rollTarget = rollInput * (input.stunt ? 7.00f : 5.60f) * controlAuthority * airborneRollBlend;
        float yawTarget = yawInput * (grounded ? 2.3f : 1.60f) * controlAuthority;

        // Banking naturally supplies a small coordinated yaw, but never rotates
        // the velocity vector directly.
        if (!grounded && Math.abs(rollInput) < 0.01f) {
            float rollAngle = AircraftOrientation.rollDegrees(orientation);
            rollTarget += MathHelper.clamp(-rollAngle * 0.012f, -0.75f, 0.75f)
                * definition.controlAssist() * stabilityAssist;
        }
        if (!grounded) {
            float rollAngle = AircraftOrientation.rollDegrees(orientation);
            // While A/D is actively rolling, yawing around the banked local-up
            // axis also pitches the nose toward the ground. Keep that coupling
            // small for a normal roll and remove it in stunt mode; once the key
            // is released, the held bank again supplies a coordinated turn.
            float coordinatedTurnAssist = Math.abs(rollInput) > 0.01f
                ? (input.stunt ? 0.0f : 0.28f) : (input.stunt ? 0.40f : 1.0f);
            yawTarget += MathHelper.clamp(rollRate * 0.18f, -0.82f, 0.82f)
                * definition.controlAssist() * coordinatedTurnAssist;
            yawTarget += MathHelper.clamp((float)Math.sin(Math.toRadians(rollAngle)) * 0.72f,
                -0.62f, 0.62f) * definition.controlAssist() * coordinatedTurnAssist;
            if (!input.pitchUp && !input.pitchDown) {
                pitchTarget += MathHelper.clamp(-definition.balanceOffset() * 0.035f, -0.30f, 0.30f)
                    * definition.controlAssist() * stabilityAssist;
            }
        }
        pitchRate += (pitchTarget - pitchRate) * 0.22f;
        rollRate += (rollTarget - rollRate) * 0.22f;
        yawRate += (yawTarget - yawRate) * (grounded ? 0.22f : 0.17f);
        pitchRate *= grounded ? 0.72f : 0.985f;
        rollRate *= grounded ? 0.68f : 0.982f;
        yawRate *= grounded ? 0.78f : 0.985f;
        pitchRate = MathHelper.clamp(pitchRate, -5.4f, 5.4f);
        rollRate = MathHelper.clamp(rollRate, -7.2f, 7.2f);
        yawRate = MathHelper.clamp(yawRate, -3.0f, 3.0f);

        orientation.rotateX((float)Math.toRadians(-pitchRate));
        orientation.rotateY((float)Math.toRadians(-yawRate));
        orientation.rotateZ((float)Math.toRadians(rollRate));
        orientation.normalize();

        Vec3d forward = AircraftOrientation.forward(orientation);
        boolean deliberateManeuver = Math.abs(rollInput) > 0.01f || Math.abs(pitchInput) > 0.04f
            || Math.abs(yawInput) > 0.04f;
        float maneuverStartSpeed = (float)velocity.length();
        if (!grounded && maneuverStartSpeed > 1.0E-4f) {
            // An arcade aircraft should curve its flight path with its nose rather
            // than rotate the model while momentum keeps travelling forever in an
            // unrelated direction. Limit the turn rate and retain the magnitude,
            // so inertia is still visible without a bank falsely erasing airspeed.
            float airflowAuthority = MathHelper.clamp(
                (maneuverStartSpeed / Math.max(0.12f, definition.takeoffSpeed()) - 0.32f) / 0.68f,
                0.0f, 1.0f);
            float pathTurnDegrees = (input.stunt ? 5.8f : 3.4f) * controlAuthority * airflowAuthority;
            if (!deliberateManeuver) pathTurnDegrees *= 0.34f;
            velocity = steerVelocityDirection(velocity, forward, Math.toRadians(pathTurnDegrees));
        }

        Vec3d right = AircraftOrientation.right(orientation);
        Vec3d up = AircraftOrientation.up(orientation);
        float currentAirspeed = (float)velocity.length();
        double forwardSpeed = velocity.dotProduct(forward);
        double lateralSpeed = velocity.dotProduct(right);
        double localVerticalSpeed = velocity.dotProduct(up);
        float angleOfAttack = (float)Math.toDegrees(Math.atan2(-localVerticalSpeed, Math.max(0.04, forwardSpeed)));
        float forwardAlignment = currentAirspeed > 1.0E-4f
            ? MathHelper.clamp((float)(forwardSpeed / currentAirspeed), -1.0f, 1.0f) : 1.0f;
        float effectiveAirflow = currentAirspeed * (0.58f + 0.42f * Math.max(0.0f, forwardAlignment));
        float speedStall = 1.0f - MathHelper.clamp(
            effectiveAirflow / Math.max(0.12f, definition.takeoffSpeed()), 0.0f, 1.0f);
        float angleStall = MathHelper.clamp((Math.abs(angleOfAttack) - STALL_ANGLE_DEGREES * 0.72f) / (STALL_ANGLE_DEGREES * 0.75f), 0.0f, 1.0f);
        float slipRatio = currentAirspeed > 1.0E-4f
            ? MathHelper.clamp((float)(Math.abs(lateralSpeed) / currentAirspeed), 0.0f, 1.0f) : 0.0f;
        float slipStall = MathHelper.clamp((slipRatio - 0.62f) / 0.34f, 0.0f, 1.0f);
        float reverseStall = MathHelper.clamp((-forwardAlignment - 0.12f) / 0.62f, 0.0f, 1.0f);
        if (deliberateManeuver) {
            float maneuverGrace = input.stunt ? 0.22f : 0.52f;
            angleStall *= input.stunt ? 0.18f : 0.52f;
            slipStall *= maneuverGrace;
            reverseStall *= maneuverGrace;
        }
        float stallTarget = Math.max(speedStall * (grounded ? 0.25f : 1.0f),
            Math.max(angleStall, Math.max(slipStall, reverseStall)));
        float stallRise = input.stunt && deliberateManeuver ? 0.038f : 0.072f;
        float stall = approach(oldStall, stallTarget, stallTarget > oldStall ? stallRise : 0.060f);

        float massThrust = MathHelper.clamp(1.0f / Math.max(0.75f, weightFactor), 0.55f, 1.25f);
        double thrust = BASE_THRUST * rpm * massThrust * definition.enginePower();
        velocity = velocity.add(forward.multiply(thrust));

        double usefulForwardSpeed = Math.max(Math.max(0.0, forwardSpeed), effectiveAirflow * 0.88f);
        float aoaLift = MathHelper.clamp(1.0f + angleOfAttack * 0.018f, 0.15f, 1.45f);
        double liftAcceleration = BASE_LIFT * usefulForwardSpeed * usefulForwardSpeed
            * definition.liftScale() * aoaLift * (1.0 - stall * 0.88f);
        if (grounded) {
            float rotationLift = MathHelper.clamp((AircraftOrientation.pitchDegrees(orientation) - 0.5f) / 5.5f, 0.0f, 1.0f);
            liftAcceleration *= MathHelper.lerp(rotationLift, 0.15f, 1.12f);
        }
        liftAcceleration = Math.min(liftAcceleration, 0.22);
        Vec3d liftDirection = up;
        if (velocity.lengthSquared() > 1.0E-6) {
            Vec3d flow = velocity.normalize();
            Vec3d projectedUp = up.subtract(flow.multiply(up.dotProduct(flow)));
            if (projectedUp.lengthSquared() > 1.0E-6) liftDirection = projectedUp.normalize();
        }
        Vec3d liftForce = liftDirection.multiply(liftAcceleration);
        boolean deliberateRoll = !grounded && Math.abs(rollInput) > 0.05f;
        if (deliberateRoll && liftForce.y < 0.0) {
            // During a commanded barrel roll, inverted lift should still cost
            // altitude, but must not combine at full strength with gravity and
            // turn one brief inverted frame into an unrecoverable dive.
            double downwardLiftRetention = input.stunt ? 0.05 : 0.24;
            liftForce = new Vec3d(liftForce.x, liftForce.y * downwardLiftRetention, liftForce.z);
        }
        velocity = velocity.add(liftForce);
        if (!grounded) {
            float healthyAirflow = MathHelper.clamp(
                (effectiveAirflow / Math.max(0.12f, definition.takeoffSpeed()) - 0.52f) / 0.48f,
                0.0f, 1.0f) * (1.0f - stall * 0.78f);
            float bankAmount = Math.abs((float)Math.sin(Math.toRadians(AircraftOrientation.rollDegrees(orientation))));
            float supportFraction = deliberateRoll ? (input.stunt ? 0.98f : 0.86f) : bankAmount * 0.66f;
            double upwardLift = Math.max(0.0, liftForce.y);
            double verticalShortfall = Math.max(0.0, GRAVITY - upwardLift);
            velocity = velocity.add(0.0, verticalShortfall * supportFraction * healthyAirflow, 0.0);
        }
        velocity = velocity.add(0.0, -GRAVITY, 0.0);

        // Air friction, turn/angle drag, and lateral slip damping. These preserve
        // inertia while making aggressive maneuvers consume energy like Elytra.
        double drag = (0.0023 + velocity.lengthSquared() * 0.0042 + Math.abs(angleOfAttack) * 0.00010)
            * definition.dragScale();
        if (input.stunt && deliberateManeuver) drag *= 0.64;
        if (input.brake) drag += 0.035;
        velocity = velocity.multiply(MathHelper.clamp(1.0 - drag, 0.90, 0.999));
        velocity = velocity.subtract(right.multiply(lateralSpeed * (grounded ? 0.30 : input.stunt ? 0.012 : 0.022)));
        if (!grounded) {
            double normalDamping = (input.stunt ? 0.006 : 0.012) * (1.0 - stall * 0.68);
            velocity = velocity.subtract(up.multiply(localVerticalSpeed * normalDamping));
        }

        if (!grounded && deliberateManeuver && !input.brake && throttle > 0.35f && oldStall < 0.58f
            && maneuverStartSpeed > definition.takeoffSpeed() * 0.62f) {
            // Full-throttle stunt input is an explicit arcade contract: retain
            // most kinetic energy while still allowing gravity, drag and climbs
            // to have a visible cost over time.
            float retention = input.stunt ? 0.996f : 0.986f;
            double minimumSpeed = maneuverStartSpeed * retention;
            double resultingSpeed = velocity.length();
            if (resultingSpeed > 1.0E-6 && resultingSpeed < minimumSpeed) {
                velocity = velocity.multiply(minimumSpeed / resultingSpeed);
            }
        }

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

    private static Vec3d steerVelocityDirection(Vec3d velocity, Vec3d targetDirection, double maxRadians) {
        double speed = velocity.length();
        if (speed < 1.0E-8 || maxRadians <= 1.0E-8 || targetDirection.lengthSquared() < 1.0E-8) return velocity;
        Vec3d current = velocity.multiply(1.0 / speed);
        Vec3d target = targetDirection.normalize();
        double dot = MathHelper.clamp(current.dotProduct(target), -1.0, 1.0);
        double angle = Math.acos(dot);
        if (angle <= maxRadians) return target.multiply(speed);
        Vec3d axis = current.crossProduct(target);
        if (axis.lengthSquared() < 1.0E-10) {
            axis = current.crossProduct(new Vec3d(0.0, 1.0, 0.0));
            if (axis.lengthSquared() < 1.0E-10) axis = current.crossProduct(new Vec3d(1.0, 0.0, 0.0));
        }
        axis = axis.normalize();
        double cos = Math.cos(maxRadians);
        double sin = Math.sin(maxRadians);
        Vec3d rotated = current.multiply(cos)
            .add(axis.crossProduct(current).multiply(sin))
            .add(axis.multiply(axis.dotProduct(current) * (1.0 - cos)));
        return rotated.normalize().multiply(speed);
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
