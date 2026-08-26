package com.blockvehicle.vehicle;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Vec3d;

public class SeatData {
    public final double rx;
    public final double ry;
    public final double rz;
    public final boolean isDriver;
    public final float yawOffset;

    public SeatData(double rx, double ry, double rz, boolean isDriver) {
        this(rx, ry, rz, isDriver, 0.0f);
    }

    public SeatData(double rx, double ry, double rz, boolean isDriver, float yawOffset) {
        this.rx = rx;
        this.ry = ry;
        this.rz = rz;
        this.isDriver = isDriver;
        this.yawOffset = yawOffset;
    }

    public Vec3d toWorldPos(Vec3d vehiclePos, float yawRad) {
        return this.toWorldPos(vehiclePos, yawRad, 0.0f, 0.0f);
    }

    public Vec3d toWorldPos(Vec3d vehiclePos, float yawRad, float pitchRad, float rollRad) {
        double cosP = Math.cos(pitchRad);
        double sinP = Math.sin(pitchRad);
        double ry1 = this.ry * cosP + this.rz * sinP;
        double rz1 = -this.ry * sinP + this.rz * cosP;
        double rx1 = this.rx;
        double cosR = Math.cos(rollRad);
        double sinR = Math.sin(rollRad);
        double rx2 = rx1 * cosR - ry1 * sinR;
        double ry2 = rx1 * sinR + ry1 * cosR;
        double rz2 = rz1;
        double cosY = Math.cos(yawRad);
        double sinY = Math.sin(yawRad);
        double wx = vehiclePos.x + (rx2 * cosY - rz2 * sinY);
        double wy = vehiclePos.y + ry2;
        double wz = vehiclePos.z + (rx2 * sinY + rz2 * cosY);
        return new Vec3d(wx, wy, wz);
    }

    public NbtCompound toNbt() {
        NbtCompound tag = new NbtCompound();
        tag.putDouble("rx", this.rx);
        tag.putDouble("ry", this.ry);
        tag.putDouble("rz", this.rz);
        tag.putBoolean("driver", this.isDriver);
        tag.putFloat("yawOffset", this.yawOffset);
        return tag;
    }

    public static SeatData fromNbt(NbtCompound tag) {
        return new SeatData(tag.getDouble("rx"), tag.getDouble("ry"), tag.getDouble("rz"), tag.getBoolean("driver"), tag.getFloat("yawOffset"));
    }
}

