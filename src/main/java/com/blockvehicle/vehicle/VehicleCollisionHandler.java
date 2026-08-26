package com.blockvehicle.vehicle;

import com.blockvehicle.entity.VehicleEntity;
import com.blockvehicle.sound.ModSounds;
import com.blockvehicle.vehicle.VehicleStructure;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.EndermiteEntity;
import net.minecraft.entity.mob.RavagerEntity;
import net.minecraft.entity.mob.SilverfishEntity;
import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.BatEntity;
import net.minecraft.entity.passive.CamelEntity;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.passive.FoxEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public final class VehicleCollisionHandler {
    private VehicleCollisionHandler() {
    }

    public static void handleCollisions(VehicleEntity vehicle) {
        if (vehicle.isRemoved()) {
            return;
        }
        VehicleStructure structure = vehicle.getStructure();
        if (structure == null) {
            return;
        }
        World world = vehicle.getWorld();
        VehicleCollisionHandler.handleVehicleToVehicleCollisions(vehicle, structure, world);
        VehicleCollisionHandler.handleEntityCollisions(vehicle, structure, world);
    }

    public static boolean resolveBlockCollisions(VehicleEntity vehicle, Vec3d attemptedMovement) {
        Box broadBox;
        if (vehicle.isRemoved()) {
            return false;
        }
        VehicleStructure structure = vehicle.getStructure();
        if (structure == null) {
            return false;
        }
        World world = vehicle.getWorld();
        Iterable broadCollisions = world.getBlockCollisions(vehicle, broadBox = vehicle.getBoundingBox().expand(0.15, 0.05, 0.15));
        if (!broadCollisions.iterator().hasNext()) {
            return false;
        }
        float relativeYaw = vehicle.getYaw() - structure.getInitialYaw();
        float yawRad = (float)Math.toRadians(relativeYaw);
        double cosY = Math.cos(yawRad);
        double sinY = Math.sin(yawRad);
        double vX = vehicle.getX();
        double vY = vehicle.getY();
        double vZ = vehicle.getZ();
        double stepThresholdY = vY + 1.25;
        double totalPushX = 0.0;
        double totalPushZ = 0.0;
        int contactCount = 0;
        List<VehicleStructure.StoredBlock> blocks = structure.getBlocks();
        for (VehicleStructure.StoredBlock sb : blocks) {
            double bWorldX = vX + (sb.rx() * cosY - sb.rz() * sinY);
            double bWorldY = vY + sb.ry();
            double bWorldZ = vZ + (sb.rx() * sinY + sb.rz() * cosY);
            double minX = bWorldX - 0.46;
            double maxX = bWorldX + 0.46;
            double minY = bWorldY + 0.08;
            double maxY = bWorldY + 0.92;
            double minZ = bWorldZ - 0.46;
            double maxZ = bWorldZ + 0.46;
            int minBlockX = (int)Math.floor(minX);
            int maxBlockX = (int)Math.floor(maxX);
            int minBlockY = (int)Math.floor(minY);
            int maxBlockY = (int)Math.floor(maxY);
            int minBlockZ = (int)Math.floor(minZ);
            int maxBlockZ = (int)Math.floor(maxZ);
            for (int bx = minBlockX; bx <= maxBlockX; ++bx) {
                for (int by = minBlockY; by <= maxBlockY; ++by) {
                    for (int bz = minBlockZ; bz <= maxBlockZ; ++bz) {
                        VoxelShape shape;
                        BlockPos checkPos = new BlockPos(bx, by, bz);
                        BlockState state = world.getBlockState(checkPos);
                        if (state.isAir() || (shape = state.getCollisionShape((BlockView)world, checkPos)).isEmpty()) continue;
                        for (Box shapeBox : shape.getBoundingBoxes()) {
                            double pushDir;
                            Box worldShapeBox = shapeBox.offset(checkPos);
                            double overlapMinX = Math.max(minX, worldShapeBox.minX);
                            double overlapMaxX = Math.min(maxX, worldShapeBox.maxX);
                            double overlapMinY = Math.max(minY, worldShapeBox.minY);
                            double overlapMaxY = Math.min(maxY, worldShapeBox.maxY);
                            double overlapMinZ = Math.max(minZ, worldShapeBox.minZ);
                            double overlapMaxZ = Math.min(maxZ, worldShapeBox.maxZ);
                            if (!(overlapMaxX > overlapMinX) || !(overlapMaxY > overlapMinY) || !(overlapMaxZ > overlapMinZ)) continue;
                            double depthX = overlapMaxX - overlapMinX;
                            double depthY = overlapMaxY - overlapMinY;
                            double depthZ = overlapMaxZ - overlapMinZ;
                            boolean isTrueWall = worldShapeBox.maxY > stepThresholdY;
                            if (!isTrueWall) continue;
                            if (depthX < depthZ && depthX < depthY) {
                                pushDir = bWorldX > (worldShapeBox.minX + worldShapeBox.maxX) / 2.0 ? 1.0 : -1.0;
                                totalPushX += pushDir * depthX;
                                ++contactCount;
                                continue;
                            }
                            if (!(depthZ < depthX) || !(depthZ < depthY)) continue;
                            pushDir = bWorldZ > (worldShapeBox.minZ + worldShapeBox.maxZ) / 2.0 ? 1.0 : -1.0;
                            totalPushZ += pushDir * depthZ;
                            ++contactCount;
                        }
                    }
                }
            }
        }
        if (contactCount > 0) {
            boolean isMoving;
            double pushMag = Math.sqrt(totalPushX * totalPushX + totalPushZ * totalPushZ);
            float spd = vehicle.getSpeed();
            boolean bl = isMoving = Math.abs(spd) > 0.003f;
            if (!isMoving && pushMag < 0.035) {
                return false;
            }
            if (pushMag > 0.001) {
                double moveDirZ;
                float globalYawRad;
                double moveDirX;
                double dot;
                double clampedPush = Math.min(pushMag, 0.4);
                double normX = totalPushX / pushMag;
                double normZ = totalPushZ / pushMag;
                double finalPushX = normX * clampedPush;
                double finalPushZ = normZ * clampedPush;
                vehicle.setPosition(vehicle.getX() + finalPushX, vehicle.getY(), vehicle.getZ() + finalPushZ);
                if (isMoving && (dot = (moveDirX = -Math.sin(globalYawRad = (float)Math.toRadians(vehicle.getYaw())) * (double)Math.signum(spd)) * normX + (moveDirZ = Math.cos(globalYawRad) * (double)Math.signum(spd)) * normZ) < -0.05) {
                    double tangentX = moveDirX - dot * normX;
                    double tangentZ = moveDirZ - dot * normZ;
                    double tangentLen = Math.sqrt(tangentX * tangentX + tangentZ * tangentZ);
                    if (tangentLen > 0.05) {
                        vehicle.setSpeed((float)((double)spd * Math.max(0.75, tangentLen * (double)0.94f)));
                    } else {
                        vehicle.setSpeed(spd * 0.5f);
                    }
                    if (Math.abs(spd) > 0.2f && !world.isClient()) {
                        world.playSound(null, vehicle.getX(), vehicle.getY(), vehicle.getZ(), ModSounds.CAR_IMPACT, SoundCategory.BLOCKS, 0.8f, 1.0f);
                    }
                }
                return true;
            }
        }
        return false;
    }

    public static float resolveRotationCollision(VehicleEntity vehicle, float oldYaw, float targetYaw) {
        if (vehicle.isRemoved()) {
            return targetYaw;
        }
        VehicleStructure structure = vehicle.getStructure();
        if (structure == null) {
            return targetYaw;
        }
        World world = vehicle.getWorld();
        float deltaYaw = MathHelper.wrapDegrees((float)(targetYaw - oldYaw));
        if (Math.abs(deltaYaw) < 0.01f) {
            return targetYaw;
        }
        float relativeYaw = targetYaw - structure.getInitialYaw();
        float yawRad = (float)Math.toRadians(relativeYaw);
        double cosY = Math.cos(yawRad);
        double sinY = Math.sin(yawRad);
        double vX = vehicle.getX();
        double vY = vehicle.getY();
        double vZ = vehicle.getZ();
        double stepThresholdY = vY + 1.25;
        double halfW = (double)structure.getWidth() / 2.0;
        double halfL = (double)structure.getLength() / 2.0;
        double[][] localCorners = new double[][]{{-halfW + 0.1, -halfL + 0.1}, {halfW - 0.1, -halfL + 0.1}, {-halfW + 0.1, halfL - 0.1}, {halfW - 0.1, halfL - 0.1}};
        boolean cornerBlocked = false;
        double pushOutX = 0.0;
        double pushOutZ = 0.0;
        block0: for (double[] corner : localCorners) {
            VoxelShape shape;
            double wx = vX + (corner[0] * cosY - corner[1] * sinY);
            double wy = vY + 0.5;
            double wz = vZ + (corner[0] * sinY + corner[1] * cosY);
            BlockPos bp = BlockPos.ofFloored((double)wx, (double)wy, (double)wz);
            BlockState state = world.getBlockState(bp);
            if (state.isAir() || (shape = state.getCollisionShape((BlockView)world, bp)).isEmpty()) continue;
            Box cornerBox = new Box(wx - 0.15, wy - 0.2, wz - 0.15, wx + 0.15, wy + 0.2, wz + 0.15);
            for (Box b : shape.getBoundingBoxes()) {
                Box worldShapeBox = b.offset(bp);
                if (!(worldShapeBox.maxY > stepThresholdY) || !cornerBox.intersects(worldShapeBox)) continue;
                cornerBlocked = true;
                double diffX = wx - ((double)bp.getX() + 0.5);
                double diffZ = wz - ((double)bp.getZ() + 0.5);
                double dist = Math.max(Math.sqrt(diffX * diffX + diffZ * diffZ), 0.001);
                pushOutX += diffX / dist * 0.15;
                pushOutZ += diffZ / dist * 0.15;
                continue block0;
            }
        }
        if (cornerBlocked) {
            double shiftMag = Math.sqrt(pushOutX * pushOutX + pushOutZ * pushOutZ);
            if (shiftMag > 0.01) {
                double tryX = vX + Math.min(pushOutX, 0.18);
                double tryZ = vZ + Math.min(pushOutZ, 0.18);
                Box tryBox = vehicle.getBoundingBox().offset(tryX - vX, 0.0, tryZ - vZ);
                Iterable testCollisions = world.getBlockCollisions(vehicle, tryBox);
                if (!testCollisions.iterator().hasNext()) {
                    vehicle.setPosition(tryX, vY, tryZ);
                    return targetYaw;
                }
            }
            vehicle.setAngularVelocity(-vehicle.getAngularVelocity() * 0.25f);
            return oldYaw + deltaYaw * 0.15f;
        }
        return targetYaw;
    }

    private static void handleEntityCollisions(VehicleEntity vehicle, VehicleStructure structure, World world) {
        Box searchBox = vehicle.getBoundingBox().expand(0.5, 0.2, 0.5);
        List<Entity> nearby = world.getOtherEntities(vehicle, searchBox, e -> e.isAlive() && !e.isSpectator() && !vehicle.hasPassenger(e) && !(e instanceof VehicleEntity));
        if (nearby.isEmpty()) {
            return;
        }
        float relativeYaw = vehicle.getYaw() - structure.getInitialYaw();
        float yawRad = (float)Math.toRadians(relativeYaw);
        double cosY = Math.cos(yawRad);
        double sinY = Math.sin(yawRad);
        double vX = vehicle.getX();
        double vY = vehicle.getY();
        double vZ = vehicle.getZ();
        float speed = vehicle.getSpeed();
        boolean isMovingFast = Math.abs(speed) >= 0.04f;
        List<VehicleStructure.StoredBlock> blocks = structure.getBlocks();
        for (Entity target : nearby) {
            if (target.getVehicle() == vehicle) continue;
            Box targetBox = target.getBoundingBox();
            double eRadius = Math.max(targetBox.getLengthX(), targetBox.getLengthZ()) / 2.0;
            double eHeight = targetBox.getLengthY();
            double dx = target.getX() - vX;
            double dz = target.getZ() - vZ;
            double dy = target.getY() - vY;
            double lx = dx * cosY + dz * sinY;
            double lz = -dx * sinY + dz * cosY;
            double ly = dy;
            double bestPushLx = 0.0;
            double bestPushLz = 0.0;
            double minPenetration = Double.MAX_VALUE;
            boolean collided = false;
            for (VehicleStructure.StoredBlock sb : blocks) {
                double pen;
                double blockMinY = sb.ry();
                double blockMaxY = sb.ry() + 1.0;
                if (ly + eHeight <= blockMinY || ly >= blockMaxY) continue;
                double diffX = lx - sb.rx();
                double diffZ = lz - sb.rz();
                double overlapX = 0.5 + eRadius - Math.abs(diffX);
                double overlapZ = 0.5 + eRadius - Math.abs(diffZ);
                if (!(overlapX > 0.0) || !(overlapZ > 0.0)) continue;
                collided = true;
                if (overlapX < overlapZ) {
                    pen = overlapX;
                    if (!(pen < minPenetration)) continue;
                    minPenetration = pen;
                    bestPushLx = (diffX >= 0.0 ? 1.0 : -1.0) * overlapX;
                    bestPushLz = 0.0;
                    continue;
                }
                pen = overlapZ;
                if (!(pen < minPenetration)) continue;
                minPenetration = pen;
                bestPushLx = 0.0;
                bestPushLz = (diffZ >= 0.0 ? 1.0 : -1.0) * overlapZ;
            }
            if (!collided) continue;
            double pushWx = bestPushLx * cosY - bestPushLz * sinY;
            double pushWz = bestPushLx * sinY + bestPushLz * cosY;
            if (isMovingFast) {
                float globalYawRad = (float)Math.toRadians(vehicle.getYaw());
                double moveDirX = -Math.sin(globalYawRad) * (double)Math.signum(speed);
                double moveDirZ = Math.cos(globalYawRad) * (double)Math.signum(speed);
                double vMass = vehicle.getTotalMass();
                double mMass = VehicleCollisionHandler.getMobMass(target);
                double restitution = 0.5;
                double speedLossRatio = mMass * (1.0 + restitution) / (vMass + mMass);
                speedLossRatio = Math.min(speedLossRatio, 0.65);
                double launchFactor = vMass * (1.0 + restitution) / (vMass + mMass);
                double launchForce = (double)Math.abs(speed) * launchFactor * 2.2;
                double launchX = moveDirX * launchForce + pushWx * 0.4;
                double launchZ = moveDirZ * launchForce + pushWz * 0.4;
                double launchY = 0.2 + (double)Math.abs(speed) * 0.6;
                target.addVelocity(launchX, launchY, launchZ);
                target.velocityModified = true;
                if (!world.isClient() && Math.abs(speed) >= 0.1f && target instanceof LivingEntity) {
                    LivingEntity living = (LivingEntity)target;
                    float damage = (float)((double)(Math.abs(speed) * 24.0f) * (vMass / 25.0));
                    LivingEntity driver = vehicle.getControllingPassenger();
                    DamageSource damageSource = driver != null ? world.getDamageSources().mobAttack(driver) : world.getDamageSources().generic();
                    living.damage((ServerWorld)world, damageSource, Math.max(1.5f, damage));
                    world.playSound(null, target.getX(), target.getY(), target.getZ(), ModSounds.CAR_IMPACT, SoundCategory.PLAYERS, 0.9f, 1.0f);
                }
                vehicle.setSpeed((float)((double)speed * (1.0 - speedLossRatio)));
                continue;
            }
            double pushMag = Math.sqrt(pushWx * pushWx + pushWz * pushWz);
            if (!(pushMag > 0.001)) continue;
            double normX = pushWx / pushMag;
            double normZ = pushWz / pushMag;
            double displacement = Math.min(pushMag, 0.35);
            target.setPosition(target.getX() + normX * displacement, target.getY(), target.getZ() + normZ * displacement);
            target.addVelocity(normX * 0.12, 0.0, normZ * 0.12);
            target.velocityModified = true;
        }
    }

    private static void handleVehicleToVehicleCollisions(VehicleEntity vehicleA, VehicleStructure structA, World world) {
        Box searchBox = vehicleA.getBoundingBox().expand(1.0);
        List<VehicleEntity> otherVehicles = world.getEntitiesByClass(VehicleEntity.class, searchBox, v -> v != vehicleA && !v.isRemoved() && v.getStructure() != null);
        if (otherVehicles.isEmpty()) {
            return;
        }
        for (VehicleEntity vehicleB : otherVehicles) {
            OBB2D obbB;
            OBB2D obbA;
            CollisionResult sat;
            VehicleStructure structB = vehicleB.getStructure();
            if (structB == null || (sat = VehicleCollisionHandler.checkSATCollision(obbA = VehicleCollisionHandler.getVehicleOBB(vehicleA, structA), obbB = VehicleCollisionHandler.getVehicleOBB(vehicleB, structB))) == null || !sat.colliding) continue;
            VehicleCollisionHandler.resolveCarToCarCollision(vehicleA, vehicleB, sat, world);
        }
    }

    private static void resolveCarToCarCollision(VehicleEntity vA, VehicleEntity vB, CollisionResult sat, World world) {
        double dz;
        Vec3d normal = sat.normal;
        double depth = sat.depth;
        double dx = vB.getX() - vA.getX();
        if (dx * normal.x + (dz = vB.getZ() - vA.getZ()) * normal.z < 0.0) {
            normal = normal.multiply(-1.0);
        }
        double separation = depth * 0.52;
        vA.setPosition(vA.getX() - normal.x * separation, vA.getY(), vA.getZ() - normal.z * separation);
        vB.setPosition(vB.getX() + normal.x * separation, vB.getY(), vB.getZ() + normal.z * separation);
        float spdA = vA.getSpeed();
        float spdB = vB.getSpeed();
        float yawRadA = (float)Math.toRadians(vA.getYaw());
        float yawRadB = (float)Math.toRadians(vB.getYaw());
        Vec3d velA = new Vec3d(-Math.sin(yawRadA) * (double)spdA, 0.0, Math.cos(yawRadA) * (double)spdA);
        Vec3d velB = new Vec3d(-Math.sin(yawRadB) * (double)spdB, 0.0, Math.cos(yawRadB) * (double)spdB);
        Vec3d relVel = velA.subtract(velB);
        double velAlongNormal = relVel.x * normal.x + relVel.z * normal.z;
        if (velAlongNormal > 0.0) {
            double massA = vA.getTotalMass();
            double massB = vB.getTotalMass();
            double totalMass = massA + massB;
            double restitution = 0.35f;
            double impulse = velAlongNormal * (1.0 + restitution);
            vA.setSpeed((float)((double)spdA - impulse * (massB / totalMass) * (double)Math.signum(spdA == 0.0f ? 1.0f : spdA)));
            vB.setSpeed((float)((double)spdB + impulse * (massA / totalMass) * (double)Math.signum(spdB == 0.0f ? 1.0f : spdB)));
            double midX = (vA.getX() + vB.getX()) / 2.0;
            double midZ = (vA.getZ() + vB.getZ()) / 2.0;
            double rAx = midX - vA.getX();
            double rAz = midZ - vA.getZ();
            double rBx = midX - vB.getX();
            double rBz = midZ - vB.getZ();
            double forceAx = -normal.x * impulse;
            double forceAz = -normal.z * impulse;
            double forceBx = normal.x * impulse;
            double forceBz = normal.z * impulse;
            double tauA = (rAx * forceAz - rAz * forceAx) * 16.0;
            double tauB = (rBx * forceBz - rBz * forceBx) * 16.0;
            vA.addAngularVelocity((float)(tauA / (double)vA.getMomentOfInertia()));
            vB.addAngularVelocity((float)(tauB / (double)vB.getMomentOfInertia()));
            if (velAlongNormal > 0.04) {
                world.playSound(null, (vA.getX() + vB.getX()) / 2.0, (vA.getY() + vB.getY()) / 2.0, (vA.getZ() + vB.getZ()) / 2.0, ModSounds.CAR_IMPACT, SoundCategory.BLOCKS, 1.0f, 1.0f);
            }
        }
    }

    private static OBB2D getVehicleOBB(VehicleEntity vehicle, VehicleStructure structure) {
        float relativeYaw = vehicle.getYaw() - structure.getInitialYaw();
        float rad = (float)Math.toRadians(relativeYaw);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        double halfW = (double)structure.getWidth() / 2.0;
        double halfL = (double)structure.getLength() / 2.0;
        return new OBB2D(vehicle.getX(), vehicle.getZ(), halfW, halfL, cos, sin);
    }

    private static CollisionResult checkSATCollision(OBB2D a, OBB2D b) {
        Vec3d[] axes = new Vec3d[]{new Vec3d(a.cos, 0.0, a.sin), new Vec3d(-a.sin, 0.0, a.cos), new Vec3d(b.cos, 0.0, b.sin), new Vec3d(-b.sin, 0.0, b.cos)};
        double minOverlap = Double.MAX_VALUE;
        Vec3d bestAxis = null;
        double dX = b.cx - a.cx;
        double dZ = b.cz - a.cz;
        for (Vec3d axis : axes) {
            double projB;
            double projD = Math.abs(dX * axis.x + dZ * axis.z);
            double projA = a.halfW * Math.abs(a.cos * axis.x + a.sin * axis.z) + a.halfL * Math.abs(-a.sin * axis.x + a.cos * axis.z);
            double overlap = projA + (projB = b.halfW * Math.abs(b.cos * axis.x + b.sin * axis.z) + b.halfL * Math.abs(-b.sin * axis.x + b.cos * axis.z)) - projD;
            if (overlap <= 0.0) {
                return null;
            }
            if (!(overlap < minOverlap)) continue;
            minOverlap = overlap;
            bestAxis = axis;
        }
        return new CollisionResult(true, bestAxis, minOverlap);
    }

    public static double getMobMass(Entity target) {
        if (target == null) {
            return 1.0;
        }
        if (target instanceof ChickenEntity || target instanceof BatEntity || target instanceof SilverfishEntity || target instanceof EndermiteEntity) {
            return 0.15;
        }
        if (target instanceof PigEntity || target instanceof SheepEntity || target instanceof WolfEntity || target instanceof CatEntity || target instanceof FoxEntity) {
            return 0.5;
        }
        if (target instanceof CowEntity || target instanceof AbstractHorseEntity || target instanceof CamelEntity || target instanceof EndermanEntity || target instanceof SpiderEntity) {
            return 1.8;
        }
        if (target instanceof IronGolemEntity || target instanceof RavagerEntity || target instanceof WardenEntity || target instanceof WitherEntity) {
            return 8.0;
        }
        return 1.0;
    }

    private record OBB2D(double cx, double cz, double halfW, double halfL, double cos, double sin) {
    }

    private record CollisionResult(boolean colliding, Vec3d normal, double depth) {
    }
}

