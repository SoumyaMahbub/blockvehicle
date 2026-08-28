package com.blockvehicle.vehicle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/** Immutable, activation-time aircraft geometry. No world scanning is needed in flight. */
public final class PlaneDefinition {
    private final Point nose;
    private final Point leftWingTip;
    private final Point rightWingTip;
    private final Vec3d centerOfMass;
    private final Vec3d centerOfLift;
    private final List<PropellerAssembly> propellers;
    private final List<Point> priorityCollisionPoints;
    private final Map<Long, PropellerAssembly> bladeLookup;
    private final float wingSpan;
    private final float wingArea;
    private final float liftScale;
    private final float takeoffSpeed;
    private final float enginePower;
    private final float dragScale;
    private final float controlAssist;
    private final float balanceOffset;
    private final float asymmetry;

    public PlaneDefinition(Point nose, Point leftWingTip, Point rightWingTip, Vec3d centerOfMass,
                           Vec3d centerOfLift, List<PropellerAssembly> propellers, List<Point> priorityCollisionPoints,
                           float wingSpan, float wingArea,
                           float liftScale, float takeoffSpeed, float enginePower, float dragScale,
                           float controlAssist, float balanceOffset, float asymmetry) {
        this.nose = nose;
        this.leftWingTip = leftWingTip;
        this.rightWingTip = rightWingTip;
        this.centerOfMass = centerOfMass != null ? centerOfMass : Vec3d.ZERO;
        this.centerOfLift = centerOfLift != null ? centerOfLift : this.centerOfMass;
        this.propellers = Collections.unmodifiableList(new ArrayList<>(propellers != null ? propellers : List.of()));
        this.priorityCollisionPoints = Collections.unmodifiableList(new ArrayList<>(
            priorityCollisionPoints != null ? priorityCollisionPoints : List.of()));
        HashMap<Long, PropellerAssembly> lookup = new HashMap<>();
        for (PropellerAssembly propeller : this.propellers) {
            for (Point blade : propeller.blades()) lookup.put(coordinateKey(blade.rx(), blade.ry(), blade.rz()), propeller);
        }
        this.bladeLookup = Collections.unmodifiableMap(lookup);
        this.wingSpan = Math.max(1.0f, wingSpan);
        this.wingArea = Math.max(1.0f, wingArea);
        this.liftScale = MathHelper.clamp(liftScale, 0.55f, 1.8f);
        this.takeoffSpeed = MathHelper.clamp(takeoffSpeed, 0.28f, 0.8f);
        this.enginePower = MathHelper.clamp(enginePower, 0.0f, 2.5f);
        this.dragScale = MathHelper.clamp(dragScale, 0.5f, 2.0f);
        this.controlAssist = MathHelper.clamp(controlAssist, 0.0f, 1.5f);
        this.balanceOffset = MathHelper.clamp(balanceOffset, -8.0f, 8.0f);
        this.asymmetry = MathHelper.clamp(asymmetry, 0.0f, 1.0f);
    }

    public Point nose() { return this.nose; }
    public Point leftWingTip() { return this.leftWingTip; }
    public Point rightWingTip() { return this.rightWingTip; }
    public Vec3d centerOfMass() { return this.centerOfMass; }
    public Vec3d centerOfLift() { return this.centerOfLift; }
    public List<PropellerAssembly> propellers() { return this.propellers; }
    public List<Point> priorityCollisionPoints() { return this.priorityCollisionPoints; }
    public float wingSpan() { return this.wingSpan; }
    public float wingArea() { return this.wingArea; }
    public float liftScale() { return this.liftScale; }
    public float takeoffSpeed() { return this.takeoffSpeed; }
    public float enginePower() { return this.enginePower; }
    public float dragScale() { return this.dragScale; }
    public float controlAssist() { return this.controlAssist; }
    public float balanceOffset() { return this.balanceOffset; }
    public float asymmetry() { return this.asymmetry; }

