package com.blockvehicle.entity;

import com.blockvehicle.ModEntities;
import com.blockvehicle.config.BlockVehicleConfig;
import com.blockvehicle.network.VehicleSyncPayload;
import com.blockvehicle.network.PlaneSyncPayload;
import com.blockvehicle.network.PlaneInputPayload;
import com.blockvehicle.vehicle.AircraftOrientation;
import com.blockvehicle.vehicle.PlaneDefinition;
import com.blockvehicle.vehicle.PlaneFlightState;
import com.blockvehicle.vehicle.PlanePhysics;
import com.blockvehicle.sound.ModSounds;
import com.blockvehicle.vehicle.SeatData;
import com.blockvehicle.vehicle.VehicleActivator;
import com.blockvehicle.vehicle.VehicleCollisionHandler;
import com.blockvehicle.vehicle.VehicleInputState;
import com.blockvehicle.vehicle.VehiclePhysics;
import com.blockvehicle.vehicle.VehicleStructure;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandler;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Position;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.joml.Quaternionf;

public class VehicleEntity
extends Entity {
    private static final TrackedData<NbtCompound> STRUCTURE_NBT = DataTracker.registerData(VehicleEntity.class, (TrackedDataHandler)TrackedDataHandlerRegistry.NBT_COMPOUND);
    private VehicleStructure structure = null;
    private float speed = 0.0f;
    private float vehicleYaw = 0.0f;
    private float angularVelocity = 0.0f;
    public float prevAngularVelocity = 0.0f;
    private double verticalVelocity = 0.0;
    private float vehiclePitch = 0.0f;
    private float vehicleRoll = 0.0f;
    public float prevVehiclePitch = 0.0f;
    public float prevVehicleRoll = 0.0f;
    private float wheelRollAngle = 0.0f;
    private float prevWheelRollAngle = 0.0f;
    private float steeringAngle = 0.0f;
    private float prevSteeringAngle = 0.0f;
    private float visualYOffset = 0.0f;
    public float prevVisualYOffset = 0.0f;
    private float tiltLift = 0.0f;
    public float prevTiltLift = 0.0f;
    private boolean wasClimbing = false;
    private int ticksSinceGroundLeft = 0;
    private double lastStepUpSpeed = 0.0;
    private VehicleInputState inputState = VehicleInputState.EMPTY;
    private double motionX = 0.0;
    private double motionZ = 0.0;
    private double impactVelocityX = 0.0;
    private double impactVelocityZ = 0.0;
    private float driftAmount = 0.0f;
    private int landingImpactCooldown = 0;
    private boolean physicsGrounded = false;
    private int offRoadBlockedTicks = 0;
    private Quaternionf aircraftOrientation = new Quaternionf();
    private Quaternionf prevAircraftOrientation = new Quaternionf();
    private Vec3d planeVelocity = Vec3d.ZERO;
    private float throttle = 0.0f;
    private float engineRpm = 0.0f;
    private float planePitchRate = 0.0f;
    private float planeRollRate = 0.0f;
    private float planeYawRate = 0.0f;
    private float stallAmount = 0.0f;
    private float angleOfAttack = 0.0f;
    private float propellerAngle = 0.0f;
    private float prevPropellerAngle = 0.0f;
    private PlaneFlightState planeFlightState = PlaneFlightState.PARKED;
    private int planeImpactStateTicks = 0;
    private int planeGroundContactTicks = 0;
    private int planeAirborneTicks = 0;
    private int lastPlaneInputSequence = -1;
    private UUID planeInputDriver = null;
    private int lastPlaneInputTick = Integer.MIN_VALUE / 2;
    private int lastLocalPlaneInputSequence = -1;
    private boolean planeProbeHitUnloadedChunk = false;
    private Quaternionf clientTargetAircraftOrientation = new Quaternionf();
    private int clientPlaneInterpSteps = 0;
    private final Map<UUID, Integer> passengerSeatMap = new HashMap<UUID, Integer>();
    public double clientTargetX;
    public double clientTargetY;
    public double clientTargetZ;
    public float clientTargetYaw;
    public float clientTargetPitch;
    public float clientTargetRoll;
    public int clientInterpSteps = 0;
    private int clientVisualInterpSteps = 0;
    private float boxHeight = 2.0f;

    public VehicleEntity(EntityType<?> type, World world) {
        super(type, world);
        this.noClip = false;
    }

    public VehicleEntity(World world, Vec3d pos, float yaw, VehicleStructure structure) {
        this(ModEntities.VEHICLE, world);
        this.setPosition(pos.x, pos.y, pos.z);
        this.vehicleYaw = yaw;
        this.setYaw(yaw);
        this.prevYaw = yaw;
        this.setStructure(structure);
    }

    public VehicleStructure getStructure() {
        NbtCompound nbt;
        if (this.structure == null && this.getWorld().isClient() && (nbt = (NbtCompound)this.dataTracker.get(STRUCTURE_NBT)) != null && !nbt.isEmpty()) {
            this.structure = VehicleStructure.fromNbt(nbt);
            this.updateBoundingBoxDimensions();
            this.refreshBoundingBox();
        }
        return this.structure;
    }

    public float getVehicleYaw() {
        return this.getYaw();
    }

    public float getSpeed() {
        return this.speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public float getVehiclePitch() {
        return this.vehiclePitch;
    }

    public float getVehicleRoll() {
        return this.vehicleRoll;
    }

    public float getPrevVehiclePitch() {
        return this.prevVehiclePitch;
    }

    public float getPrevVehicleRoll() {
        return this.prevVehicleRoll;
    }

    public float getWheelRollAngle() {
        return this.wheelRollAngle;
    }

    public float getPrevWheelRollAngle() {
        return this.prevWheelRollAngle;
    }

    public float getSteeringAngle() {
        return this.steeringAngle;
    }

    public float getPrevSteeringAngle() {
        return this.prevSteeringAngle;
    }

    public float getVisualYOffset() {
        return this.visualYOffset;
    }

    public float getPrevVisualYOffset() {
        return this.prevVisualYOffset;
    }

    public float getTiltLift() {
        return this.tiltLift;
    }

    public float getPrevTiltLift() {
        return this.prevTiltLift;
    }

    public float getAngularVelocity() {
        return this.angularVelocity;
    }

    public float getDriftAmount() {
        return this.driftAmount;
    }

    public boolean isPlane() {
        return this.structure != null && this.structure.isPlane();
    }

    public Quaternionf getAircraftOrientation() {
        return new Quaternionf(this.aircraftOrientation);
    }

    public Quaternionf getPrevAircraftOrientation() {
        return new Quaternionf(this.prevAircraftOrientation);
    }

    public Quaternionf getRelativeAircraftOrientation() {
        float initialYaw = this.structure != null ? this.structure.getInitialYaw() : 0.0f;
        return AircraftOrientation.relative(this.aircraftOrientation, initialYaw);
    }

    public Quaternionf getPrevRelativeAircraftOrientation() {
        float initialYaw = this.structure != null ? this.structure.getInitialYaw() : 0.0f;
        return AircraftOrientation.relative(this.prevAircraftOrientation, initialYaw);
    }

    public Vec3d getPlaneVelocity() { return this.planeVelocity; }
    public Vec3d getCollisionVelocity() {
        if (this.isPlane()) return this.planeVelocity;
        float yawRadians = (float)Math.toRadians(this.getYaw());
        return new Vec3d(-Math.sin(yawRadians) * this.speed, this.verticalVelocity,
            Math.cos(yawRadians) * this.speed);
    }

    public void dampenCollisionVelocity(float factor) {
        factor = MathHelper.clamp(factor, 0.0f, 1.0f);
        if (this.isPlane()) {
            this.planeVelocity = this.planeVelocity.multiply(factor);
            this.speed = (float)this.planeVelocity.length();
            this.setVelocity(this.planeVelocity);
        } else {
            this.speed *= factor;
        }
    }
    public float getThrottle() { return this.throttle; }
    public float getEngineRpm() { return this.engineRpm; }
    public float getPlanePitchRate() { return this.planePitchRate; }
    public float getPlaneRollRate() { return this.planeRollRate; }
    public float getPlaneYawRate() { return this.planeYawRate; }
    public float getStallAmount() { return this.stallAmount; }
    public float getAngleOfAttack() { return this.angleOfAttack; }
    public float getPropellerAngle() { return this.propellerAngle; }
    public float getPrevPropellerAngle() { return this.prevPropellerAngle; }
    public PlaneFlightState getPlaneFlightState() { return this.planeFlightState; }

    public Vec3d transformStructurePoint(Vec3d localPoint) {
        if (!this.isPlane()) {
            float relativeYaw = this.getYaw() - this.structure.getInitialYaw();
            float yawRad = (float)Math.toRadians(relativeYaw);
            float pitchRad = (float)Math.toRadians(this.vehiclePitch);
            float rollRad = (float)Math.toRadians(this.vehicleRoll);
            SeatData helper = new SeatData(localPoint.x, localPoint.y, localPoint.z, false);
            return helper.toWorldPos(this.getPos().add(0.0, this.visualYOffset, 0.0), yawRad, pitchRad, rollRad);
        }
        PlaneDefinition definition = this.structure.getPlaneDefinition();
        Vec3d transformed = AircraftOrientation.transformAroundPivot(this.getRelativeAircraftOrientation(), localPoint, definition.centerOfMass());
        return this.getPos().add(transformed);
    }

    public void markOffRoadBlocked() {
        this.offRoadBlockedTicks = Math.max(this.offRoadBlockedTicks, 7);
    }

    public void setAngularVelocity(float omega) {
        this.angularVelocity = omega;
    }

    public void addAngularVelocity(float deltaOmega) {
        if (this.isPlane()) {
            this.planeYawRate = MathHelper.clamp(this.planeYawRate + deltaOmega * 0.25f, -3.0f, 3.0f);
            return;
        }
        this.angularVelocity = MathHelper.clamp(this.angularVelocity + deltaOmega, -VehiclePhysics.MAX_ANGULAR_VELOCITY, VehiclePhysics.MAX_ANGULAR_VELOCITY);
    }

    public int getBlockCount() {
        return this.structure != null ? this.structure.getBlocks().size() : 30;
    }

    public float getTotalMass() {
        return this.structure != null ? this.structure.getTotalMass() : 25.0f;
    }

    public float getWeightFactor() {
        float mass = this.getTotalMass();
        return (float)Math.sqrt(Math.max(mass, 3.0f) / 25.0f);
    }

    public float getMaxSpeed() {
        float wf = this.getWeightFactor();
        float scaled = (float)((double)0.85f / Math.pow(wf, 0.38));
        return MathHelper.clamp((float)scaled, (float)0.48f, (float)1.25f);
    }

    public float getAcceleration() {
        float wf = this.getWeightFactor();
        float scaled = 0.016f / wf;
        return MathHelper.clamp((float)scaled, (float)0.01f, (float)0.024f);
    }

    public float getReverseAcceleration() {
        return this.getAcceleration() * 0.6f;
    }

    public float getMaxReverseSpeed() {
        return this.getMaxSpeed() * 0.42f;
    }

    public float getMomentOfInertia() {
        if (this.structure == null) {
            return 1.0f;
        }
        double w = this.structure.getWidth();
        double l = this.structure.getLength();
        float wf = this.getWeightFactor();
        double rawInertia = (double)wf * (w * w + l * l) / 18.0;
        return (float)MathHelper.clamp((double)rawInertia, (double)0.6, (double)6.0);
    }

    public void setYaw(float yaw) {
        super.setYaw(yaw);
        this.vehicleYaw = yaw;
    }

    public void setStructure(VehicleStructure structure) {
        this.structure = structure;
        if (structure != null) {
            if (structure.isPlane() && this.aircraftOrientation.lengthSquared() < 0.5f) {
                this.aircraftOrientation = AircraftOrientation.fromYaw(this.getYaw());
                this.prevAircraftOrientation.set(this.aircraftOrientation);
            } else if (structure.isPlane() && this.age == 0 && this.planeFlightState == PlaneFlightState.PARKED) {
                this.aircraftOrientation = AircraftOrientation.fromYaw(structure.getInitialYaw());
                this.prevAircraftOrientation.set(this.aircraftOrientation);
            }
            this.updateBoundingBoxDimensions();
            if (!this.getWorld().isClient()) {
                this.dataTracker.set(STRUCTURE_NBT, structure.toNbt());
            }
            this.refreshBoundingBox();
        }
    }

    public void onTrackedDataSet(TrackedData<?> data) {
        NbtCompound nbt;
        super.onTrackedDataSet(data);
        if (STRUCTURE_NBT.equals(data) && this.structure == null && (nbt = (NbtCompound)this.dataTracker.get(STRUCTURE_NBT)) != null && !nbt.isEmpty()) {
            this.structure = VehicleStructure.fromNbt(nbt);
            this.updateBoundingBoxDimensions();
            this.refreshBoundingBox();
        }
    }

    public Box computeRotatedBoundingBox() {
        if (this.structure == null) {
            return this.getDimensions(this.getPose()).getBoxAt(this.getPos());
        }
        double x = this.getX();
        double y = this.getY();
        double z = this.getZ();
        if (this.structure.isPlane()) {
            double minX = Double.POSITIVE_INFINITY;
            double minY = Double.POSITIVE_INFINITY;
            double minZ = Double.POSITIVE_INFINITY;
            double maxX = Double.NEGATIVE_INFINITY;
            double maxY = Double.NEGATIVE_INFINITY;
            double maxZ = Double.NEGATIVE_INFINITY;
            double[] xs = {this.structure.getMinLocalX(), this.structure.getMaxLocalX()};
            double[] ys = {this.structure.getMinLocalY(), this.structure.getMaxLocalY()};
            double[] zs = {this.structure.getMinLocalZ(), this.structure.getMaxLocalZ()};
            PlaneDefinition definition = this.structure.getPlaneDefinition();
            Quaternionf relative = this.getRelativeAircraftOrientation();
            for (double lx : xs) for (double ly : ys) for (double lz : zs) {
                Vec3d transformed = AircraftOrientation.transformAroundPivot(relative, new Vec3d(lx, ly, lz), definition.centerOfMass());
                minX = Math.min(minX, transformed.x);
                minY = Math.min(minY, transformed.y);
                minZ = Math.min(minZ, transformed.z);
                maxX = Math.max(maxX, transformed.x);
                maxY = Math.max(maxY, transformed.y);
                maxZ = Math.max(maxZ, transformed.z);
            }
            double extentLimit = BlockVehicleConfig.get().maxVehicleAxis * 0.5 + 1.0;
            minX = Math.max(minX, -extentLimit);
            minY = Math.max(minY, -extentLimit);
            minZ = Math.max(minZ, -extentLimit);
            maxX = Math.min(maxX, extentLimit);
            maxY = Math.min(maxY, extentLimit);
            maxZ = Math.min(maxZ, extentLimit);
            return new Box(x + minX - 0.04, y + minY - 0.04, z + minZ - 0.04,
                x + maxX + 0.04, y + maxY + 0.04, z + maxZ + 0.04);
        }
        float relativeYaw = this.getYaw() - this.structure.getInitialYaw();
        float rad = (float)Math.toRadians(relativeYaw);
        double absCos = Math.abs(Math.cos(rad));
        double absSin = Math.abs(Math.sin(rad));
        double halfW = (double)this.structure.getWidth() / 2.0;
        double halfL = (double)this.structure.getLength() / 2.0;
        double halfX = halfW * absCos + halfL * absSin + 0.05;
        double halfZ = halfW * absSin + halfL * absCos + 0.05;
        return new Box(x - halfX, y, z - halfZ, x + halfX, y + (double)this.boxHeight, z + halfZ);
    }

    public EntityDimensions getDimensions(EntityPose pose) {
        if (this.structure != null) {
            float limit = BlockVehicleConfig.get().maxVehicleAxis;
            float size = Math.min(Math.max(this.structure.getWidth(), this.structure.getLength()), limit);
            return EntityDimensions.changing(size, Math.min(this.structure.getHeight(), limit));
        }
        return super.getDimensions(pose);
    }

    private void updateBoundingBoxDimensions() {
        if (this.structure == null) {
            return;
        }
        this.boxHeight = Math.min(this.structure.getHeight(), BlockVehicleConfig.get().maxVehicleAxis);
        this.calculateDimensions();
    }

    private void refreshBoundingBox() {
        this.setBoundingBox(this.computeRotatedBoundingBox());
    }

    public void setInputState(VehicleInputState state) {
        this.inputState = state;
    }

    public VehicleInputState getInputState() {
        return this.inputState;
    }

    public LivingEntity getControllingPassenger() {
        if (this.structure == null) return null;
        List<SeatData> seats = this.structure.getSeats();
        for (Entity passenger : this.getPassengerList()) {
            if (passenger instanceof LivingEntity living && this.getSeatFor(passenger, seats).isDriver) {
                return living;
            }
        }
        return null;
    }

    public boolean isDrivenByLocalPlayer() {
        PlayerEntity player;
        LivingEntity livingEntity;
        return this.getWorld().isClient() && (livingEntity = this.getControllingPassenger()) instanceof PlayerEntity && (player = (PlayerEntity)livingEntity).isMainPlayer();
    }

    @Override
    public boolean isLogicalSideForUpdatingMovement() {
        if (this.isDrivenByLocalPlayer()) {
            // BlockVehicle has rotated, per-block collision geometry that vanilla's
            // vehicle-movement validator cannot reproduce. The mod's single C2S
            // movement stream below is authoritative for the local driver.
            return false;
        }
        return super.isLogicalSideForUpdatingMovement();
    }

    public void applyClientDriverUpdate(double x, double y, double z, float yaw, float spd, float pitch, float roll) {
        double deltaY = y - this.getY();
        this.prevX = this.getX();
        this.prevY = this.getY();
        this.prevZ = this.getZ();
        this.prevYaw = this.getYaw();
        this.prevVehiclePitch = this.vehiclePitch;
        this.prevVehicleRoll = this.vehicleRoll;
        this.setPosition(x, y, z);
        this.setYaw(yaw);
        this.speed = MathHelper.clamp(spd, -this.getMaxSpeed(), this.getMaxSpeed());
        this.vehiclePitch = MathHelper.clamp(pitch, -35.0f, 35.0f);
        this.vehicleRoll = MathHelper.clamp(roll, -35.0f, 35.0f);
        this.verticalVelocity = MathHelper.clamp(deltaY, -VehiclePhysics.MAX_FALL_SPEED, 1.2);
        this.motionX = -Math.sin((double)((float)Math.toRadians(yaw))) * (double)this.speed;
        this.motionZ = Math.cos((double)((float)Math.toRadians(yaw))) * (double)this.speed;
        this.setVelocity(this.motionX, this.verticalVelocity, this.motionZ);
        this.velocityModified = true;
        this.refreshBoundingBox();
    }

    public boolean isValidClientDriverUpdate(double x, double y, double z, float yaw, float spd, float pitch, float roll) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
            || !Float.isFinite(yaw) || !Float.isFinite(spd) || !Float.isFinite(pitch) || !Float.isFinite(roll)) {
            return false;
        }
        double dx = x - this.getX();
        double dz = z - this.getZ();
        double maxHorizontalDelta = Math.max(3.0, this.getMaxSpeed() * 5.0 + 1.0);
        return dx * dx + dz * dz <= maxHorizontalDelta * maxHorizontalDelta
            && Math.abs(y - this.getY()) <= 12.0
            && Math.abs(spd) <= this.getMaxSpeed() * 1.35f
            && Math.abs(pitch) <= 35.0f
            && Math.abs(roll) <= 35.0f;
    }

    public boolean acceptPlaneInput(UUID driverId, PlaneInputPayload payload) {
        if (!this.isPlane() || driverId == null || payload.sequence() < 0
            || (payload.flags() & ~0x3FF) != 0
            || !Float.isFinite(payload.lookYaw()) || !Float.isFinite(payload.lookPitch())
            || Math.abs(payload.lookPitch()) > 90.01f || Math.abs(payload.lookYaw()) > 1.0E7f) {
            return false;
        }
        if (!driverId.equals(this.planeInputDriver)) {
            this.planeInputDriver = driverId;
            this.lastPlaneInputSequence = -1;
        }
        if (payload.sequence() <= this.lastPlaneInputSequence) return false;
        this.lastPlaneInputSequence = payload.sequence();
        this.lastPlaneInputTick = this.age;
        int flags = payload.flags();
        this.inputState = new VehicleInputState((flags & 1) != 0, (flags & 2) != 0,
            (flags & 4) != 0, (flags & 8) != 0, (flags & 16) != 0,
            (flags & 32) != 0, (flags & 64) != 0, (flags & 128) != 0, (flags & 256) != 0, (flags & 512) != 0,
            payload.lookYaw(), payload.lookPitch());
        return true;
    }

    public void recordLocalPlaneInputSequence(int sequence) {
        this.lastLocalPlaneInputSequence = sequence;
    }

    public void applyCollisionImpulse(double impulseX, double impulseY, double impulseZ, float angularImpulse) {
        if (this.isPlane()) {
            this.planeVelocity = this.planeVelocity.add(
                MathHelper.clamp(impulseX, -1.8, 1.8),
                MathHelper.clamp(impulseY, -1.8, 1.8),
                MathHelper.clamp(impulseZ, -1.8, 1.8));
            this.planeYawRate = MathHelper.clamp(this.planeYawRate + angularImpulse * 0.25f, -3.0f, 3.0f);
            this.setOnGround(false);
            return;
        }
        this.impactVelocityX = MathHelper.clamp(this.impactVelocityX + impulseX, -1.8, 1.8);
        this.impactVelocityZ = MathHelper.clamp(this.impactVelocityZ + impulseZ, -1.8, 1.8);
        this.verticalVelocity = MathHelper.clamp(this.verticalVelocity + impulseY, -VehiclePhysics.MAX_FALL_SPEED, 1.2);
        this.addAngularVelocity(angularImpulse);
        this.setOnGround(false);
    }

    public void tick() {
        this.prevX = this.getX();
        this.prevY = this.getY();
        this.prevZ = this.getZ();
        this.prevYaw = this.getYaw();
        this.prevVehiclePitch = this.vehiclePitch;
        this.prevVehicleRoll = this.vehicleRoll;
        this.prevWheelRollAngle = this.wheelRollAngle;
        this.prevSteeringAngle = this.steeringAngle;
        this.prevVisualYOffset = this.visualYOffset;
        this.prevTiltLift = this.tiltLift;
        this.prevAngularVelocity = this.angularVelocity;
        this.prevAircraftOrientation.set(this.aircraftOrientation);
        this.prevPropellerAngle = this.propellerAngle;
        if (this.landingImpactCooldown > 0) {
            --this.landingImpactCooldown;
        }
        if (this.planeImpactStateTicks > 0) {
            --this.planeImpactStateTicks;
        }
        if (this.offRoadBlockedTicks > 0) {
            --this.offRoadBlockedTicks;
        }
        this.visualYOffset += (0.0f - this.visualYOffset) * 0.28f;
        if (Math.abs(this.visualYOffset) < 0.002f) {
            this.visualYOffset = 0.0f;
        }
        super.tick();
        if (this.getWorld().isClient()) {
            if (this.isDrivenByLocalPlayer()) {
                if (this.isPlane()) this.tickPlanePhysics();
                else this.tickPhysics();
            } else {
                this.tickClientInterp();
            }
            return;
        }
        if (this.isPlane()) {
            // The server owns the canonical plane simulation. The pilot runs the
            // same step locally for immediate controls, then reconciles snapshots.
            if (this.hasDriver() && this.age - this.lastPlaneInputTick > 10) {
                this.inputState = new VehicleInputState(false, false, false, false, false,
                    false, false, false, false, this.getYaw(), -this.vehiclePitch);
            }
            this.tickPlanePhysics();
        } else if (!this.hasDriver()) {
            this.tickPhysics();
        } else {
            VehicleCollisionHandler.handleCollisions(this);
        }
        this.syncToClients();
    }

    public void updateTrackedPositionAndAngles(double x, double y, double z, float yaw, float pitch, int interpolationSteps) {
        if (this.isDrivenByLocalPlayer()) {
            return;
        }
        double distSq = this.squaredDistanceTo(x, y, z);
        if (distSq > 64.0) {
            this.setPosition(x, y, z);
            this.setYaw(yaw);
            this.prevX = x;
            this.prevY = y;
            this.prevZ = z;
            this.prevYaw = yaw;
            this.clientInterpSteps = 0;
            return;
        }
        this.clientTargetX = x;
        this.clientTargetY = y;
        this.clientTargetZ = z;
        this.clientTargetYaw = yaw;
        this.clientInterpSteps = Math.max(interpolationSteps, 1);
    }

    private void tickClientInterp() {
        if (this.clientInterpSteps > 0) {
            double nx = this.getX() + (this.clientTargetX - this.getX()) / (double)this.clientInterpSteps;
            double ny = this.getY() + (this.clientTargetY - this.getY()) / (double)this.clientInterpSteps;
            double nz = this.getZ() + (this.clientTargetZ - this.getZ()) / (double)this.clientInterpSteps;
            float nyw = this.getYaw() + MathHelper.wrapDegrees((float)(this.clientTargetYaw - this.getYaw())) / (float)this.clientInterpSteps;
            --this.clientInterpSteps;
            this.setPosition(nx, ny, nz);
            this.setYaw(nyw);
        }
        if (this.clientVisualInterpSteps > 0) {
            this.vehiclePitch += (this.clientTargetPitch - this.vehiclePitch) / (float)this.clientVisualInterpSteps;
            this.vehicleRoll += (this.clientTargetRoll - this.vehicleRoll) / (float)this.clientVisualInterpSteps;
            --this.clientVisualInterpSteps;
        }
        if (this.isPlane() && this.clientPlaneInterpSteps > 0) {
            float amount = 1.0f / (float)this.clientPlaneInterpSteps;
            this.aircraftOrientation.slerp(this.clientTargetAircraftOrientation, amount).normalize();
            --this.clientPlaneInterpSteps;
            this.setYaw(AircraftOrientation.yawDegrees(this.aircraftOrientation));
            this.vehiclePitch = AircraftOrientation.pitchDegrees(this.aircraftOrientation);
            this.vehicleRoll = AircraftOrientation.rollDegrees(this.aircraftOrientation);
        }
        if (this.isPlane()) {
            this.propellerAngle = MathHelper.wrapDegrees(this.propellerAngle + this.engineRpm * 42.0f);
            this.refreshBoundingBox();
            return;
        }
        float rollDelta = this.speed / 0.65f * 57.295776f * 0.45f;
        this.wheelRollAngle += rollDelta;
        this.steeringAngle += (0.0f - this.steeringAngle) * 0.2f;
        this.angularVelocity += (0.0f - this.angularVelocity) * 0.25f;
        this.refreshBoundingBox();
    }

    private void tickPlanePhysics() {
        if (!this.isPlane()) {
            return;
        }
        boolean wasGrounded = this.physicsGrounded;
        double descentSpeed = Math.max(0.0, -this.planeVelocity.y);
        PlaneGroundContact groundContact = this.samplePlaneGroundContact(PlanePhysics.GEAR_REACH);
        boolean contactNow = groundContact.supported() && groundContact.gap() <= 0.18;
        if (wasGrounded && this.planeVelocity.y <= 0.015 && this.planeFlightState != PlaneFlightState.TAKEOFF
            && groundContact.supported() && groundContact.gap() > 0.07) {
            double settle = Math.min(0.12, groundContact.gap() - 0.05);
            Vec3d settledPosition = this.getPos().add(0.0, -settle, 0.0);
            if (settle > 0.0 && this.isPlanePositionClear(settledPosition, this.aircraftOrientation, true)) this.setPosition(settledPosition);
        }
        if (contactNow) {
            this.planeGroundContactTicks = Math.min(this.planeGroundContactTicks + 1, 10);
            this.planeAirborneTicks = 0;
        } else {
            this.planeAirborneTicks = Math.min(this.planeAirborneTicks + 1, 10);
            this.planeGroundContactTicks = Math.max(0, this.planeGroundContactTicks - 1);
        }
        boolean grounded = this.planeGroundContactTicks >= 2
            || contactNow && this.planeVelocity.lengthSquared() < 0.01
            || wasGrounded && groundContact.supported() && this.planeAirborneTicks < 3;
        PlanePhysics.State state = new PlanePhysics.State(new Quaternionf(this.aircraftOrientation), this.planeVelocity,
            this.throttle, this.engineRpm, this.planePitchRate, this.planeRollRate, this.planeYawRate,
            this.stallAmount, this.planeFlightState, this.getYaw());
        PlanePhysics.Result result = PlanePhysics.step(state, this.inputState, this.structure.getPlaneDefinition(),
            grounded, this.hasTerrainBelow(4.0), this.hasDriver(), this.getWeightFactor());
        Quaternionf safeOrientation = this.resolvePlaneRotation(this.aircraftOrientation, result.orientation());
        boolean rotationBlocked = Math.abs(safeOrientation.dot(result.orientation())) < 0.99999f;
        this.aircraftOrientation.set(safeOrientation);
        this.planeVelocity = result.velocity();
        this.throttle = result.throttle();
        this.engineRpm = result.engineRpm();
        this.planePitchRate = result.pitchRate();
        this.planeRollRate = result.rollRate();
        this.planeYawRate = result.yawRate();
        if (rotationBlocked) {
            this.planePitchRate *= -0.10f;
            this.planeRollRate *= -0.10f;
            this.planeYawRate *= 0.25f;
        }
        this.stallAmount = result.stallAmount();
        this.angleOfAttack = result.angleOfAttack();
        this.speed = result.airspeed();
        if (this.planeImpactStateTicks <= 0
            || this.planeFlightState != PlaneFlightState.CRASHED && this.planeFlightState != PlaneFlightState.HARD_LANDING) {
            this.planeFlightState = result.flightState();
        }
        this.propellerAngle = MathHelper.wrapDegrees(this.propellerAngle + this.engineRpm * 42.0f);
        this.setYaw(AircraftOrientation.yawDegrees(this.aircraftOrientation));
        this.vehiclePitch = AircraftOrientation.pitchDegrees(this.aircraftOrientation);
        this.vehicleRoll = AircraftOrientation.rollDegrees(this.aircraftOrientation);
        this.movePlaneSwept(this.planeVelocity);
        PlaneGroundContact contactAfter = this.samplePlaneGroundContact(PlanePhysics.GEAR_REACH);
        boolean deliberatelyDeparting = this.planeVelocity.y > 0.025
            && this.speed > this.structure.getPlaneDefinition().takeoffSpeed() * 0.72f;
        boolean supportedAfter = contactAfter.supported() && contactAfter.gap() <= 0.20
            || !deliberatelyDeparting && wasGrounded && contactAfter.supported() && contactAfter.gap() <= PlanePhysics.GEAR_REACH;
        this.physicsGrounded = supportedAfter;
        if (supportedAfter && this.planeVelocity.y < 0.0) {
            this.planeVelocity = new Vec3d(this.planeVelocity.x, 0.0, this.planeVelocity.z);
            this.setOnGround(true);
        } else if (!supportedAfter) {
            this.setOnGround(false);
        }
        if (supportedAfter && !wasGrounded && descentSpeed > 0.16) {
            this.applyPlaneLandingImpact(descentSpeed);
        }
        if (this.isTouchingWater()) {
            // Water is not a solid collision shape, so damp a ditched aircraft
            // aggressively and add a little buoyancy instead of letting it fly
            // through an ocean at full airspeed.
            this.planeVelocity = this.planeVelocity.multiply(0.72).add(0.0, 0.025, 0.0);
            this.throttle *= 0.94f;
            this.engineRpm *= 0.96f;
        } else if (this.isInLava()) {
            this.planeVelocity = this.planeVelocity.multiply(0.58).add(0.0, 0.01, 0.0);
            this.throttle *= 0.85f;
            this.engineRpm *= 0.90f;
        }
        this.motionX = this.planeVelocity.x;
        this.motionZ = this.planeVelocity.z;
        this.verticalVelocity = this.planeVelocity.y;
        this.setVelocity(this.planeVelocity);
        if (!this.getWorld().isClient()) {
            VehicleCollisionHandler.handleCollisions(this);
        }
        this.refreshBoundingBox();
    }

    private void applyPlaneLandingImpact(double descentSpeed) {
        float severity = MathHelper.clamp((float)((descentSpeed - 0.16) / 0.62), 0.0f, 1.0f);
        this.planeFlightState = descentSpeed > 0.72 ? PlaneFlightState.CRASHED : PlaneFlightState.HARD_LANDING;
        this.planeImpactStateTicks = descentSpeed > 0.72 ? 50 : 16;
        float horizontalRetention = MathHelper.lerp(severity, 0.93f, 0.66f);
        this.planeVelocity = new Vec3d(this.planeVelocity.x * horizontalRetention, 0.0,
            this.planeVelocity.z * horizontalRetention);
        this.planePitchRate *= 0.45f;
        this.planeRollRate *= 0.52f;
        this.planeYawRate *= 0.72f;
        this.visualYOffset = Math.max(-0.72f, this.visualYOffset - 0.18f - severity * 0.42f);
        if (this.landingImpactCooldown == 0 && !this.getWorld().isClient()) {
            this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.CAR_IMPACT,
                SoundCategory.BLOCKS, 0.45f + severity * 0.55f, 1.12f - severity * 0.34f);
            if (this.getWorld() instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(severity > 0.72f ? ParticleTypes.LARGE_SMOKE : ParticleTypes.CLOUD,
                    this.getX(), this.getY() + 0.15, this.getZ(), 4 + Math.round(severity * 10.0f),
                    0.55, 0.10, 0.55, 0.035);
            }
            this.landingImpactCooldown = 10;
        }
    }

    private PlaneGroundContact samplePlaneGroundContact(double reach) {
        if (!this.isPlane()) return PlaneGroundContact.NONE;
        List<VehicleStructure.WheelData> wheels = this.structure.getWheels();
        int support = 0;
        int samples = 0;
        double closestGap = Double.POSITIVE_INFINITY;
        if (!wheels.isEmpty()) {
            int count = Math.min(8, wheels.size());
            for (int i = 0; i < count; ++i) {
                VehicleStructure.WheelData wheel = wheels.get((int)((long)i * wheels.size() / count));
                Vec3d worldPoint = this.transformStructurePoint(new Vec3d(wheel.rx(), wheel.ry(), wheel.rz()));
                ++samples;
                double gap = this.findSupportGapAt(worldPoint.x, worldPoint.y, worldPoint.z, reach);
                if (Double.isFinite(gap)) {
                    ++support;
                    closestGap = Math.min(closestGap, gap);
                }
            }
        } else {
            List<VehicleStructure.StoredBlock> contacts = this.structure.getContactBlocks();
            int count = Math.min(12, contacts.size());
            for (int i = 0; i < count; ++i) {
                VehicleStructure.StoredBlock block = contacts.get((int)((long)i * contacts.size() / count));
                Vec3d worldPoint = this.transformStructurePoint(new Vec3d(block.rx(), block.ry(), block.rz()));
                ++samples;
                double gap = this.findSupportGapAt(worldPoint.x, worldPoint.y, worldPoint.z, Math.min(reach, 0.30));
                if (Double.isFinite(gap)) {
                    ++support;
                    closestGap = Math.min(closestGap, gap);
                }
            }
        }
        int required = samples >= 4 ? 2 : 1;
        return support >= required ? new PlaneGroundContact(true, closestGap, support) : PlaneGroundContact.NONE;
    }

    private record PlaneGroundContact(boolean supported, double gap, int samples) {
        private static final PlaneGroundContact NONE = new PlaneGroundContact(false, Double.POSITIVE_INFINITY, 0);
    }

    private void movePlaneSwept(Vec3d requestedVelocity) {
        if (requestedVelocity.lengthSquared() < 1.0E-12) return;
        Box startBox = this.computeRotatedBoundingBox();
        Box endBox = startBox.offset(requestedVelocity);
        Box sweptBounds = startBox.union(endBox).expand(0.04);
        if (this.arePlaneBoundsInLoadedWorld(sweptBounds)
            && !this.getWorld().getBlockCollisions(this, sweptBounds).iterator().hasNext()) {
            this.setPosition(this.getPos().add(requestedVelocity));
            return;
        }
        double distance = requestedVelocity.length();
        int steps = MathHelper.clamp((int)Math.ceil(distance / 0.28), 1, PlanePhysics.MAX_COLLISION_SUBSTEPS);
        Vec3d step = requestedVelocity.multiply(1.0 / steps);
        Vec3d adjustedVelocity = requestedVelocity;
        double impactSpeed = 0.0;
        for (int i = 0; i < steps; ++i) {
            Vec3d start = this.getPos();
            Vec3d candidate = start;
            boolean hitX = false;
            boolean hitY = false;
            boolean hitZ = false;
            boolean hitUnloadedChunk = false;
            if (Math.abs(step.x) > 1.0E-8) {
                Vec3d next = candidate.add(step.x, 0.0, 0.0);
                if (this.isPlanePositionClear(next, this.aircraftOrientation, true)) candidate = next;
                else {
                    hitX = true;
                    hitUnloadedChunk |= this.planeProbeHitUnloadedChunk;
                }
            }
            if (Math.abs(step.y) > 1.0E-8) {
                Vec3d next = candidate.add(0.0, step.y, 0.0);
                if (this.isPlanePositionClear(next, this.aircraftOrientation, true)) candidate = next;
                else {
                    hitY = true;
                    hitUnloadedChunk |= this.planeProbeHitUnloadedChunk;
                }
            }
            if (Math.abs(step.z) > 1.0E-8) {
                Vec3d next = candidate.add(0.0, 0.0, step.z);
                if (this.isPlanePositionClear(next, this.aircraftOrientation, true)) candidate = next;
                else {
                    hitZ = true;
                    hitUnloadedChunk |= this.planeProbeHitUnloadedChunk;
                }
            }
            this.setPosition(candidate.x, candidate.y, candidate.z);
            double actualX = candidate.x - start.x;
            double actualY = candidate.y - start.y;
            double actualZ = candidate.z - start.z;
            if (hitX || hitY || hitZ) {
                if (!hitUnloadedChunk) impactSpeed = Math.max(impactSpeed, Math.sqrt(
                    (step.x - actualX) * (step.x - actualX)
                        + (step.y - actualY) * (step.y - actualY)
                        + (step.z - actualZ) * (step.z - actualZ)) * steps);
                adjustedVelocity = new Vec3d(hitX ? actualX * steps * 0.15 : adjustedVelocity.x,
                    hitY ? Math.max(0.0, actualY * steps * 0.1) : adjustedVelocity.y,
                    hitZ ? actualZ * steps * 0.15 : adjustedVelocity.z);
                if (hitX) this.planeYawRate += Math.signum(step.z) * 0.35f;
                if (hitZ) this.planeYawRate -= Math.signum(step.x) * 0.35f;
                break;
            }
        }
        this.planeVelocity = adjustedVelocity;
        if (impactSpeed > 0.30) {
            if (impactSpeed > 0.82) {
                this.planeFlightState = PlaneFlightState.CRASHED;
                this.planeImpactStateTicks = 50;
            } else if (requestedVelocity.y < -0.18) {
                this.planeFlightState = PlaneFlightState.HARD_LANDING;
                this.planeImpactStateTicks = 16;
            }
            this.visualYOffset = Math.max(-0.75f, this.visualYOffset - (float)Math.min(0.55, impactSpeed * 0.35));
            if (this.landingImpactCooldown == 0 && !this.getWorld().isClient()) {
                this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.CAR_IMPACT,
                    SoundCategory.BLOCKS, MathHelper.clamp((float)impactSpeed, 0.35f, 1.0f),
                    MathHelper.clamp(1.2f - (float)impactSpeed * 0.25f, 0.72f, 1.1f));
                this.landingImpactCooldown = 8;
            }
        }
    }

    /** Sweeps the changing orientation so a stationary wing cannot rotate through a wall. */
    private Quaternionf resolvePlaneRotation(Quaternionf start, Quaternionf requested) {
        Quaternionf from = AircraftOrientation.sanitize(start, this.getYaw());
        Quaternionf target = AircraftOrientation.sanitize(requested, this.getYaw());
        float directDelta = Math.max(Math.max(Math.abs(from.x - target.x), Math.abs(from.y - target.y)),
            Math.max(Math.abs(from.z - target.z), Math.abs(from.w - target.w)));
        float flippedDelta = Math.max(Math.max(Math.abs(from.x + target.x), Math.abs(from.y + target.y)),
            Math.max(Math.abs(from.z + target.z), Math.abs(from.w + target.w)));
        if (Math.min(directDelta, flippedDelta) < 1.0E-7f) return target;
        float dot = MathHelper.clamp(Math.abs(from.dot(target)), 0.0f, 1.0f);
        float angleDegrees = (float)Math.toDegrees(2.0 * Math.acos(dot));
        int steps = MathHelper.clamp((int)Math.ceil(angleDegrees / 2.0f), 1, 4);
        Quaternionf lastClear = new Quaternionf(from);
        for (int i = 1; i <= steps; ++i) {
            Quaternionf candidate = new Quaternionf(from).slerp(target, i / (float)steps).normalize();
            if (!this.isPlanePositionClear(this.getPos(), candidate, true)) break;
            lastClear.set(candidate);
        }
        return lastClear;
    }

    /** Cached surface probes provide an oriented body collision without a huge AABB snagging empty corners. */
    private boolean isPlanePositionClear(Vec3d candidatePosition) {
        return this.isPlanePositionClear(candidatePosition, this.aircraftOrientation);
    }

    private boolean isPlanePositionClear(Vec3d candidatePosition, Quaternionf absoluteOrientation) {
        return this.isPlanePositionClear(candidatePosition, absoluteOrientation, false);
    }

    private boolean isPlanePositionClear(Vec3d candidatePosition, Quaternionf absoluteOrientation, boolean ignoreLandingGear) {
        this.planeProbeHitUnloadedChunk = false;
        List<VehicleStructure.StoredBlock> probes = this.structure.getCollisionProbeBlocks();
        if (probes.isEmpty()) return true;
        int samples = Math.min(112, probes.size());
        Quaternionf relative = AircraftOrientation.relative(absoluteOrientation, this.structure.getInitialYaw());
        PlaneDefinition definition = this.structure.getPlaneDefinition();
        for (int sample = 0; sample < samples; ++sample) {
            VehicleStructure.StoredBlock block = probes.get((int)((long)sample * probes.size() / samples));
            if (this.structure.isPropellerBlade(block.rx(), block.ry(), block.rz())) continue;
            if (ignoreLandingGear && this.structure.isWheel(block.rx(), block.ry(), block.rz())) continue;
            Vec3d localCenter = new Vec3d(block.rx(), block.ry() + 0.5, block.rz());
            if (!this.isPlaneProbeClear(candidatePosition, relative, definition, localCenter)) return false;
        }
        // Activation caches real blocks at the nose, wings, tail, top and bottom.
        for (PlaneDefinition.Point point : definition.priorityCollisionPoints()) {
            if (!this.isPlaneProbeClear(candidatePosition, relative, definition, point.blockCenter())) return false;
        }
        if (!ignoreLandingGear) {
            int gearSamples = Math.min(16, this.structure.getWheels().size());
            for (int i = 0; i < gearSamples; ++i) {
                VehicleStructure.WheelData wheel = this.structure.getWheels().get(
                    (int)((long)i * this.structure.getWheels().size() / gearSamples));
                if (!this.isPlaneProbeClear(candidatePosition, relative, definition,
                    new Vec3d(wheel.rx(), wheel.ry() + 0.5, wheel.rz()))) return false;
            }
        }
        return true;
    }

    private boolean isPlaneProbeClear(Vec3d candidatePosition, Quaternionf relative,
                                      PlaneDefinition definition, Vec3d localCenter) {
            Vec3d offset = AircraftOrientation.transformAroundPivot(relative, localCenter, definition.centerOfMass());
            Vec3d center = candidatePosition.add(offset);
            Box probeBox = new Box(center.x - 0.43, center.y - 0.43, center.z - 0.43,
                center.x + 0.43, center.y + 0.43, center.z + 0.43);
            int minX = MathHelper.floor(probeBox.minX);
            int minY = MathHelper.floor(probeBox.minY);
            int minZ = MathHelper.floor(probeBox.minZ);
            int maxX = MathHelper.floor(probeBox.maxX);
            int maxY = MathHelper.floor(probeBox.maxY);
            int maxZ = MathHelper.floor(probeBox.maxZ);
            for (int bx = minX; bx <= maxX; ++bx) for (int by = minY; by <= maxY; ++by) for (int bz = minZ; bz <= maxZ; ++bz) {
                BlockPos pos = new BlockPos(bx, by, bz);
                if (!this.getWorld().isInBuildLimit(pos)) return false;
                if (!this.getWorld().getWorldBorder().contains(pos)) return false;
                if (!this.getWorld().isChunkLoaded(pos)) {
                    this.planeProbeHitUnloadedChunk = true;
                    return false;
                }
                BlockState state = this.getWorld().getBlockState(pos);
                if (state.isAir()) continue;
                VoxelShape shape = state.getCollisionShape((BlockView)this.getWorld(), pos);
                if (shape.isEmpty()) continue;
                for (Box shapeBox : shape.getBoundingBoxes()) {
                    if (probeBox.intersects(shapeBox.offset(pos))) return false;
                }
            }
        return true;
    }

    private boolean arePlaneBoundsInLoadedWorld(Box box) {
        int minX = MathHelper.floor(box.minX);
        int minY = MathHelper.floor(box.minY);
        int minZ = MathHelper.floor(box.minZ);
        int maxX = MathHelper.floor(box.maxX);
        int maxY = MathHelper.floor(box.maxY);
        int maxZ = MathHelper.floor(box.maxZ);
        if (minY < this.getWorld().getBottomY() || maxY >= this.getWorld().getTopYInclusive()) return false;
        BlockPos[] corners = new BlockPos[]{new BlockPos(minX, minY, minZ), new BlockPos(minX, minY, maxZ),
            new BlockPos(maxX, maxY, minZ), new BlockPos(maxX, maxY, maxZ)};
        for (BlockPos pos : corners) {
            if (!this.getWorld().getWorldBorder().contains(pos)) return false;
        }
        int minChunkX = minX >> 4;
        int maxChunkX = maxX >> 4;
        int minChunkZ = minZ >> 4;
        int maxChunkZ = maxZ >> 4;
        for (int chunkX = minChunkX; chunkX <= maxChunkX; ++chunkX) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; ++chunkZ) {
                if (!this.getWorld().isChunkLoaded(new BlockPos(chunkX << 4, minY, chunkZ << 4))) return false;
            }
        }
        return true;
    }

    private boolean hasTerrainBelow(double reach) {
        if (!this.isPlane()) return false;
        PlaneDefinition definition = this.structure.getPlaneDefinition();
        Vec3d[] points = new Vec3d[]{definition.centerOfMass(), definition.nose().blockCenter(),
            definition.leftWingTip().blockCenter(), definition.rightWingTip().blockCenter()};
        for (Vec3d point : points) {
            Vec3d worldPoint = this.transformStructurePoint(point);
            if (this.hasSupportAt(worldPoint.x, worldPoint.y, worldPoint.z, reach)) return true;
        }
        return false;
    }

    private void tickPhysics() {
        double grip;
        float damping;
        if (this.structure == null) {
            return;
        }
        boolean grounded = this.hasPhysicsGroundSupport();
        this.physicsGrounded = grounded;
        if (!grounded) {
            // A rotated vehicle uses a broad entity box. Vanilla may mark that box
            // grounded when only a distant corner overlaps terrain, so tire forces
            // must use the vehicle's own underside/wheel contacts instead.
            this.setOnGround(false);
        }
        boolean steeringInput = this.inputState.left != this.inputState.right;
        boolean wantsDrift = grounded && this.inputState.brake && steeringInput && Math.abs(this.speed) >= VehiclePhysics.DRIFT_MIN_SPEED;
        float targetDrift = wantsDrift ? MathHelper.clamp((Math.abs(this.speed) - VehiclePhysics.DRIFT_MIN_SPEED) / 0.32f, 0.35f, 1.0f) : 0.0f;
        this.driftAmount += (targetDrift - this.driftAmount) * (wantsDrift ? 0.34f : 0.18f);
        if (this.driftAmount < 0.01f) {
            this.driftAmount = 0.0f;
        }
        float curYaw = this.getYaw();
        boolean isMoving = Math.abs(this.speed) > 0.003f;
        float inertia = this.getMomentOfInertia();
        float maxSpd = this.getMaxSpeed();
        float baseAcc = this.getAcceleration();
        float revAcc = this.getReverseAcceleration();
        float maxRevSpd = this.getMaxReverseSpeed();
        float steerTorque = 0.0f;
        if (grounded && isMoving && (this.inputState.left || this.inputState.right)) {
            float dir;
            float speedFrac = Math.min(Math.abs(this.speed) / maxSpd, 1.0f);
            float effectiveTorque = VehiclePhysics.STEERING_TORQUE * (0.55f + 0.45f * speedFrac);
            if (this.offRoadBlockedTicks > 0) {
                effectiveTorque *= VehiclePhysics.OFFROAD_STEERING_BOOST;
            }
            if (wantsDrift) {
                effectiveTorque *= VehiclePhysics.DRIFT_STEERING_BOOST;
            }
            float f = dir = this.speed >= 0.0f ? 1.0f : -1.0f;
            if (this.inputState.left) {
                steerTorque -= effectiveTorque * dir;
            }
            if (this.inputState.right) {
                steerTorque += effectiveTorque * dir;
            }
        }
        float f = damping = wantsDrift ? 0.88f : (this.inputState.brake ? VehiclePhysics.BRAKE_ANGULAR_DAMPING : VehiclePhysics.ANGULAR_DAMPING);
        if (!grounded) {
            damping = VehiclePhysics.AIR_ANGULAR_DAMPING;
        }
        if (!isMoving) {
            damping = 0.4f;
        }
        float angularAcc = steerTorque / inertia;
        this.angularVelocity = (this.angularVelocity + angularAcc) * damping;
        float maxAngularVelocity = grounded
            ? VehiclePhysics.MAX_ANGULAR_VELOCITY
            : VehiclePhysics.MAX_AIR_ANGULAR_VELOCITY;
        this.angularVelocity = MathHelper.clamp(this.angularVelocity, -maxAngularVelocity, maxAngularVelocity);
        if (Math.abs(this.angularVelocity) < 0.01f) {
            this.angularVelocity = 0.0f;
        }
        curYaw += this.angularVelocity;
        curYaw = MathHelper.wrapDegrees((float)curYaw);
        float safeYaw = VehicleCollisionHandler.resolveRotationCollision(this, this.getYaw(), curYaw);
        this.setYaw(safeYaw);
        float rollDelta = this.speed / 0.65f * 57.295776f * 0.45f;
        this.wheelRollAngle += rollDelta;
        float targetSteer = 0.0f;
        if (this.inputState.left) {
            targetSteer -= 28.0f;
        }
        if (this.inputState.right) {
            targetSteer += 28.0f;
        }
        this.steeringAngle += (targetSteer - this.steeringAngle) * 0.35f;
        float speedRatio = Math.min(Math.abs(this.speed) / maxSpd, 1.0f);
        float dynamicAcc = baseAcc * Math.max(0.12f, 1.0f - 0.72f * speedRatio * speedRatio);
        float dynamicRevAcc = revAcc * Math.max(0.2f, 1.0f - 0.5f * (Math.abs(this.speed) / maxRevSpd));
        if (!this.hasDriver()) {
            this.inputState = VehicleInputState.EMPTY;
            this.speed = grounded ? (this.speed *= 0.965f) : (this.speed *= VehiclePhysics.AIR_DRAG);
        } else if (!grounded) {
            // With no tire contact, throttle and brakes cannot redirect the chassis.
            this.speed *= VehiclePhysics.AIR_DRAG;
        } else {
            if (this.inputState.brake) {
                float brakeAmount = wantsDrift
                    ? VehiclePhysics.DRIFT_BRAKE_BASE_DECELERATION + Math.abs(this.speed) * VehiclePhysics.DRIFT_BRAKE_SPEED_FACTOR
                    : VehiclePhysics.BRAKE_BASE_DECELERATION + Math.abs(this.speed) * VehiclePhysics.BRAKE_SPEED_FACTOR;
                if (Math.abs(this.speed) <= brakeAmount) {
                    this.speed = 0.0f;
                } else {
                    this.speed -= Math.signum(this.speed) * brakeAmount;
                }
            } else if (this.inputState.forward) {
                this.speed = Math.min(this.speed + dynamicAcc, maxSpd);
            } else if (this.inputState.backward) {
                this.speed = Math.max(this.speed - dynamicRevAcc, -maxRevSpd);
            } else {
                this.speed *= VehiclePhysics.FRICTION;
            }
        }
        if (grounded && Math.abs(this.speed) < 0.003f) {
            this.speed = 0.0f;
        }
        float yawRad = (float)Math.toRadians(safeYaw);
        double headingX = -Math.sin(yawRad);
        double headingZ = Math.cos(yawRad);
        double targetVelX = headingX * (double)this.speed;
        double targetVelZ = headingZ * (double)this.speed;
        if (!grounded) {
            this.motionX *= VehiclePhysics.AIR_DRAG;
            this.motionZ *= VehiclePhysics.AIR_DRAG;
        } else if (this.inputState.brake && !wantsDrift) {
            grip = 0.78;
            this.applyGroundTireForces(headingX, headingZ, targetVelX, targetVelZ, grip, 0.0f);
        } else if (Math.abs(this.speed) < 0.15f) {
            grip = 0.85;
            this.applyGroundTireForces(headingX, headingZ, targetVelX, targetVelZ, grip, 0.0f);
        } else {
            float speedFrac = Math.min(Math.abs(this.speed) / maxSpd, 1.0f);
            grip = 0.72 - 0.2 * (double)speedFrac;
            this.applyGroundTireForces(headingX, headingZ, targetVelX, targetVelZ, grip, wantsDrift ? Math.max(this.driftAmount, 0.6f) : this.driftAmount);
        }
        if (grounded && Math.abs(this.speed) < 0.003f) {
            this.motionX = 0.0;
            this.motionZ = 0.0;
        }
        boolean wasOnGroundBefore = grounded;
        if (grounded) {
            this.ticksSinceGroundLeft = 0;
            this.verticalVelocity = -0.06;
            if (Math.abs(this.speed) > 0.02f) {
                double pz;
                double signSpd = Math.signum(this.speed);
                double probeDist = Math.max(0.6, (double)Math.abs(this.speed) * 1.8);
                double px = this.getX() + headingX * probeDist * signSpd;
                double groundAheadY = this.probeGroundY(px, pz = this.getZ() + headingZ * probeDist * signSpd, this.getY() - 1.5, this.getY() + 1.25 + 0.1);
                double stepDeltaAhead = groundAheadY - this.getY();
                if (stepDeltaAhead > 0.05 && stepDeltaAhead <= 1.25) {
                    float slopeDrag = (float)(1.0 - stepDeltaAhead * (double)0.6f * 0.05);
                    this.speed *= Math.max(0.94f, slopeDrag);
                }
            }
        } else {
            ++this.ticksSinceGroundLeft;
            this.verticalVelocity = Math.max(this.verticalVelocity - (double)VehiclePhysics.GRAVITY, (double)-VehiclePhysics.MAX_FALL_SPEED);
        }
        Vec3d movement = new Vec3d(this.motionX + this.impactVelocityX, this.verticalVelocity, this.motionZ + this.impactVelocityZ);
        this.doMove(movement, wasOnGroundBefore);
        double impactDamping = this.physicsGrounded ? 0.82 : 0.96;
        this.impactVelocityX *= impactDamping;
        this.impactVelocityZ *= impactDamping;
        if (Math.abs(this.impactVelocityX) < 0.003) {
            this.impactVelocityX = 0.0;
        }
        if (Math.abs(this.impactVelocityZ) < 0.003) {
            this.impactVelocityZ = 0.0;
        }
        if (!this.getWorld().isClient()) {
            VehicleCollisionHandler.handleCollisions(this);
        }
        this.computeTilt();
        this.refreshBoundingBox();
    }

    private void applyGroundTireForces(double headingX, double headingZ, double targetVelX, double targetVelZ, double normalResponse, float driftBlend) {
        double rightX = headingZ;
        double rightZ = -headingX;
        double forwardVelocity = this.motionX * headingX + this.motionZ * headingZ;
        double lateralVelocity = this.motionX * rightX + this.motionZ * rightZ;
        double targetForward = targetVelX * headingX + targetVelZ * headingZ;
        double forwardResponse = normalResponse + (VehiclePhysics.DRIFT_FORWARD_RESPONSE - normalResponse) * (double)driftBlend;
        double normalLateralRetention = Math.max(0.08, 1.0 - normalResponse);
        double lateralRetention = normalLateralRetention
            + (VehiclePhysics.DRIFT_LATERAL_RETENTION - normalLateralRetention) * (double)driftBlend;
        forwardVelocity += (targetForward - forwardVelocity) * forwardResponse;
        lateralVelocity *= lateralRetention;
        this.motionX = headingX * forwardVelocity + rightX * lateralVelocity;
        this.motionZ = headingZ * forwardVelocity + rightZ * lateralVelocity;
    }

    private double probeGroundY(double wx, double wz, double minY, double maxY) {
        int topBY = (int)Math.floor(maxY);
        int botBY = (int)Math.floor(minY);
        for (int by = topBY; by >= botBY; --by) {
            double surfaceY;
            VoxelShape shape;
            BlockPos bp = BlockPos.ofFloored((double)wx, (double)by, (double)wz);
            BlockState state = this.getWorld().getBlockState(bp);
            if (state.isAir() || (shape = state.getCollisionShape((BlockView)this.getWorld(), bp)).isEmpty() || !((surfaceY = (double)bp.getY() + shape.getMax(Direction.Axis.Y)) <= maxY)) continue;
            return surfaceY;
        }
        return minY;
    }

    /**
     * Vanilla's on-ground flag belongs to the entity's axis-aligned bounding box.
     * That box becomes much larger than the actual chassis when a wide vehicle is
     * rotated, so one remote corner can otherwise keep the drivetrain active over
     * a cliff. Require support under real wheels (or sampled underside blocks for
     * wheel-less builds) before treating the vehicle as grounded.
     */
    private boolean hasPhysicsGroundSupport() {
        if (this.structure == null || !this.isOnGround() && !this.physicsGrounded) {
            return false;
        }
        // Preserve contact across a one-tick vanilla onGround flicker, but never
        // magnet a genuinely rising vehicle back onto the road.
        if (!this.isOnGround() && this.verticalVelocity > 0.08) {
            return false;
        }
        return this.hasSampledGroundSupport(VehiclePhysics.GROUND_SUPPORT_REACH);
    }

    private boolean hasSampledGroundSupport(double supportReach) {
        List<VehicleStructure.StoredBlock> contacts = this.structure.getContactBlocks();
        if (contacts.isEmpty()) {
            return false;
        }
        double undersideY = this.getY() + contacts.get(0).ry();
        float relativeYaw = this.getYaw() - this.structure.getInitialYaw();
        double yawRad = Math.toRadians(relativeYaw);
        double cosY = Math.cos(yawRad);
        double sinY = Math.sin(yawRad);
        List<VehicleStructure.WheelData> wheels = this.structure.getWheels();
        int supported = 0;
        int sampleCount;
        if (!wheels.isEmpty()) {
            sampleCount = Math.min(8, wheels.size());
            for (int sample = 0; sample < sampleCount; ++sample) {
                VehicleStructure.WheelData wheel = wheels.get((int)((long)sample * wheels.size() / sampleCount));
                double worldX = this.getX() + wheel.rx() * cosY - wheel.rz() * sinY;
                double worldZ = this.getZ() + wheel.rx() * sinY + wheel.rz() * cosY;
                if (this.hasSupportAt(worldX, undersideY, worldZ, supportReach)) {
                    // One real wheel contact is enough to retain partial road
                    // authority; a fully airborne build has no sampled contacts.
                    return true;
                }
            }
        }
        // Wheels can momentarily bridge the edge of a block on rough terrain.
        // Also sample the chassis underside so that a build resting or scraping
        // on the road does not lose all steering merely because both wheel-center
        // points happen to be over small gaps.
        sampleCount = Math.min(16, contacts.size());
        for (int sample = 0; sample < sampleCount; ++sample) {
            VehicleStructure.StoredBlock contact = contacts.get((int)((long)sample * contacts.size() / sampleCount));
            double worldX = this.getX() + contact.rx() * cosY - contact.rz() * sinY;
            double worldZ = this.getZ() + contact.rx() * sinY + contact.rz() * cosY;
            if (this.hasSupportAt(worldX, undersideY, worldZ, supportReach)) {
                ++supported;
            }
        }
        // Tiny two-wheel builds must remain usable on slabs and narrow roads;
        // larger footprints require two contacts so a single corner cannot make
        // the whole chassis behave as if every tire still has grip.
        int requiredSupport = sampleCount >= 4 ? 2 : 1;
        return supported >= requiredSupport;
    }

    private boolean hasSupportAt(double worldX, double undersideY, double worldZ, double supportReach) {
        return Double.isFinite(this.findSupportGapAt(worldX, undersideY, worldZ, supportReach));
    }

    private double findSupportGapAt(double worldX, double undersideY, double worldZ, double supportReach) {
        double highestAllowedSurface = undersideY + 0.16;
        double lowestAllowedSurface = undersideY - supportReach;
        int topBlockY = MathHelper.floor(highestAllowedSurface);
        int bottomBlockY = MathHelper.floor(lowestAllowedSurface);
        for (int by = topBlockY; by >= bottomBlockY; --by) {
            BlockPos checkPos = BlockPos.ofFloored(worldX, by, worldZ);
            if (!this.getWorld().isChunkLoaded(checkPos)
                || !this.getWorld().getWorldBorder().contains(checkPos)) return Double.POSITIVE_INFINITY;
            BlockState state = this.getWorld().getBlockState(checkPos);
            if (state.isAir()) {
                continue;
            }
            VoxelShape shape = state.getCollisionShape((BlockView)this.getWorld(), checkPos);
            if (shape.isEmpty()) {
                continue;
            }
            double localX = worldX - checkPos.getX();
            double localZ = worldZ - checkPos.getZ();
            if (localX < shape.getMin(Direction.Axis.X) - 0.06
                || localX > shape.getMax(Direction.Axis.X) + 0.06
                || localZ < shape.getMin(Direction.Axis.Z) - 0.06
                || localZ > shape.getMax(Direction.Axis.Z) + 0.06) {
                continue;
            }
            double surfaceY = checkPos.getY() + shape.getMax(Direction.Axis.Y);
            if (surfaceY >= lowestAllowedSurface && surfaceY <= highestAllowedSurface) {
                return Math.max(-0.16, undersideY - surfaceY);
            }
        }
        return Double.POSITIVE_INFINITY;
    }

    public float getStepHeight() {
        return 1.25f;
    }

    private void doMove(Vec3d movement, boolean wasOnGroundBefore) {
        double xBeforeMove = this.getX();
        double yBeforeMove = this.getY();
        double zBeforeMove = this.getZ();
        this.setVelocity(movement);
        if (wasOnGroundBefore && movement.x * movement.x + movement.z * movement.z > 1.0E-6) {
            this.moveWithTerrainCollider(movement);
        } else {
            this.move(MovementType.SELF, movement);
        }
        double requestedHorizontal = Math.sqrt(movement.x * movement.x + movement.z * movement.z);
        boolean supportedAfterMove = this.hasPhysicsGroundSupport();
        if (!supportedAfterMove) {
            this.setOnGround(false);
        }
        if (wasOnGroundBefore && !supportedAfterMove && movement.y <= 0.0) {
            boolean rampTakeoff = this.wasClimbing && this.lastStepUpSpeed > 0.035 && requestedHorizontal > 0.1;
            boolean roadContinuesBelow = this.hasSampledGroundSupport(VehiclePhysics.SUSPENSION_MAX_REACH + 0.08);
            if (rampTakeoff && !roadContinuesBelow) {
                double launchVelocity = Math.min(
                    VehiclePhysics.MAX_RAMP_LAUNCH_SPEED,
                    this.lastStepUpSpeed * (double)VehiclePhysics.RAMP_LAUNCH_TRANSFER
                        + requestedHorizontal * (double)VehiclePhysics.RAMP_LAUNCH_SPEED_BONUS
                );
                double currentRise = this.getY() - yBeforeMove;
                double requestedLift = Math.max(0.0, launchVelocity - currentRise);
                double beforeLift = this.getY();
                this.move(MovementType.SELF, new Vec3d(0.0, requestedLift, 0.0));
                double actualLift = this.getY() - beforeLift;
                this.verticalVelocity = actualLift >= requestedLift - 0.01 ? launchVelocity : 0.0;
                this.setOnGround(false);
                this.wasClimbing = false;
                this.lastStepUpSpeed = 0.0;
            } else {
                // A short suspension probe keeps slabs and shallow road seams smooth.
                // It is deliberately much shorter than a block so cliffs do not
                // magnetically hold the vehicle up or erase its fall velocity.
                double yBeforeStepDown = this.getY();
                this.move(MovementType.SELF, new Vec3d(0.0, -VehiclePhysics.SUSPENSION_MAX_REACH, 0.0));
                supportedAfterMove = this.hasPhysicsGroundSupport();
                if (supportedAfterMove) {
                    this.verticalVelocity = -0.06;
                } else {
                    this.setPosition(this.getX(), yBeforeStepDown, this.getZ());
                    this.setOnGround(false);
                    this.verticalVelocity = Math.min(movement.y, -0.06);
                }
            }
        }
        double totalYDelta = this.getY() - yBeforeMove;
        double actualX = this.getX() - xBeforeMove;
        double actualZ = this.getZ() - zBeforeMove;
        double actualHorizontal = Math.sqrt(actualX * actualX + actualZ * actualZ);
        double blockedHorizontal = Math.sqrt((movement.x - actualX) * (movement.x - actualX) + (movement.z - actualZ) * (movement.z - actualZ));
        double clipDetectionThreshold = wasOnGroundBefore ? 0.006 : 0.02;
        boolean hardHorizontalClip = requestedHorizontal > clipDetectionThreshold
            && (actualHorizontal < requestedHorizontal * 0.6 || blockedHorizontal > Math.max(0.04, requestedHorizontal * 0.22));
        if (hardHorizontalClip && wasOnGroundBefore
            && this.tryOffRoadStep(xBeforeMove, yBeforeMove, zBeforeMove, movement, actualHorizontal)) {
            totalYDelta = this.getY() - yBeforeMove;
            actualX = this.getX() - xBeforeMove;
            actualZ = this.getZ() - zBeforeMove;
            actualHorizontal = Math.sqrt(actualX * actualX + actualZ * actualZ);
            blockedHorizontal = Math.sqrt((movement.x - actualX) * (movement.x - actualX) + (movement.z - actualZ) * (movement.z - actualZ));
            hardHorizontalClip = requestedHorizontal > clipDetectionThreshold
                && (actualHorizontal < requestedHorizontal * 0.6 || blockedHorizontal > Math.max(0.04, requestedHorizontal * 0.22));
        }
        if (hardHorizontalClip) {
            float speedRetention = wasOnGroundBefore
                ? VehiclePhysics.OFFROAD_COLLISION_SPEED_RETENTION
                : 0.12f;
            this.speed *= speedRetention;
            this.motionX = actualX;
            this.motionZ = actualZ;
            this.impactVelocityX *= wasOnGroundBefore ? 0.45 : 0.15;
            this.impactVelocityZ *= wasOnGroundBefore ? 0.45 : 0.15;
            this.angularVelocity *= wasOnGroundBefore
                ? VehiclePhysics.OFFROAD_ANGULAR_RETENTION
                : 0.35f;
            if (wasOnGroundBefore) {
                this.markOffRoadBlocked();
            }
        }
        VehicleCollisionHandler.resolveBlockCollisions(this, movement);
        this.refreshBoundingBox();
        supportedAfterMove = this.hasPhysicsGroundSupport();
        this.physicsGrounded = supportedAfterMove;
        if (!supportedAfterMove) {
            this.setOnGround(false);
        }
        if (!wasOnGroundBefore && supportedAfterMove) {
            this.handleLandingImpact(Math.max(0.0, -movement.y));
        }
        if (wasOnGroundBefore && supportedAfterMove && totalYDelta > 0.025 && requestedHorizontal > 0.03) {
            this.wasClimbing = true;
            double climbVelocity = Math.min(totalYDelta, requestedHorizontal * 0.9 + 0.12);
            this.lastStepUpSpeed = Math.max(this.lastStepUpSpeed * 0.45, climbVelocity);
        } else if (supportedAfterMove) {
            this.lastStepUpSpeed *= 0.45;
            if (this.lastStepUpSpeed < 0.02) {
                this.lastStepUpSpeed = 0.0;
                this.wasClimbing = false;
            }
        }
        if (wasOnGroundBefore && supportedAfterMove && Math.abs(totalYDelta) > 0.04) {
            this.visualYOffset -= (float)totalYDelta;
            this.visualYOffset = MathHelper.clamp((float)this.visualYOffset, (float)-1.25f, (float)1.25f);
        }
        if (supportedAfterMove) {
            this.verticalVelocity = -0.06;
        }
    }

    /**
     * The axis-aligned box enclosing a rotated five-block car includes large empty
     * corners. On a hillside those corners collide with terrain the rendered car
     * never touches. Use a compact central footprint for grounded horizontal
     * traversal, restore the full box for vertical support/entity interaction, and
     * leave true wall handling to the structure-aware collision probes.
     */
    private void moveWithTerrainCollider(Vec3d movement) {
        double minPlanSize = Math.min(this.structure.getWidth(), this.structure.getLength());
        double colliderWidth = MathHelper.clamp(minPlanSize * 0.82, 0.76, 1.35);
        double halfWidth = colliderWidth * 0.5;
        double startY = this.getY();
        this.setBoundingBox(new Box(
            this.getX() - halfWidth,
            startY,
            this.getZ() - halfWidth,
            this.getX() + halfWidth,
            startY + Math.max(0.9, this.boxHeight),
            this.getZ() + halfWidth
        ));
        this.move(MovementType.SELF, new Vec3d(movement.x, 0.0, movement.z));
        this.refreshBoundingBox();
        this.move(MovementType.SELF, new Vec3d(0.0, movement.y, 0.0));
        this.refreshBoundingBox();
    }

    /**
     * Vanilla step-up can fail for long rotated vehicles because their broad AABB
     * touches several terrace blocks at once. Retry a clipped grounded movement
     * from above the climbable terrain, then settle back onto sampled road. Tall
     * walls still block the elevated retry, while one-block off-road ledges no
     * longer pin the chassis in place.
     */
    private boolean tryOffRoadStep(double startX, double startY, double startZ, Vec3d movement, double originalHorizontalProgress) {
        double savedX = this.getX();
        double savedY = this.getY();
        double savedZ = this.getZ();
        boolean savedOnGround = this.isOnGround();
        this.setPosition(startX, startY, startZ);
        this.refreshBoundingBox();
        this.setOnGround(true);
        this.move(MovementType.SELF, new Vec3d(0.0, VehiclePhysics.MAX_CLIMB_HEIGHT, 0.0));
        double lift = this.getY() - startY;
        if (lift < 0.45) {
            this.restoreMoveCandidate(savedX, savedY, savedZ, savedOnGround);
            return false;
        }
        this.move(MovementType.SELF, new Vec3d(movement.x, 0.0, movement.z));
        double candidateX = this.getX() - startX;
        double candidateZ = this.getZ() - startZ;
        double candidateProgress = Math.sqrt(candidateX * candidateX + candidateZ * candidateZ);
        double requiredImprovement = Math.max(0.004, Math.sqrt(movement.x * movement.x + movement.z * movement.z) * 0.18);
        if (candidateProgress < originalHorizontalProgress + requiredImprovement) {
            this.restoreMoveCandidate(savedX, savedY, savedZ, savedOnGround);
            return false;
        }
        this.move(MovementType.SELF, new Vec3d(0.0, -(lift + VehiclePhysics.SUSPENSION_MAX_REACH), 0.0));
        if (!this.isOnGround() || !this.hasSampledGroundSupport(VehiclePhysics.GROUND_SUPPORT_REACH + 0.12)) {
            this.restoreMoveCandidate(savedX, savedY, savedZ, savedOnGround);
            return false;
        }
        this.verticalVelocity = -0.06;
        return true;
    }

    private void restoreMoveCandidate(double x, double y, double z, boolean onGround) {
        this.setPosition(x, y, z);
        this.refreshBoundingBox();
        this.setOnGround(onGround);
    }

    private void handleLandingImpact(double fallSpeed) {
        if (fallSpeed < VehiclePhysics.MIN_LANDING_IMPACT_SPEED) {
            return;
        }
        float weightScale = MathHelper.clamp((float)Math.sqrt(this.getWeightFactor()), 0.85f, 1.3f);
        float weightedSpeed = (float)fallSpeed * weightScale;
        float impact = MathHelper.clamp(
            (weightedSpeed - VehiclePhysics.MIN_LANDING_IMPACT_SPEED)
                / (VehiclePhysics.FULL_LANDING_IMPACT_SPEED - VehiclePhysics.MIN_LANDING_IMPACT_SPEED),
            0.0f,
            1.0f
        );
        float compression = 0.12f + impact * 0.58f;
        this.visualYOffset = MathHelper.clamp(this.visualYOffset - compression, -0.9f, 1.25f);
        this.speed *= 1.0f - impact * 0.18f;
        this.motionX *= 1.0 - (double)impact * 0.1;
        this.motionZ *= 1.0 - (double)impact * 0.1;
        this.angularVelocity *= 1.0f - impact * 0.3f;
        this.vehiclePitch *= 0.62f;
        this.vehicleRoll *= 0.72f;
        if (this.landingImpactCooldown > 0) {
            return;
        }
        float volume = 0.35f + impact * 0.65f;
        float pitch = 1.12f - impact * 0.32f;
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.CAR_IMPACT, SoundCategory.BLOCKS, volume, pitch);
        } else {
            this.getWorld().playSound(this.getX(), this.getY(), this.getZ(), ModSounds.CAR_IMPACT, SoundCategory.BLOCKS, volume, pitch, false);
        }
        this.landingImpactCooldown = 8;
    }

    private void computeTilt() {
        boolean isStopped;
        if (this.structure == null) {
            return;
        }
        if ((this.age & 1) != 0) {
            return;
        }
        List<VehicleStructure.StoredBlock> contacts = this.structure.getContactBlocks();
        if (contacts.isEmpty()) {
            return;
        }
        float relativeYaw = this.getYaw() - this.structure.getInitialYaw();
        float yawRad = (float)Math.toRadians(relativeYaw);
        float cosY = (float)Math.cos(yawRad);
        float sinY = (float)Math.sin(yawRad);
        double minRy = contacts.get(0).ry();
        int n = 0;
        double sumF = 0.0;
        double sumR = 0.0;
        double sumH = 0.0;
        double sumFF = 0.0;
        double sumRR = 0.0;
        double sumFR = 0.0;
        double sumFH = 0.0;
        double sumRH = 0.0;
        int sampleCount = Math.min(16, contacts.size());
        for (int sample = 0; sample < sampleCount; ++sample) {
            VehicleStructure.StoredBlock cb = contacts.get((int)((long)sample * contacts.size() / sampleCount));
            double worldX = this.getX() + (cb.rx() * (double)cosY - cb.rz() * (double)sinY);
            double worldZ = this.getZ() + (cb.rx() * (double)sinY + cb.rz() * (double)cosY);
            int topBlockY = (int)Math.floor(this.getY() + minRy + 1.25);
            int bottomBlockY = (int)Math.floor(this.getY() + minRy - 2.5);
            double groundY = this.getY() + minRy;
            for (int by = topBlockY; by >= bottomBlockY; --by) {
                double surfaceY;
                VoxelShape shape;
                BlockPos checkPos = BlockPos.ofFloored((double)worldX, (double)by, (double)worldZ);
                BlockState state = this.getWorld().getBlockState(checkPos);
                if (state.isAir() || (shape = state.getCollisionShape((BlockView)this.getWorld(), checkPos)).isEmpty() || !((surfaceY = (double)checkPos.getY() + shape.getMax(Direction.Axis.Y)) <= this.getY() + minRy + 1.25 + 0.15)) continue;
                groundY = surfaceY;
                break;
            }
            double localF = cb.rz();
            double localR = cb.rx();
            sumF += localF;
            sumR += localR;
            sumH += groundY;
            sumFF += localF * localF;
            sumRR += localR * localR;
            sumFR += localF * localR;
            sumFH += localF * groundY;
            sumRH += localR * groundY;
            ++n;
        }
        float targetPitch = 0.0f;
        float targetRoll = 0.0f;
        if (n >= 2) {
            double meanF = sumF / (double)n;
            double meanR = sumR / (double)n;
            double meanH = sumH / (double)n;
            double cFF = sumFF - (double)n * meanF * meanF;
            double cRR = sumRR - (double)n * meanR * meanR;
            double cFR = sumFR - (double)n * meanF * meanR;
            double cFH = sumFH - (double)n * meanF * meanH;
            double cRH = sumRH - (double)n * meanR * meanH;
            double det = cFF * cRR - cFR * cFR;
            if (Math.abs(det) > 1.0E-5) {
                double a = (cRR * cFH - cFR * cRH) / det;
                double b = (cFF * cRH - cFR * cFH) / det;
                targetPitch = (float)Math.toDegrees(Math.atan(a));
                targetRoll = (float)Math.toDegrees(Math.atan(b));
            } else {
                if (cFF > 1.0E-4) {
                    double a = cFH / cFF;
                    targetPitch = (float)Math.toDegrees(Math.atan(a));
                }
                if (cRR > 1.0E-4) {
                    double b = cRH / cRR;
                    targetRoll = (float)Math.toDegrees(Math.atan(b));
                }
            }
        }
        boolean bl = isStopped = Math.abs(this.speed) < 0.003f;
        if (isStopped) {
            targetPitch = this.vehiclePitch;
            targetRoll = this.vehicleRoll;
        } else {
            if (Math.abs(targetPitch - this.vehiclePitch) < 0.4f) {
                targetPitch = this.vehiclePitch;
            }
            if (Math.abs(targetRoll - this.vehicleRoll) < 0.4f) {
                targetRoll = this.vehicleRoll;
            }
            if (Math.abs(targetPitch) < 0.25f) {
                targetPitch = 0.0f;
            }
            if (Math.abs(targetRoll) < 0.25f) {
                targetRoll = 0.0f;
            }
        }
        if (this.physicsGrounded && Math.abs(this.speed) > 0.05f) {
            float yawRadTilt = (float)Math.toRadians(this.getYaw());
            double rightX = Math.cos(yawRadTilt);
            double rightZ = Math.sin(yawRadTilt);
            double lateralSlip = this.motionX * rightX + this.motionZ * rightZ;
            targetRoll -= (float)(lateralSlip * 20.0);
        }
        if (this.physicsGrounded && this.hasDriver()) {
            if (this.inputState.forward && this.speed > 0.05f && this.speed < this.getMaxSpeed() * 0.75f) {
                targetPitch += 1.5f;
            } else if (this.inputState.brake && Math.abs(this.speed) > 0.1f) {
                targetPitch -= 2.5f;
            }
        }
        if (!this.physicsGrounded) {
            float fwdSpd = Math.max(0.12f, Math.abs(this.speed));
            float flightPitch = -((float)Math.toDegrees(Math.atan2(this.verticalVelocity, fwdSpd)));
            targetPitch = MathHelper.clamp((float)flightPitch, (float)-26.0f, (float)26.0f);
            targetRoll = this.vehicleRoll * 0.9f;
        }
        targetPitch = MathHelper.clamp((float)targetPitch, (float)-20.0f, (float)20.0f);
        targetRoll = MathHelper.clamp((float)targetRoll, (float)-20.0f, (float)20.0f);
        this.vehiclePitch += (targetPitch - this.vehiclePitch) * 0.18f;
        this.vehicleRoll += (targetRoll - this.vehicleRoll) * 0.18f;
        if (Math.abs(this.vehiclePitch) < 0.05f) {
            this.vehiclePitch = 0.0f;
        }
        if (Math.abs(this.vehicleRoll) < 0.05f) {
            this.vehicleRoll = 0.0f;
        }
    }

    public Vec3d getPassengerRidingPos(Entity passenger) {
        VehicleStructure struct = this.getStructure();
        if (struct == null) {
            return super.getPassengerRidingPos(passenger);
        }
        List<SeatData> seats = struct.getSeats();
        SeatData seat = this.getSeatFor(passenger, seats);
        if (this.isPlane()) {
            PlaneDefinition definition = struct.getPlaneDefinition();
            Vec3d localSeat = new Vec3d(seat.rx, seat.ry, seat.rz);
            Vec3d transformed = AircraftOrientation.transformAroundPivot(this.getRelativeAircraftOrientation(), localSeat, definition.centerOfMass());
            return this.getPos().add(transformed).add(0.0, this.visualYOffset, 0.0);
        }
        float relativeYaw = this.getYaw() - struct.getInitialYaw();
        float relativeYawRad = (float)Math.toRadians(relativeYaw);
        float pitchRad = (float)Math.toRadians(this.vehiclePitch);
        float rollRad = (float)Math.toRadians(this.vehicleRoll);
        Vec3d basePos = new Vec3d(this.getX(), this.getY() + (double)this.visualYOffset, this.getZ());
        return seat.toWorldPos(basePos, relativeYawRad, pitchRad, rollRad);
    }

    protected void updatePassengerPosition(Entity passenger, Entity.PositionUpdater positionUpdater) {
        if (!this.hasPassenger(passenger)) {
            return;
        }
        Vec3d worldSeat = this.getPassengerRidingPos(passenger);
        positionUpdater.accept(passenger, worldSeat.x, worldSeat.y, worldSeat.z);
        this.clampPassengerYaw(passenger);
    }

    protected void clampPassengerYaw(Entity passenger) {
        float deltaYaw;
        float clamped;
        float adjustment;
        float seatYaw = this.getPassengerSeatYaw(passenger);
        if (passenger instanceof LivingEntity) {
            LivingEntity living = (LivingEntity)passenger;
            living.bodyYaw = seatYaw;
        }
        if (Math.abs(adjustment = (clamped = MathHelper.clamp((float)(deltaYaw = MathHelper.wrapDegrees((float)(passenger.getYaw() - seatYaw))), (float)-105.0f, (float)105.0f)) - deltaYaw) > 0.001f) {
            passenger.setYaw(passenger.getYaw() + adjustment);
            passenger.setHeadYaw(passenger.getYaw());
        }
    }

    private SeatData getSeatFor(Entity passenger, List<SeatData> seats) {
        if (seats.isEmpty()) {
            return new SeatData(0.0, 0.1, 0.0, false);
        }
        Integer idx = this.passengerSeatMap.get(passenger.getUuid());
        if (idx == null) {
            int order = this.getPassengerList().indexOf(passenger);
            idx = order >= 0 && order < seats.size() ? Integer.valueOf(order) : Integer.valueOf(0);
            this.passengerSeatMap.put(passenger.getUuid(), idx);
        }
        if (idx >= 0 && idx < seats.size()) {
            return seats.get(idx);
        }
        return seats.get(0);
    }

    protected void addPassenger(Entity passenger) {
        super.addPassenger(passenger);
        this.assignSeat(passenger);
        float seatYaw = this.getPassengerSeatYaw(passenger);
        passenger.setYaw(seatYaw);
        passenger.setHeadYaw(seatYaw);
        passenger.setBodyYaw(seatYaw);
    }

    private float getPassengerSeatYaw(Entity passenger) {
        if (this.structure == null) return this.getYaw();
        SeatData seat = this.getSeatFor(passenger, this.structure.getSeats());
        return this.getYaw() + MathHelper.wrapDegrees(seat.yawOffset - this.structure.getInitialYaw());
    }

    protected void removePassenger(Entity passenger) {
        boolean wasControllingPassenger = this.getControllingPassenger() == passenger;
        super.removePassenger(passenger);
        this.passengerSeatMap.remove(passenger.getUuid());
        if (wasControllingPassenger) {
            this.inputState = VehicleInputState.EMPTY;
            if (passenger.getUuid().equals(this.planeInputDriver)) {
                this.planeInputDriver = null;
                this.lastPlaneInputSequence = -1;
            }
        }
        double ejectR = this.structure != null ? (double)Math.max(this.structure.getWidth(), this.structure.getLength()) / 2.0 + 1.2 : 2.0;
        float yawRad = (float)Math.toRadians(this.getYaw());
        double[][] candidateOffsets = new double[][]{{-Math.cos(yawRad) * ejectR, 0.0, -Math.sin(yawRad) * ejectR}, {Math.cos(yawRad) * ejectR, 0.0, Math.sin(yawRad) * ejectR}, {Math.sin(yawRad) * ejectR, 0.0, -Math.cos(yawRad) * ejectR}, {0.0, 1.2, 0.0}};
        Vec3d safePos = null;
        for (double[] off : candidateOffsets) {
            Vec3d check = this.getPos().add(off[0], off[1], off[2]);
            BlockPos bp = BlockPos.ofFloored((Position)check);
            if (!this.getWorld().getBlockState(bp).isAir() || !this.getWorld().getBlockState(bp.up()).isAir()) continue;
            safePos = check;
            break;
        }
        if (safePos != null) {
            passenger.setPosition(safePos.x, safePos.y, safePos.z);
        } else {
            passenger.setPosition(this.getX(), this.getY() + 0.5, this.getZ());
        }
    }

    private void assignSeat(Entity passenger) {
        VehicleStructure struct = this.getStructure();
        if (struct == null) {
            return;
        }
        List<SeatData> seats = struct.getSeats();
        for (int i = 0; i < seats.size(); ++i) {
            if (this.passengerSeatMap.containsValue(i)) continue;
            this.passengerSeatMap.put(passenger.getUuid(), i);
            return;
        }
        this.passengerSeatMap.put(passenger.getUuid(), 0);
    }

    public void mountAsDriver(Entity passenger) {
        VehicleStructure struct = this.getStructure();
        if (struct == null) {
            return;
        }
        List<SeatData> seats = struct.getSeats();
        for (int i = 0; i < seats.size(); ++i) {
            if (!seats.get((int)i).isDriver) continue;
            this.passengerSeatMap.put(passenger.getUuid(), i);
            return;
        }
    }

    public boolean hasDriver() {
        VehicleStructure struct = this.getStructure();
        if (struct == null) {
            return false;
        }
        List<SeatData> seats = struct.getSeats();
        for (Entity p : this.getPassengerList()) {
            SeatData seat = this.getSeatFor(p, seats);
            if (!seat.isDriver) continue;
            return true;
        }
        return false;
    }

    public boolean canAddPassenger(Entity passenger) {
        VehicleStructure struct = this.getStructure();
        if (struct == null) {
            return false;
        }
        return this.getPassengerList().size() < struct.getSeats().size();
    }

    private void syncToClients() {
        World world = this.getWorld();
        if (!(world instanceof ServerWorld)) {
            return;
        }
        ServerWorld sw = (ServerWorld)world;
        int interval = this.isPlane() ? (this.hasDriver() ? 2 : 4) : (Math.abs(this.speed) > 0.003f || this.hasDriver() ? 4 : 10);
        if (this.age % interval != 0) {
            return;
        }
        HashSet<ServerPlayerEntity> recipients = new HashSet<ServerPlayerEntity>(PlayerLookup.tracking(this));
        for (Entity passenger : this.getPassengerList()) {
            if (!(passenger instanceof ServerPlayerEntity)) continue;
            ServerPlayerEntity driverPlayer = (ServerPlayerEntity)passenger;
            recipients.add(driverPlayer);
        }
        if (this.isPlane()) {
            PlaneSyncPayload pkt = new PlaneSyncPayload(this.getId(), this.lastPlaneInputSequence,
                this.getX(), this.getY(), this.getZ(),
                (float)this.planeVelocity.x, (float)this.planeVelocity.y, (float)this.planeVelocity.z,
                this.aircraftOrientation.x, this.aircraftOrientation.y, this.aircraftOrientation.z, this.aircraftOrientation.w,
                this.throttle, this.engineRpm, this.planePitchRate, this.planeRollRate, this.planeYawRate,
                this.stallAmount, this.angleOfAttack, this.planeFlightState.ordinal(), this.propellerAngle);
            for (ServerPlayerEntity player : recipients) ServerPlayNetworking.send(player, pkt);
        } else {
            VehicleSyncPayload pkt = new VehicleSyncPayload(this.getId(), this.speed, this.vehiclePitch, this.vehicleRoll, this.angularVelocity);
            for (ServerPlayerEntity player : recipients) {
                ServerPlayNetworking.send((ServerPlayerEntity)player, (CustomPayload)pkt);
            }
        }
    }

    public void applyPlaneServerTelemetry(PlaneSyncPayload payload) {
        if (!this.isPlane()) return;
        Quaternionf target = new Quaternionf(payload.qx(), payload.qy(), payload.qz(), payload.qw());
        target = AircraftOrientation.sanitize(target, this.getYaw());
        Vec3d targetPosition = new Vec3d(payload.x(), payload.y(), payload.z());
        Vec3d targetVelocity = new Vec3d(payload.velocityX(), payload.velocityY(), payload.velocityZ());
        if (this.isDrivenByLocalPlayer()) {
            int predictionTicks = payload.sequence() >= 0 && this.lastLocalPlaneInputSequence >= payload.sequence()
                ? Math.min(6, this.lastLocalPlaneInputSequence - payload.sequence()) : 0;
            if (predictionTicks > 0) {
                targetPosition = targetPosition.add(targetVelocity.multiply(predictionTicks));
                for (int i = 0; i < predictionTicks; ++i) {
                    target.rotateX((float)Math.toRadians(-payload.pitchRate()));
                    target.rotateY((float)Math.toRadians(-payload.yawRate()));
                    target.rotateZ((float)Math.toRadians(payload.rollRate()));
                }
                target.normalize();
            }
            double positionErrorSq = this.getPos().squaredDistanceTo(targetPosition);
            if (positionErrorSq > 100.0) {
                this.setPosition(targetPosition);
                this.prevX = targetPosition.x;
                this.prevY = targetPosition.y;
                this.prevZ = targetPosition.z;
            } else if (positionErrorSq > 0.01) {
                this.setPosition(this.getPos().lerp(targetPosition, positionErrorSq > 4.0 ? 0.16 : 0.055));
            }
            float orientationDot = Math.abs(this.aircraftOrientation.dot(target));
            if (orientationDot < 0.99995f) {
                this.aircraftOrientation.slerp(target, orientationDot < 0.92f ? 0.25f : 0.08f).normalize();
            }
            this.planeVelocity = this.planeVelocity.lerp(targetVelocity, positionErrorSq > 0.64 ? 0.18 : 0.07);
            this.throttle += (payload.throttle() - this.throttle) * 0.12f;
            this.engineRpm += (payload.engineRpm() - this.engineRpm) * 0.12f;
            this.planePitchRate += (payload.pitchRate() - this.planePitchRate) * 0.10f;
            this.planeRollRate += (payload.rollRate() - this.planeRollRate) * 0.10f;
            this.planeYawRate += (payload.yawRate() - this.planeYawRate) * 0.10f;
            this.stallAmount += (payload.stallAmount() - this.stallAmount) * 0.16f;
            this.angleOfAttack += (payload.angleOfAttack() - this.angleOfAttack) * 0.16f;
        } else {
            double positionErrorSq = this.getPos().squaredDistanceTo(targetPosition);
            if (positionErrorSq > 144.0) {
                this.setPosition(targetPosition);
                this.clientInterpSteps = 0;
            } else {
                this.clientTargetX = payload.x();
                this.clientTargetY = payload.y();
                this.clientTargetZ = payload.z();
                this.clientTargetYaw = AircraftOrientation.yawDegrees(target);
                this.clientInterpSteps = 3;
            }
            this.clientTargetAircraftOrientation = target;
            this.clientPlaneInterpSteps = 3;
            this.planeVelocity = targetVelocity;
            this.throttle = payload.throttle();
            this.engineRpm = payload.engineRpm();
            this.planePitchRate = payload.pitchRate();
            this.planeRollRate = payload.rollRate();
            this.planeYawRate = payload.yawRate();
            this.stallAmount = payload.stallAmount();
            this.angleOfAttack = payload.angleOfAttack();
        }
        this.speed = (float)this.planeVelocity.length();
        this.planeFlightState = PlaneFlightState.byOrdinal(payload.flightState());
        if (this.planeFlightState == PlaneFlightState.CRASHED) this.planeImpactStateTicks = Math.max(this.planeImpactStateTicks, 20);
        else if (this.planeFlightState == PlaneFlightState.HARD_LANDING) this.planeImpactStateTicks = Math.max(this.planeImpactStateTicks, 8);
        float angleError = MathHelper.wrapDegrees(payload.propellerAngle() - this.propellerAngle);
        this.propellerAngle = MathHelper.wrapDegrees(this.propellerAngle + angleError * 0.35f);
        this.setYaw(AircraftOrientation.yawDegrees(this.aircraftOrientation));
        this.vehiclePitch = AircraftOrientation.pitchDegrees(this.aircraftOrientation);
        this.vehicleRoll = AircraftOrientation.rollDegrees(this.aircraftOrientation);
        this.setVelocity(this.planeVelocity);
        this.refreshBoundingBox();
    }

    public void applyServerTelemetry(float spd, float pitch, float roll, float omega) {
        if (this.isDrivenByLocalPlayer()) {
            return;
        }
        this.clientTargetPitch = pitch;
        this.clientTargetRoll = roll;
        this.angularVelocity = omega;
        this.speed = spd;
        this.clientVisualInterpSteps = 4;
    }

    public boolean damage(ServerWorld world, DamageSource source, float amount) {
        return false;
    }

    public void dismantleBackToBlocks(ServerWorld world, PlayerEntity player) {
        this.dismantleBackToBlocks(world, player, false);
    }

    public void dismantleBackToBlocks(ServerWorld world, PlayerEntity player, boolean force) {
        if (!force && this.isPlane() && (!this.physicsGrounded || this.speed > 0.08f
            || Math.abs(this.vehiclePitch) > 3.0f || Math.abs(this.vehicleRoll) > 3.0f)) {
            if (player != null) player.sendMessage(Text.literal("\u00a7cLand, stop, and level the plane before dismantling it."), true);
            return;
        }
        float snappedYaw = this.getYaw();
        if (this.structure != null) {
            float relative = MathHelper.wrapDegrees(this.getYaw() - this.structure.getInitialYaw());
            snappedYaw = MathHelper.wrapDegrees(this.structure.getInitialYaw() + Math.round(relative / 90.0f) * 90.0f);
            if (!VehicleActivator.canPlaceAt(world, this.structure, this.getPos(), snappedYaw)) {
                if (player != null) player.sendMessage(Text.literal("\u00a7cCannot dismantle: the aligned restoration area is blocked."), true);
                return;
            }
        }
        this.removeAllPassengers();
        if (this.structure != null) {
            VehicleActivator.deactivate(world, this.structure, this.getPos(), snappedYaw);
        }
        this.discard();
        if (player != null) {
            player.sendMessage((Text)Text.literal("\u00a7aVehicle dismantled back into blocks!"), true);
        }
    }

    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(STRUCTURE_NBT, new NbtCompound());
    }

    public boolean canHit() {
        return !this.isRemoved();
    }

    public boolean isCollidable() {
        return false;
    }

    public boolean isPushable() {
        return false;
    }

    public void pushAwayFrom(Entity entity) {
    }

    public ActionResult interact(PlayerEntity player, Hand hand) {
        if (this.getWorld().isClient()) {
            return ActionResult.SUCCESS;
        }
        if (!this.hasPassenger(player)) {
            player.startRiding(this);
        }
        return ActionResult.SUCCESS;
    }

    protected void readCustomDataFromNbt(NbtCompound nbt) {
        if (nbt.contains("structure")) {
            this.setStructure(VehicleStructure.fromNbt(nbt.getCompound("structure")));
        }
        float savedYaw = nbt.getFloat("vehicleYaw");
        this.speed = nbt.getFloat("speed");
        this.vehiclePitch = nbt.getFloat("vehiclePitch");
        this.vehicleRoll = nbt.getFloat("vehicleRoll");
        this.setYaw(savedYaw);
        if (this.isPlane()) {
            if (nbt.contains("aircraftQx")) {
                this.aircraftOrientation = AircraftOrientation.sanitize(new Quaternionf(nbt.getFloat("aircraftQx"),
                    nbt.getFloat("aircraftQy"), nbt.getFloat("aircraftQz"), nbt.getFloat("aircraftQw")), savedYaw);
            } else {
                this.aircraftOrientation = AircraftOrientation.fromYaw(savedYaw);
            }
            this.prevAircraftOrientation.set(this.aircraftOrientation);
            this.planeVelocity = new Vec3d(nbt.getDouble("planeVelocityX"), nbt.getDouble("planeVelocityY"), nbt.getDouble("planeVelocityZ"));
            this.throttle = MathHelper.clamp(nbt.getFloat("planeThrottle"), 0.0f, 1.0f);
            this.engineRpm = MathHelper.clamp(nbt.getFloat("planeRpm"), 0.0f, 1.0f);
            this.planePitchRate = nbt.getFloat("planePitchRate");
            this.planeRollRate = nbt.getFloat("planeRollRate");
            this.planeYawRate = nbt.getFloat("planeYawRate");
            this.stallAmount = MathHelper.clamp(nbt.getFloat("planeStall"), 0.0f, 1.0f);
            this.propellerAngle = nbt.getFloat("propellerAngle");
            this.planeFlightState = PlaneFlightState.byOrdinal(nbt.getInt("planeFlightState"));
        }
        this.refreshBoundingBox();
    }

    protected void writeCustomDataToNbt(NbtCompound nbt) {
        if (this.structure != null) {
            nbt.put("structure", (NbtElement)this.structure.toNbt());
        }
        nbt.putFloat("vehicleYaw", this.getYaw());
        nbt.putFloat("speed", this.speed);
        nbt.putFloat("vehiclePitch", this.vehiclePitch);
        nbt.putFloat("vehicleRoll", this.vehicleRoll);
        if (this.isPlane()) {
            nbt.putFloat("aircraftQx", this.aircraftOrientation.x);
            nbt.putFloat("aircraftQy", this.aircraftOrientation.y);
            nbt.putFloat("aircraftQz", this.aircraftOrientation.z);
            nbt.putFloat("aircraftQw", this.aircraftOrientation.w);
            nbt.putDouble("planeVelocityX", this.planeVelocity.x);
            nbt.putDouble("planeVelocityY", this.planeVelocity.y);
            nbt.putDouble("planeVelocityZ", this.planeVelocity.z);
            nbt.putFloat("planeThrottle", this.throttle);
            nbt.putFloat("planeRpm", this.engineRpm);
            nbt.putFloat("planePitchRate", this.planePitchRate);
            nbt.putFloat("planeRollRate", this.planeRollRate);
            nbt.putFloat("planeYawRate", this.planeYawRate);
            nbt.putFloat("planeStall", this.stallAmount);
            nbt.putFloat("propellerAngle", this.propellerAngle);
            nbt.putInt("planeFlightState", this.planeFlightState.ordinal());
        }
    }
}
