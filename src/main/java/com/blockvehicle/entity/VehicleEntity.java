package com.blockvehicle.entity;

import com.blockvehicle.ModEntities;
import com.blockvehicle.network.VehicleSyncPayload;
import com.blockvehicle.vehicle.SeatData;
import com.blockvehicle.vehicle.VehicleActivator;
import com.blockvehicle.vehicle.VehicleCollisionHandler;
import com.blockvehicle.vehicle.VehicleInputState;
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
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
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
    private final Map<UUID, Integer> passengerSeatMap = new HashMap<UUID, Integer>();
    public double clientTargetX;
    public double clientTargetY;
    public double clientTargetZ;
    public float clientTargetYaw;
    public float clientTargetPitch;
    public float clientTargetRoll;
    public int clientInterpSteps = 0;
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

    public void setAngularVelocity(float omega) {
        this.angularVelocity = omega;
    }

    public void addAngularVelocity(float deltaOmega) {
        this.angularVelocity = MathHelper.clamp((float)(this.angularVelocity + deltaOmega), (float)-10.5f, (float)10.5f);
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
            float size = Math.max(this.structure.getWidth(), this.structure.getLength());
            return EntityDimensions.changing((float)size, (float)this.structure.getHeight());
        }
        return super.getDimensions(pose);
    }

    private void updateBoundingBoxDimensions() {
        if (this.structure == null) {
            return;
        }
        this.boxHeight = this.structure.getHeight();
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
        LivingEntity living;
        if (this.getPassengerList().isEmpty()) {
            return null;
        }
        Entity first = this.getPassengerList().get(0);
        return first instanceof LivingEntity ? (living = (LivingEntity)first) : null;
    }

    public boolean isDrivenByLocalPlayer() {
        PlayerEntity player;
        LivingEntity livingEntity;
        return this.getWorld().isClient() && (livingEntity = this.getControllingPassenger()) instanceof PlayerEntity && (player = (PlayerEntity)livingEntity).isMainPlayer();
    }

    public void applyClientDriverUpdate(double x, double y, double z, float yaw, float spd, float pitch, float roll) {
        this.prevX = this.getX();
        this.prevY = this.getY();
        this.prevZ = this.getZ();
        this.prevYaw = this.getYaw();
        this.prevVehiclePitch = this.vehiclePitch;
        this.prevVehicleRoll = this.vehicleRoll;
        this.setPos(x, y, z);
        this.setYaw(yaw);
        this.speed = spd;
        this.vehiclePitch = pitch;
        this.vehicleRoll = roll;
        float yawRad = (float)Math.toRadians(yaw);
        this.setVelocity(-Math.sin(yawRad) * (double)spd, 0.0, Math.cos(yawRad) * (double)spd);
        this.velocityModified = true;
        this.refreshBoundingBox();
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
        this.visualYOffset += (0.0f - this.visualYOffset) * 0.28f;
        if (Math.abs(this.visualYOffset) < 0.002f) {
            this.visualYOffset = 0.0f;
        }
        super.tick();
        if (this.getWorld().isClient()) {
            if (this.isDrivenByLocalPlayer()) {
                this.tickPhysics();
            } else {
                this.tickClientInterp();
                VehicleCollisionHandler.handleCollisions(this);
            }
            return;
        }
        if (!this.hasDriver()) {
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
            this.vehiclePitch += (this.clientTargetPitch - this.vehiclePitch) / (float)this.clientInterpSteps;
            this.vehicleRoll += (this.clientTargetRoll - this.vehicleRoll) / (float)this.clientInterpSteps;
            --this.clientInterpSteps;
            this.setPosition(nx, ny, nz);
            this.setYaw(nyw);
        }
        float rollDelta = this.speed / 0.65f * 57.295776f * 0.45f;
        this.wheelRollAngle += rollDelta;
        this.steeringAngle += (0.0f - this.steeringAngle) * 0.2f;
        this.angularVelocity += (0.0f - this.angularVelocity) * 0.25f;
        this.refreshBoundingBox();
    }

    private void tickPhysics() {
        double grip;
        float damping;
        if (this.structure == null) {
            return;
        }
        float curYaw = this.getYaw();
        boolean isMoving = Math.abs(this.speed) > 0.003f;
        float inertia = this.getMomentOfInertia();
        float maxSpd = this.getMaxSpeed();
        float baseAcc = this.getAcceleration();
        float revAcc = this.getReverseAcceleration();
        float maxRevSpd = this.getMaxReverseSpeed();
        float steerTorque = 0.0f;
        if (isMoving && (this.inputState.left || this.inputState.right)) {
            float dir;
            float speedFrac = Math.min(Math.abs(this.speed) / maxSpd, 1.0f);
            float effectiveTorque = 2.4f * (0.55f + 0.45f * speedFrac);
            float f = dir = this.speed >= 0.0f ? 1.0f : -1.0f;
            if (this.inputState.left) {
                steerTorque -= effectiveTorque * dir;
            }
            if (this.inputState.right) {
                steerTorque += effectiveTorque * dir;
            }
            if (!this.inputState.forward && !this.inputState.backward) {
                this.speed *= 0.99f;
            }
        }
        float f = damping = this.inputState.brake ? 0.92f : 0.74f;
        if (!isMoving) {
            damping = 0.4f;
        }
        float angularAcc = steerTorque / inertia;
        this.angularVelocity = (this.angularVelocity + angularAcc) * damping;
        this.angularVelocity = MathHelper.clamp((float)this.angularVelocity, (float)-10.5f, (float)10.5f);
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
            this.speed = this.isOnGround() ? (this.speed *= 0.92f) : (this.speed *= 0.995f);
        } else {
            this.speed = this.inputState.brake ? (this.speed *= 0.86f) : (this.inputState.forward ? Math.min(this.speed + dynamicAcc, maxSpd) : (this.inputState.backward ? Math.max(this.speed - dynamicRevAcc, -maxRevSpd) : (this.speed *= this.isOnGround() ? 0.975f : 0.995f)));
        }
        if (this.isOnGround() && Math.abs(this.speed) < 0.003f) {
            this.speed = 0.0f;
        }
        float yawRad = (float)Math.toRadians(safeYaw);
        double headingX = -Math.sin(yawRad);
        double headingZ = Math.cos(yawRad);
        double targetVelX = headingX * (double)this.speed;
        double targetVelZ = headingZ * (double)this.speed;
        if (!this.isOnGround()) {
            grip = 0.05;
        } else if (this.inputState.brake) {
            grip = 0.16;
        } else if (Math.abs(this.speed) < 0.15f) {
            grip = 0.85;
        } else {
            float speedFrac = Math.min(Math.abs(this.speed) / maxSpd, 1.0f);
            grip = 0.62 - 0.28 * (double)speedFrac;
        }
        this.motionX += (targetVelX - this.motionX) * grip;
        this.motionZ += (targetVelZ - this.motionZ) * grip;
        if (this.isOnGround() && Math.abs(this.speed) < 0.003f) {
            this.motionX = 0.0;
            this.motionZ = 0.0;
        }
        boolean wasOnGroundBefore = this.isOnGround();
        if (this.isOnGround()) {
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
            this.verticalVelocity = Math.max(this.verticalVelocity - (double)0.08f, (double)-3.92f);
        }
        Vec3d movement = new Vec3d(this.motionX, this.verticalVelocity, this.motionZ);
        this.doMove(movement, wasOnGroundBefore);
        VehicleCollisionHandler.handleCollisions(this);
        this.computeTilt();
        this.refreshBoundingBox();
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

    public float getStepHeight() {
        return 1.25f;
    }

    private void doMove(Vec3d movement, boolean wasOnGroundBefore) {
        double yBeforeMove = this.getY();
        this.setVelocity(movement);
        this.move(MovementType.SELF, movement);
        if (wasOnGroundBefore && !this.isOnGround() && movement.y <= 0.0) {
            double yBeforeStepDown = this.getY();
            this.move(MovementType.SELF, new Vec3d(0.0, -1.25, 0.0));
            if (this.isOnGround()) {
                this.verticalVelocity = -0.06;
            } else {
                this.setPosition(this.getX(), yBeforeStepDown, this.getZ());
                this.verticalVelocity = 0.0;
            }
        }
        double totalYDelta = this.getY() - yBeforeMove;
        VehicleCollisionHandler.resolveBlockCollisions(this, movement);
        this.refreshBoundingBox();
        if (wasOnGroundBefore && this.isOnGround() && Math.abs(totalYDelta) > 0.04) {
            this.visualYOffset -= (float)totalYDelta;
            this.visualYOffset = MathHelper.clamp((float)this.visualYOffset, (float)-1.25f, (float)1.25f);
        }
        if (this.isOnGround()) {
            this.verticalVelocity = -0.06;
        }
    }

    private void computeTilt() {
        boolean isStopped;
        if (this.structure == null) {
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
        for (VehicleStructure.StoredBlock cb : contacts) {
            double worldX = this.getX() + (cb.rx() * (double)cosY - cb.rz() * (double)sinY);
            double worldZ = this.getZ() + (cb.rx() * (double)sinY + cb.rz() * (double)cosY);
            int topBlockY = (int)Math.floor(this.getY() + minRy + 1.25);
            int bottomBlockY = (int)Math.floor(this.getY() + minRy - 2.5);
            double groundY = this.getY() + minRy;
            boolean found = false;
            for (int by = topBlockY; by >= bottomBlockY; --by) {
                double surfaceY;
                VoxelShape shape;
                BlockPos checkPos = BlockPos.ofFloored((double)worldX, (double)by, (double)worldZ);
                BlockState state = this.getWorld().getBlockState(checkPos);
                if (state.isAir() || (shape = state.getCollisionShape((BlockView)this.getWorld(), checkPos)).isEmpty() || !((surfaceY = (double)checkPos.getY() + shape.getMax(Direction.Axis.Y)) <= this.getY() + minRy + 1.25 + 0.15)) continue;
                groundY = surfaceY;
                found = true;
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
        if (this.isOnGround() && Math.abs(this.speed) > 0.05f) {
            float yawRadTilt = (float)Math.toRadians(this.getYaw());
            double rightX = Math.cos(yawRadTilt);
            double rightZ = Math.sin(yawRadTilt);
            double lateralSlip = this.motionX * rightX + this.motionZ * rightZ;
            targetRoll -= (float)(lateralSlip * 20.0);
        }
        if (this.isOnGround() && this.hasDriver()) {
            if (this.inputState.forward && this.speed > 0.05f && this.speed < this.getMaxSpeed() * 0.75f) {
                targetPitch += 1.5f;
            } else if (this.inputState.brake && Math.abs(this.speed) > 0.1f) {
                targetPitch -= 2.5f;
            }
        }
        if (!this.isOnGround()) {
            float fwdSpd = Math.max(0.12f, Math.abs(this.speed));
            float flightPitch = -((float)Math.toDegrees(Math.atan2(this.verticalVelocity, fwdSpd)));
            targetPitch = MathHelper.clamp((float)flightPitch, (float)-26.0f, (float)26.0f);
            targetRoll *= 0.85f;
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
        if (passenger instanceof LivingEntity) {
            LivingEntity living = (LivingEntity)passenger;
            living.bodyYaw = this.getYaw();
        }
        if (Math.abs(adjustment = (clamped = MathHelper.clamp((float)(deltaYaw = MathHelper.wrapDegrees((float)(passenger.getYaw() - this.getYaw()))), (float)-105.0f, (float)105.0f)) - deltaYaw) > 0.001f) {
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
        passenger.setYaw(this.getYaw());
        passenger.setHeadYaw(this.getYaw());
        passenger.setBodyYaw(this.getYaw());
    }

    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        this.passengerSeatMap.remove(passenger.getUuid());
        this.inputState = VehicleInputState.EMPTY;
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
        VehicleSyncPayload pkt = new VehicleSyncPayload(this.getId(), this.getX(), this.getY(), this.getZ(), this.getYaw(), this.speed, this.vehiclePitch, this.vehicleRoll, this.angularVelocity);
        HashSet<ServerPlayerEntity> recipients = new HashSet<ServerPlayerEntity>(PlayerLookup.tracking(this));
        for (Entity passenger : this.getPassengerList()) {
            if (!(passenger instanceof ServerPlayerEntity)) continue;
            ServerPlayerEntity driverPlayer = (ServerPlayerEntity)passenger;
            recipients.add(driverPlayer);
        }
        for (ServerPlayerEntity player : recipients) {
            ServerPlayNetworking.send((ServerPlayerEntity)player, (CustomPayload)pkt);
        }
    }

    public void applyServerSync(double x, double y, double z, float yaw, float spd, float pitch, float roll, float omega) {
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
            this.vehiclePitch = pitch;
            this.vehicleRoll = roll;
            this.prevVehiclePitch = pitch;
            this.prevVehicleRoll = roll;
            this.angularVelocity = omega;
            this.prevAngularVelocity = omega;
            this.clientInterpSteps = 0;
            this.speed = spd;
            return;
        }
        this.clientTargetX = x;
        this.clientTargetY = y;
        this.clientTargetZ = z;
        this.clientTargetYaw = yaw;
        this.clientTargetPitch = pitch;
        this.clientTargetRoll = roll;
        this.angularVelocity = omega;
        this.speed = spd;
        this.clientInterpSteps = 3;
    }

    public boolean damage(ServerWorld world, DamageSource source, float amount) {
        return false;
    }

    public void dismantleBackToBlocks(ServerWorld world, PlayerEntity player) {
        this.removeAllPassengers();
        if (this.structure != null) {
            VehicleActivator.deactivate(world, this.structure, this.getPos(), this.getYaw());
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
    }
}