    public boolean hasEngines() {
        return this.enginePower > 0.0f && !this.bladeLookup.isEmpty();
    }

    public boolean isPropellerBlade(double rx, double ry, double rz) {
        return this.bladeLookup.containsKey(coordinateKey(rx, ry, rz));
    }

    public PropellerAssembly getPropellerForBlade(double rx, double ry, double rz) {
        return this.bladeLookup.get(coordinateKey(rx, ry, rz));
    }

    public NbtCompound toNbt() {
        NbtCompound tag = new NbtCompound();
        if (this.nose != null) tag.put("nose", this.nose.toNbt());
        if (this.leftWingTip != null) tag.put("leftWing", this.leftWingTip.toNbt());
        if (this.rightWingTip != null) tag.put("rightWing", this.rightWingTip.toNbt());
        tag.putDouble("comX", this.centerOfMass.x);
        tag.putDouble("comY", this.centerOfMass.y);
        tag.putDouble("comZ", this.centerOfMass.z);
        tag.putDouble("liftX", this.centerOfLift.x);
        tag.putDouble("liftY", this.centerOfLift.y);
        tag.putDouble("liftZ", this.centerOfLift.z);
        tag.putFloat("wingSpan", this.wingSpan);
        tag.putFloat("wingArea", this.wingArea);
        tag.putFloat("liftScale", this.liftScale);
        tag.putFloat("takeoffSpeed", this.takeoffSpeed);
        tag.putFloat("enginePower", this.enginePower);
        tag.putFloat("dragScale", this.dragScale);
        tag.putFloat("controlAssist", this.controlAssist);
        tag.putFloat("balanceOffset", this.balanceOffset);
        tag.putFloat("asymmetry", this.asymmetry);
        NbtList priorityList = new NbtList();
        for (Point point : this.priorityCollisionPoints) priorityList.add(point.toNbt());
        tag.put("priorityCollisionPoints", priorityList);
        NbtList propellerList = new NbtList();
        for (PropellerAssembly propeller : this.propellers) {
            propellerList.add(propeller.toNbt());
        }
        tag.put("propellers", propellerList);
        return tag;
    }

    public static PlaneDefinition fromNbt(NbtCompound tag) {
        Point nose = tag.contains("nose") ? Point.fromNbt(tag.getCompound("nose")) : null;
        Point left = tag.contains("leftWing") ? Point.fromNbt(tag.getCompound("leftWing")) : null;
        Point right = tag.contains("rightWing") ? Point.fromNbt(tag.getCompound("rightWing")) : null;
        Vec3d center = new Vec3d(tag.getDouble("comX"), tag.getDouble("comY"), tag.getDouble("comZ"));
        Vec3d centerOfLift = tag.contains("liftX")
            ? new Vec3d(tag.getDouble("liftX"), tag.getDouble("liftY"), tag.getDouble("liftZ")) : center;
        ArrayList<PropellerAssembly> propellers = new ArrayList<>();
        NbtList list = tag.getList("propellers", NbtElement.COMPOUND_TYPE);
        for (NbtElement element : list) {
            propellers.add(PropellerAssembly.fromNbt((NbtCompound)element));
        }
        ArrayList<Point> priorityPoints = new ArrayList<>();
        if (tag.contains("priorityCollisionPoints")) {
            NbtList priorityList = tag.getList("priorityCollisionPoints", NbtElement.COMPOUND_TYPE);
            for (NbtElement element : priorityList) priorityPoints.add(Point.fromNbt((NbtCompound)element));
        } else {
            if (nose != null) priorityPoints.add(nose);
            if (left != null) priorityPoints.add(left);
            if (right != null) priorityPoints.add(right);
        }
        int bladeCount = propellers.stream().mapToInt(propeller -> propeller.blades().size()).sum();
        float legacyEnginePower = bladeCount > 0 ? MathHelper.clamp(0.55f + 0.18f * (float)Math.sqrt(bladeCount)
            + 0.12f * Math.max(0, propellers.size() - 1), 0.65f, 1.8f) : 0.0f;
        return new PlaneDefinition(nose, left, right, center, centerOfLift, propellers, priorityPoints,
            tag.getFloat("wingSpan"), tag.getFloat("wingArea"),
            tag.getFloat("liftScale"), tag.getFloat("takeoffSpeed"),
            tag.contains("enginePower") ? tag.getFloat("enginePower") : legacyEnginePower,
            tag.contains("dragScale") ? tag.getFloat("dragScale") : 1.0f,
            tag.contains("controlAssist") ? tag.getFloat("controlAssist") : 1.0f,
            tag.getFloat("balanceOffset"), tag.getFloat("asymmetry"));
    }

