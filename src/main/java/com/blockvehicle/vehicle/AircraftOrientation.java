package com.blockvehicle.vehicle;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Quaternion helpers shared by physics, seats, collision probes and rendering. */
public final class AircraftOrientation {
    private AircraftOrientation() {
    }

    public static Quaternionf fromYaw(float yawDegrees) {
        return new Quaternionf().rotationY((float)Math.toRadians(-yawDegrees));
    }

    public static Quaternionf sanitize(Quaternionf orientation, float fallbackYaw) {
        if (orientation == null || !Float.isFinite(orientation.x) || !Float.isFinite(orientation.y)
            || !Float.isFinite(orientation.z) || !Float.isFinite(orientation.w)
            || orientation.lengthSquared() < 1.0E-8f) {
            return fromYaw(fallbackYaw);
        }
        return new Quaternionf(orientation).normalize();
    }

    public static Quaternionf relative(Quaternionf absolute, float initialYaw) {
        Quaternionf inverseInitial = fromYaw(initialYaw).conjugate();
        return new Quaternionf(absolute).mul(inverseInitial).normalize();
    }

    public static Vec3d transform(Quaternionf orientation, Vec3d vector) {
        Vector3f transformed = orientation.transform(new Vector3f((float)vector.x, (float)vector.y, (float)vector.z));
        return new Vec3d(transformed.x, transformed.y, transformed.z);
    }

    public static Vec3d transformAroundPivot(Quaternionf relative, Vec3d point, Vec3d pivot) {
        return pivot.add(transform(relative, point.subtract(pivot)));
    }

    public static Vec3d forward(Quaternionf absolute) {
        return transform(absolute, new Vec3d(0.0, 0.0, 1.0)).normalize();
    }

    public static Vec3d up(Quaternionf absolute) {
        return transform(absolute, new Vec3d(0.0, 1.0, 0.0)).normalize();
    }

    public static Vec3d right(Quaternionf absolute) {
        // Minecraft's yaw-zero forward is +Z, making aircraft-right local -X.
        return transform(absolute, new Vec3d(-1.0, 0.0, 0.0)).normalize();
    }

    public static float yawDegrees(Quaternionf absolute) {
        Vec3d forward = forward(absolute);
        return MathHelper.wrapDegrees((float)Math.toDegrees(Math.atan2(-forward.x, forward.z)));
    }

    public static float pitchDegrees(Quaternionf absolute) {
        Vec3d forward = forward(absolute);
        return (float)Math.toDegrees(Math.asin(MathHelper.clamp(forward.y, -1.0, 1.0)));
    }

    public static float rollDegrees(Quaternionf absolute) {
        Vec3d forward = forward(absolute);
        Vec3d right = right(absolute);
        Vec3d worldReferenceRight = new Vec3d(-forward.z, 0.0, forward.x);
        if (worldReferenceRight.lengthSquared() < 1.0E-8) {
            return 0.0f;
        }
        worldReferenceRight = worldReferenceRight.normalize();
        Vec3d referenceUp = worldReferenceRight.crossProduct(forward).normalize();
        return MathHelper.wrapDegrees((float)Math.toDegrees(Math.atan2(-right.dotProduct(referenceUp), right.dotProduct(worldReferenceRight))));
    }
}