    private static long coordinateKey(double rx, double ry, double rz) {
        long x = Math.round(rx * 2.0) & 0x1FFFFFL;
        long y = Math.round(ry * 2.0) & 0x1FFFFFL;
        long z = Math.round(rz * 2.0) & 0x1FFFFFL;
        return x << 42 | y << 21 | z;
    }

    /** Coordinates match StoredBlock: X/Z are block centers and Y is the block base. */
    public record Point(double rx, double ry, double rz) {
        public Vec3d blockCenter() {
            return new Vec3d(this.rx, this.ry + 0.5, this.rz);
        }

        public double squaredDistanceTo(Point other) {
            double dx = this.rx - other.rx;
            double dy = this.ry - other.ry;
            double dz = this.rz - other.rz;
            return dx * dx + dy * dy + dz * dz;
        }

        public NbtCompound toNbt() {
            NbtCompound tag = new NbtCompound();
            tag.putDouble("x", this.rx);
            tag.putDouble("y", this.ry);
            tag.putDouble("z", this.rz);
            return tag;
        }

        public static Point fromNbt(NbtCompound tag) {
            return new Point(tag.getDouble("x"), tag.getDouble("y"), tag.getDouble("z"));
        }
    }

    public record PropellerAssembly(Point hub, Vec3d axis, List<Point> blades, boolean clockwise) {
        public PropellerAssembly {
            axis = axis != null && axis.lengthSquared() > 1.0E-8 ? axis.normalize() : new Vec3d(0.0, 0.0, 1.0);
            blades = List.copyOf(blades != null ? blades : List.of());
        }

        public boolean containsBlade(double rx, double ry, double rz) {
            for (Point blade : this.blades) {
                if (Math.abs(blade.rx - rx) < 0.1 && Math.abs(blade.ry - ry) < 0.1 && Math.abs(blade.rz - rz) < 0.1) {
                    return true;
                }
            }
            return false;
        }

        public NbtCompound toNbt() {
            NbtCompound tag = new NbtCompound();
            tag.put("hub", this.hub.toNbt());
            tag.putDouble("axisX", this.axis.x);
            tag.putDouble("axisY", this.axis.y);
            tag.putDouble("axisZ", this.axis.z);
            tag.putBoolean("clockwise", this.clockwise);
            NbtList bladeList = new NbtList();
            for (Point blade : this.blades) bladeList.add(blade.toNbt());
            tag.put("blades", bladeList);
            return tag;
        }

        public static PropellerAssembly fromNbt(NbtCompound tag) {
            Point hub = Point.fromNbt(tag.getCompound("hub"));
            Vec3d axis = new Vec3d(tag.getDouble("axisX"), tag.getDouble("axisY"), tag.getDouble("axisZ"));
            ArrayList<Point> blades = new ArrayList<>();
            NbtList list = tag.getList("blades", NbtElement.COMPOUND_TYPE);
            for (NbtElement element : list) blades.add(Point.fromNbt((NbtCompound)element));
            return new PropellerAssembly(hub, axis, blades, tag.getBoolean("clockwise"));
        }
    }
}
