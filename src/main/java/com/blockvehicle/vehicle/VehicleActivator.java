package com.blockvehicle.vehicle;

import com.blockvehicle.config.BlockVehicleConfig;
import com.blockvehicle.ModBlocks;
import com.blockvehicle.block.DriverSeatBlock;
import com.blockvehicle.block.PassengerSeatBlock;
import com.blockvehicle.vehicle.SeatData;
import com.blockvehicle.vehicle.VehicleStructure;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.block.AbstractSignBlock;
import net.minecraft.block.BannerBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ButtonBlock;
import net.minecraft.block.CarpetBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.LadderBlock;
import net.minecraft.block.LanternBlock;
import net.minecraft.block.LeverBlock;
import net.minecraft.block.PaneBlock;
import net.minecraft.block.PressurePlateBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.TorchBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.TripwireBlock;
import net.minecraft.block.TripwireHookBlock;
import net.minecraft.block.WallBannerBlock;
import net.minecraft.block.WallTorchBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.GlowItemFrameEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class VehicleActivator {
    public static int countNonAirBlocks(ServerWorld world, BlockPos min, BlockPos max, int stopAfter) {
        int x0 = Math.min(min.getX(), max.getX());
        int y0 = Math.min(min.getY(), max.getY());
        int z0 = Math.min(min.getZ(), max.getZ());
        int x1 = Math.max(min.getX(), max.getX());
        int y1 = Math.max(min.getY(), max.getY());
        int z1 = Math.max(min.getZ(), max.getZ());
        int count = 0;
        for (int x = x0; x <= x1; ++x) {
            for (int y = y0; y <= y1; ++y) {
                for (int z = z0; z <= z1; ++z) {
                    if (!world.getBlockState(new BlockPos(x, y, z)).isAir() && ++count > stopAfter) {
                        return count;
                    }
                }
            }
        }
        return count;
    }

    public static float directionToYaw(Direction dir) {
        if (dir == null) {
            return 0.0f;
        }
        return switch (dir) {
            case Direction.NORTH -> 180.0f;
            case Direction.SOUTH -> 0.0f;
            case Direction.WEST -> 90.0f;
            case Direction.EAST -> 270.0f;
            default -> 0.0f;
        };
    }

    public static Direction getBlockHorizontalFacing(BlockState state, Direction fallback) {
        Direction dir;
        if (state.contains(Properties.HORIZONTAL_FACING)) {
            return (Direction)state.get(Properties.HORIZONTAL_FACING);
        }
        if (state.contains(Properties.FACING) && (dir = (Direction)state.get(Properties.FACING)).getAxis().isHorizontal()) {
            return dir;
        }
        return fallback != null ? fallback : Direction.SOUTH;
    }

    public static boolean isAttachedBlock(BlockState state) {
        if (state == null || state.isAir()) {
            return false;
        }
        Block b = state.getBlock();
        return b instanceof ButtonBlock || b instanceof LeverBlock || b instanceof TorchBlock || b instanceof WallTorchBlock || b instanceof TripwireHookBlock || b instanceof TripwireBlock || b instanceof PressurePlateBlock || b instanceof CarpetBlock || b instanceof LadderBlock || b instanceof AbstractSignBlock || b instanceof BannerBlock || b instanceof WallBannerBlock || b instanceof TrapdoorBlock || b instanceof DoorBlock || b instanceof LanternBlock || b instanceof PaneBlock;
    }

    private static boolean isAutoWheelBlock(BlockState state) {
        if (state == null) {
            return false;
        }
        Block b = state.getBlock();
        String id = Registries.BLOCK.getId(b).getPath();
        return id.contains("wheel") || id.contains("tire") || b == Blocks.BLACK_WOOL || b == Blocks.GRAY_WOOL || b == Blocks.COAL_BLOCK || b == Blocks.BLACK_CONCRETE || b == Blocks.GRAY_CONCRETE || b == Blocks.GRINDSTONE || b == Blocks.POLISHED_BLACKSTONE || b == Blocks.POLISHED_BLACKSTONE_SLAB || b == Blocks.BLACK_TERRACOTTA || b == Blocks.DEEPSLATE_TILES || b == Blocks.DEEPSLATE_TILE_SLAB || b == Blocks.NETHERITE_BLOCK;
    }

    public static VehicleStructure activate(ServerWorld world, BlockPos min, BlockPos max) {
        return VehicleActivator.activate(world, min, max, null, null, Set.of(), Set.of(), PlaneSetup.GROUND, Direction.SOUTH);
    }

    public static VehicleStructure activate(ServerWorld world, BlockPos min, BlockPos max, BlockPos customDriverSeat, Direction customDriverFacing, Set<BlockPos> customPassengerSeats, Direction defaultPlayerFacing) {
        return VehicleActivator.activate(world, min, max, customDriverSeat, customDriverFacing, customPassengerSeats, Set.of(), defaultPlayerFacing);
    }

    public static VehicleStructure activate(ServerWorld world, BlockPos min, BlockPos max, BlockPos customDriverSeat, Direction customDriverFacing, Set<BlockPos> customPassengerSeats, Set<BlockPos> customWheels, Direction defaultPlayerFacing) {
        return VehicleActivator.activate(world, min, max, customDriverSeat, customDriverFacing, customPassengerSeats, customWheels, PlaneSetup.GROUND, defaultPlayerFacing);
    }

    public static VehicleStructure activate(ServerWorld world, BlockPos min, BlockPos max, BlockPos customDriverSeat, Direction customDriverFacing, Set<BlockPos> customPassengerSeats, Set<BlockPos> customWheels, PlaneSetup planeSetup, Direction defaultPlayerFacing) {
        return VehicleActivator.capture(world, min, max, customDriverSeat, customDriverFacing, customPassengerSeats, customWheels, planeSetup, defaultPlayerFacing, true);
    }

    public static VehicleStructure capturePreset(ServerWorld world, BlockPos min, BlockPos max, BlockPos customDriverSeat, Direction customDriverFacing, Set<BlockPos> customPassengerSeats, Set<BlockPos> customWheels, Direction defaultPlayerFacing) {
        return VehicleActivator.capturePreset(world, min, max, customDriverSeat, customDriverFacing, customPassengerSeats, customWheels, PlaneSetup.GROUND, defaultPlayerFacing);
    }

    public static VehicleStructure capturePreset(ServerWorld world, BlockPos min, BlockPos max, BlockPos customDriverSeat, Direction customDriverFacing, Set<BlockPos> customPassengerSeats, Set<BlockPos> customWheels, PlaneSetup planeSetup, Direction defaultPlayerFacing) {
        return VehicleActivator.capture(world, min, max, customDriverSeat, customDriverFacing, customPassengerSeats, customWheels, planeSetup, defaultPlayerFacing, false);
    }

    private static VehicleStructure capture(ServerWorld world, BlockPos min, BlockPos max, BlockPos customDriverSeat, Direction customDriverFacing, Set<BlockPos> customPassengerSeats, Set<BlockPos> customWheels, PlaneSetup planeSetup, Direction defaultPlayerFacing, boolean removeFromWorld) {
        double rz;
        double ry;
        double rx;
        int x0 = Math.min(min.getX(), max.getX());
        int y0 = Math.min(min.getY(), max.getY());
        int z0 = Math.min(min.getZ(), max.getZ());
        int x1 = Math.max(min.getX(), max.getX());
        int y1 = Math.max(min.getY(), max.getY());
        int z1 = Math.max(min.getZ(), max.getZ());
        int width = x1 - x0 + 1;
        int height = y1 - y0 + 1;
        int length = z1 - z0 + 1;
        double cx = (double)x0 + (double)width / 2.0;
        double cy = y0;
        double cz = (double)z0 + (double)length / 2.0;
        Vec3d origin = new Vec3d(cx, cy, cz);
        ArrayList<VehicleStructure.StoredBlock> storedBlocks = new ArrayList<VehicleStructure.StoredBlock>();
        ArrayList<SeatData> seats = new ArrayList<SeatData>();
        ArrayList<Vec3d> wheelLocalPositions = new ArrayList<Vec3d>();
        SeatData driverSeat = null;
        float driverYaw = VehicleActivator.directionToYaw(defaultPlayerFacing != null ? defaultPlayerFacing : Direction.SOUTH);
        for (int x = x0; x <= x1; ++x) {
            for (int y = y0; y <= y1; ++y) {
                for (int z = z0; z <= z1; ++z) {
                    boolean isAutoWheel;
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = world.getBlockState(pos);
                    if (state.isAir()) continue;
                    rx = (double)x + 0.5 - cx;
                    ry = y - y0;
                    rz = (double)z + 0.5 - cz;
                    NbtCompound beNbt = null;
                    BlockEntity be = world.getBlockEntity(pos);
                    if (be != null) {
                        beNbt = be.createNbtWithId((RegistryWrapper.WrapperLookup)world.getRegistryManager());
                    }
                    storedBlocks.add(new VehicleStructure.StoredBlock(state, rx, ry, rz, beNbt));
                    Direction blockFacing = VehicleActivator.getBlockHorizontalFacing(state, defaultPlayerFacing);
                    if (pos.equals(customDriverSeat)) {
                        Direction facing = customDriverFacing != null ? customDriverFacing : blockFacing;
                        float seatYaw = VehicleActivator.directionToYaw(facing);
                        driverSeat = new SeatData(rx, ry + 0.1, rz, true, seatYaw);
                        driverYaw = seatYaw;
                    } else if (customPassengerSeats != null && customPassengerSeats.contains(pos)) {
                        seats.add(new SeatData(rx, ry + 0.1, rz, false, VehicleActivator.directionToYaw(blockFacing)));
                    } else if (state.isOf(ModBlocks.DRIVER_SEAT)) {
                        Direction facing = (Direction)state.get(DriverSeatBlock.FACING);
                        float seatYaw = VehicleActivator.directionToYaw(facing);
                        if (driverSeat == null) {
                            driverSeat = new SeatData(rx, ry + 0.1, rz, true, seatYaw);
                            driverYaw = seatYaw;
                        } else {
                            seats.add(new SeatData(rx, ry + 0.1, rz, false, seatYaw));
                        }
                    } else if (state.isOf(ModBlocks.PASSENGER_SEAT)) {
                        Direction facing = (Direction)state.get(PassengerSeatBlock.FACING);
                        seats.add(new SeatData(rx, ry + 0.1, rz, false, VehicleActivator.directionToYaw(facing)));
                    } else if (state.getBlock() instanceof StairsBlock) {
                        float seatYaw = VehicleActivator.directionToYaw(blockFacing);
                        if (driverSeat == null && customDriverSeat == null) {
                            driverSeat = new SeatData(rx, ry + 0.1, rz, true, seatYaw);
                            driverYaw = seatYaw;
                        } else if (customPassengerSeats == null || !customPassengerSeats.contains(pos)) {
                            seats.add(new SeatData(rx, ry + 0.1, rz, false, seatYaw));
                        }
                    }
                    boolean isCustomWheel = customWheels != null && customWheels.contains(pos);
                    boolean bl = isAutoWheel = (customWheels == null || customWheels.isEmpty()) && ry <= 1.0 && VehicleActivator.isAutoWheelBlock(state);
                    if (!isCustomWheel && !isAutoWheel) continue;
                    wheelLocalPositions.add(new Vec3d(rx, ry, rz));
                }
            }
        }
        Box box = new Box((double)x0 - 0.5, (double)y0 - 0.5, (double)z0 - 0.5, (double)x1 + 1.5, (double)y1 + 1.5, (double)z1 + 1.5);
        List<ItemFrameEntity> itemFrames = world.getEntitiesByClass(ItemFrameEntity.class, box, e -> true);
        ArrayList<VehicleStructure.StoredItemFrame> storedFrames = new ArrayList<VehicleStructure.StoredItemFrame>();
        for (ItemFrameEntity frame : itemFrames) {
            rx = frame.getX() - cx;
            ry = frame.getY() - cy;
            rz = frame.getZ() - cz;
            NbtCompound itemTag = null;
            ItemStack held = frame.getHeldItemStack();
            if (!held.isEmpty()) {
                itemTag = (NbtCompound)held.toNbt((RegistryWrapper.WrapperLookup)world.getRegistryManager());
            }
            boolean isGlow = frame instanceof GlowItemFrameEntity;
            storedFrames.add(new VehicleStructure.StoredItemFrame(itemTag, frame.getFacing().getName(), rx, ry, rz, frame.getRotation(), isGlow));
        }
        if (removeFromWorld) {
            for (ItemFrameEntity frame : itemFrames) {
                frame.discard();
            }
            for (int x = x0; x <= x1; ++x) {
                for (int y = y0; y <= y1; ++y) {
                    for (int z = z0; z <= z1; ++z) {
                        BlockPos pos = new BlockPos(x, y, z);
                        if (world.getBlockState(pos).isAir()) continue;
                        world.removeBlockEntity(pos);
                        world.setBlockState(pos, Blocks.AIR.getDefaultState(), 51);
                    }
                }
            }
        }
        if (driverSeat == null) {
            driverSeat = new SeatData(0.0, 0.1, 0.0, true, driverYaw);
        }
        seats.add(0, driverSeat);
        VehicleMode vehicleMode = planeSetup != null ? planeSetup.mode() : VehicleMode.GROUND;
        float vehicleYaw = driverYaw;
        if (vehicleMode == VehicleMode.PLANE && planeSetup.isCompletePlane()) {
            vehicleYaw = resolvePlaneYaw(planeSetup, cx, cz, driverSeat, driverYaw);
        } else if (vehicleMode == VehicleMode.HELICOPTER && planeSetup.isCompleteHelicopter()) {
            vehicleYaw = resolvePlaneYaw(planeSetup, cx, cz, driverSeat, driverYaw);
        } else {
            vehicleMode = VehicleMode.GROUND;
        }
        float yawRad = (float)Math.toRadians(vehicleYaw);
        double fwdX = -Math.sin(yawRad);
        double fwdZ = Math.cos(yawRad);
        double maxFwdProj = -1.7976931348623157E308;
        for (Vec3d rpos : wheelLocalPositions) {
            double proj = rpos.x * fwdX + rpos.z * fwdZ;
            if (!(proj > maxFwdProj)) continue;
            maxFwdProj = proj;
        }
        ArrayList<VehicleStructure.WheelData> wheelDataList = new ArrayList<VehicleStructure.WheelData>();
        for (Vec3d rpos : wheelLocalPositions) {
            double proj = rpos.x * fwdX + rpos.z * fwdZ;
            boolean isSteering = Math.abs(proj - maxFwdProj) < 0.6;
            wheelDataList.add(new VehicleStructure.WheelData(rpos.x, rpos.y, rpos.z, isSteering));
        }
        PlaneDefinition planeDefinition = vehicleMode == VehicleMode.PLANE
            ? buildPlaneDefinition(planeSetup, storedBlocks, cx, y0, cz, vehicleYaw)
            : vehicleMode == VehicleMode.HELICOPTER
                ? buildHelicopterDefinition(planeSetup, storedBlocks, cx, y0, cz, vehicleYaw) : null;
        return new VehicleStructure(storedBlocks, seats, wheelDataList, storedFrames, width, height, length,
            origin, vehicleYaw, vehicleMode, planeDefinition);
    }

    private static PlaneDefinition buildPlaneDefinition(PlaneSetup setup, List<VehicleStructure.StoredBlock> blocks,
                                                        double cx, int baseY, double cz, float driverYaw) {
        float yawRadians = (float)Math.toRadians(driverYaw);
        double forwardX = -Math.sin(yawRadians);
        double forwardZ = Math.cos(yawRadians);
        PlaneDefinition.Point nose = setup.nose() != null ? localPoint(setup.nose(), cx, baseY, cz) : null;
        if (nose == null) {
            VehicleStructure.StoredBlock front = blocks.stream().max(Comparator.comparingDouble(
                block -> block.rx() * forwardX + block.rz() * forwardZ)).orElse(null);
            nose = front != null ? new PlaneDefinition.Point(front.rx(), front.ry(), front.rz())
                : new PlaneDefinition.Point(forwardX * 2.0, 0.0, forwardZ * 2.0);
        }
        PlaneDefinition.Point left = localPoint(setup.leftWingTip(), cx, baseY, cz);
        PlaneDefinition.Point right = localPoint(setup.rightWingTip(), cx, baseY, cz);
        // Builders naturally click the visible wing from their own perspective.
        // Accept reversed clicks and normalize the stored left/right convention.
        double rightX = -forwardZ;
        double rightZ = forwardX;
        double leftSide = left.rx() * rightX + left.rz() * rightZ;
        double rightSide = right.rx() * rightX + right.rz() * rightZ;
        if (leftSide > 0.0 && rightSide < 0.0) {
            PlaneDefinition.Point swap = left;
            left = right;
            right = swap;
        }
        double totalMass = 0.0;
        double massX = 0.0;
        double massY = 0.0;
        double massZ = 0.0;
        for (VehicleStructure.StoredBlock block : blocks) {
            double mass = VehicleStructure.getBlockMass(block.state());
            totalMass += mass;
            massX += block.rx() * mass;
            massY += (block.ry() + 0.5) * mass;
            massZ += block.rz() * mass;
        }
        Vec3d centerOfMass = totalMass > 1.0E-6
            ? new Vec3d(massX / totalMass, massY / totalMass, massZ / totalMass) : Vec3d.ZERO;
        Vec3d propellerAxis = new Vec3d(-Math.sin(yawRadians), 0.0, Math.cos(yawRadians));
        Comparator<BlockPos> positionOrder = Comparator.comparingInt(BlockPos::getX)
            .thenComparingInt(BlockPos::getY).thenComparingInt(BlockPos::getZ);
        ArrayList<PlaneDefinition.Point> hubs = new ArrayList<>();
        Map<PlaneDefinition.Point, BlockPos> hubSources = new HashMap<>();
        setup.propellerHubs().stream().sorted(positionOrder).forEach(hub -> {
            PlaneDefinition.Point local = localPoint(hub, cx, baseY, cz);
            hubs.add(local);
            hubSources.put(local, hub);
        });
        Map<PlaneDefinition.Point, ArrayList<PlaneDefinition.Point>> bladesByHub = new HashMap<>();
        for (PlaneDefinition.Point hub : hubs) bladesByHub.put(hub, new ArrayList<>());
        for (BlockPos bladePos : setup.propellerBlades().stream().sorted(positionOrder).toList()) {
            PlaneDefinition.Point blade = localPoint(bladePos, cx, baseY, cz);
            PlaneDefinition.Point closest = null;
            double closestDistance = Double.POSITIVE_INFINITY;
            for (PlaneDefinition.Point hub : hubs) {
                double distance = blade.squaredDistanceTo(hub);
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closest = hub;
                }
            }
            if (closest != null) bladesByHub.get(closest).add(blade);
        }
        ArrayList<PlaneDefinition.PropellerAssembly> propellers = new ArrayList<>();
        for (PlaneDefinition.Point hub : hubs) {
            BlockPos sourceHub = hubSources.get(hub);
            Direction configuredAxis = setup.propellerAxes().get(sourceHub);
            Vec3d axis = configuredAxis != null
                ? new Vec3d(configuredAxis.getOffsetX(), configuredAxis.getOffsetY(), configuredAxis.getOffsetZ())
                : propellerAxis;
            boolean clockwise = !setup.counterClockwiseHubs().contains(sourceHub);
            propellers.add(new PlaneDefinition.PropellerAssembly(hub, axis,
                bladesByHub.getOrDefault(hub, new ArrayList<>()), clockwise));
        }
        double wingDx = left.rx() - right.rx();
        double wingDy = left.ry() - right.ry();
        double wingDz = left.rz() - right.rz();
        float wingSpan = (float)Math.sqrt(wingDx * wingDx + wingDy * wingDy + wingDz * wingDz);
        float wingArea = Math.max(1.0f, wingSpan * Math.max(1.25f, (float)Math.sqrt(blocks.size()) * 0.22f));
        float liftScale = MathHelper.clamp(0.62f + wingSpan / (float)Math.sqrt(Math.max(totalMass, 1.0)) * 0.34f
            + wingArea / (float)Math.max(totalMass, 1.0) * 0.055f, 0.58f, 1.8f);
        float takeoffSpeed = MathHelper.clamp((float)Math.sqrt(PlanePhysics.GRAVITY / (PlanePhysics.BASE_LIFT * liftScale)) * 1.08f, 0.30f, 0.78f);
        Vec3d centerOfLift = new Vec3d((left.rx() + right.rx()) * 0.5,
            (left.ry() + right.ry()) * 0.5 + 0.5, (left.rz() + right.rz()) * 0.5);
        float balanceOffset = (float)((centerOfMass.x - centerOfLift.x) * forwardX
            + (centerOfMass.z - centerOfLift.z) * forwardZ);
        float leftDistance = (float)Math.abs(left.rx() * rightX + left.rz() * rightZ);
        float rightDistance = (float)Math.abs(right.rx() * rightX + right.rz() * rightZ);
        float asymmetry = Math.abs(leftDistance - rightDistance) / Math.max(1.0f, wingSpan);
        int bladeCount = propellers.stream().mapToInt(propeller -> propeller.blades().size()).sum();
        float enginePower = bladeCount > 0 ? MathHelper.clamp(0.55f + 0.18f * (float)Math.sqrt(bladeCount)
            + 0.12f * Math.max(0, propellers.size() - 1), 0.65f, 1.8f) : 0.0f;
        BlockVehicleConfig.Values config = BlockVehicleConfig.get();
        liftScale *= (float)config.planeLiftMultiplier;
        enginePower *= (float)config.planeThrustMultiplier;
        VehicleStructure.StoredBlock tailBlock = blocks.stream().min(Comparator.comparingDouble(
            block -> block.rx() * forwardX + block.rz() * forwardZ)).orElse(null);
        VehicleStructure.StoredBlock topBlock = blocks.stream().max(Comparator.comparingDouble(VehicleStructure.StoredBlock::ry)).orElse(null);
        VehicleStructure.StoredBlock bottomBlock = blocks.stream().min(Comparator.comparingDouble(VehicleStructure.StoredBlock::ry)).orElse(null);
        ArrayList<PlaneDefinition.Point> priorityPoints = new ArrayList<>(List.of(nose, left, right));
        for (VehicleStructure.StoredBlock block : new VehicleStructure.StoredBlock[]{tailBlock, topBlock, bottomBlock}) {
            if (block == null) continue;
            PlaneDefinition.Point point = new PlaneDefinition.Point(block.rx(), block.ry(), block.rz());
            if (!priorityPoints.contains(point)) priorityPoints.add(point);
        }
        return new PlaneDefinition(nose, left, right, centerOfMass, centerOfLift, propellers, priorityPoints,
            wingSpan, wingArea, liftScale, takeoffSpeed, enginePower, (float)config.planeDragMultiplier,
            (float)config.planeControlAssist, balanceOffset, asymmetry);
    }

    private static PlaneDefinition.Point localPoint(BlockPos pos, double cx, int baseY, double cz) {
        return new PlaneDefinition.Point(pos.getX() + 0.5 - cx, pos.getY() - baseY, pos.getZ() + 0.5 - cz);
    }

    private static PlaneDefinition buildHelicopterDefinition(PlaneSetup setup, List<VehicleStructure.StoredBlock> blocks,
                                                              double cx, int baseY, double cz, float yaw) {
        float yawRadians = (float)Math.toRadians(yaw);
        double forwardX = -Math.sin(yawRadians);
        double forwardZ = Math.cos(yawRadians);
        PlaneDefinition.Point nose = setup.nose() != null ? localPoint(setup.nose(), cx, baseY, cz) : null;
        if (nose == null) {
            VehicleStructure.StoredBlock front = blocks.stream().max(Comparator.comparingDouble(
                block -> block.rx() * forwardX + block.rz() * forwardZ)).orElse(null);
            nose = front != null ? new PlaneDefinition.Point(front.rx(), front.ry(), front.rz())
                : new PlaneDefinition.Point(forwardX * 2.0, 0.0, forwardZ * 2.0);
        }
        double totalMass = 0.0, massX = 0.0, massY = 0.0, massZ = 0.0;
        for (VehicleStructure.StoredBlock block : blocks) {
            double mass = VehicleStructure.getBlockMass(block.state());
            totalMass += mass;
            massX += block.rx() * mass;
            massY += (block.ry() + 0.5) * mass;
            massZ += block.rz() * mass;
        }
        Vec3d centerOfMass = totalMass > 1.0E-6
            ? new Vec3d(massX / totalMass, massY / totalMass, massZ / totalMass) : Vec3d.ZERO;
        Comparator<BlockPos> order = Comparator.comparingInt(BlockPos::getX)
            .thenComparingInt(BlockPos::getY).thenComparingInt(BlockPos::getZ);
        ArrayList<PlaneDefinition.Point> hubs = new ArrayList<>();
        Map<PlaneDefinition.Point, BlockPos> sources = new HashMap<>();
        setup.propellerHubs().stream().sorted(order).forEach(pos -> {
            PlaneDefinition.Point hub = localPoint(pos, cx, baseY, cz);
            hubs.add(hub);
            sources.put(hub, pos);
        });
        Map<PlaneDefinition.Point, ArrayList<PlaneDefinition.Point>> blades = new HashMap<>();
        for (PlaneDefinition.Point hub : hubs) blades.put(hub, new ArrayList<>());
        for (BlockPos bladePos : setup.propellerBlades().stream().sorted(order).toList()) {
            PlaneDefinition.Point blade = localPoint(bladePos, cx, baseY, cz);
            PlaneDefinition.Point closest = hubs.stream().min(Comparator.comparingDouble(blade::squaredDistanceTo)).orElse(null);
            if (closest != null) blades.get(closest).add(blade);
        }
        ArrayList<PlaneDefinition.PropellerAssembly> rotors = new ArrayList<>();
        for (PlaneDefinition.Point hub : hubs) {
            BlockPos source = sources.get(hub);
            Direction direction = setup.propellerAxes().getOrDefault(source, Direction.UP);
            Vec3d axis = new Vec3d(direction.getOffsetX(), direction.getOffsetY(), direction.getOffsetZ());
            rotors.add(new PlaneDefinition.PropellerAssembly(hub, axis, blades.get(hub),
                !setup.counterClockwiseHubs().contains(source)));
        }
        List<PlaneDefinition.PropellerAssembly> mainRotors = rotors.stream()
            .filter(rotor -> Math.abs(rotor.axis().y) >= 0.7).toList();
        Vec3d centerOfLift = mainRotors.stream().map(rotor -> rotor.hub().blockCenter())
            .reduce(Vec3d.ZERO, Vec3d::add).multiply(1.0 / Math.max(1, mainRotors.size()));
        float radius = 1.0f;
        for (PlaneDefinition.PropellerAssembly rotor : mainRotors) for (PlaneDefinition.Point blade : rotor.blades()) {
            radius = Math.max(radius, (float)Math.sqrt(blade.squaredDistanceTo(rotor.hub())));
        }
        int mainBladeCount = mainRotors.stream().mapToInt(rotor -> rotor.blades().size()).sum();
        int tailRotorCount = rotors.size() - mainRotors.size();
        float rotorDiameter = radius * 2.0f;
        float diskArea = (float)Math.PI * radius * radius;
        float liftScale = MathHelper.clamp(0.82f + (float)Math.sqrt(Math.max(1, mainBladeCount)) * 0.12f
            + diskArea / (float)Math.max(8.0, totalMass) * 0.08f, 0.72f, 1.8f);
        float enginePower = MathHelper.clamp(0.82f + (float)Math.sqrt(Math.max(1, mainBladeCount)) * 0.10f
            + Math.max(0, mainRotors.size() - 1) * 0.14f, 0.9f, 2.2f);
        BlockVehicleConfig.Values config = BlockVehicleConfig.get();
        liftScale *= (float)config.planeLiftMultiplier;
        enginePower *= (float)config.planeThrustMultiplier;
        float balanceOffset = (float)Math.sqrt((centerOfMass.x - centerOfLift.x) * (centerOfMass.x - centerOfLift.x)
            + (centerOfMass.z - centerOfLift.z) * (centerOfMass.z - centerOfLift.z));
        float asymmetry = MathHelper.clamp(balanceOffset / Math.max(1.0f, radius), 0.0f, 1.0f);
        ArrayList<PlaneDefinition.Point> priority = new ArrayList<>();
        priority.add(nose);
        for (PlaneDefinition.PropellerAssembly rotor : rotors) priority.add(rotor.hub());
        blocks.stream().min(Comparator.comparingDouble(VehicleStructure.StoredBlock::ry)).ifPresent(block ->
            priority.add(new PlaneDefinition.Point(block.rx(), block.ry(), block.rz())));
        // PlaneDefinition is the shared immutable aircraft-geometry container.
        // For helicopters, wingSpan/wingArea store rotor diameter/disk area and
        // takeoffSpeed stores yaw authority (tail rotors make it stronger).
        return new PlaneDefinition(nose, null, null, centerOfMass, centerOfLift, rotors, priority,
            rotorDiameter, diskArea, liftScale, tailRotorCount > 0 ? 0.72f : 0.46f,
            enginePower, (float)config.planeDragMultiplier, (float)config.planeControlAssist,
            balanceOffset, asymmetry);
    }

    public static String validatePlaneSetup(PlaneSetup setup, BlockPos min, BlockPos max,
                                            BlockPos driverSeat, Direction fallbackFacing) {
        if (setup != null && setup.mode() == VehicleMode.HELICOPTER) {
            if (!setup.isCompleteHelicopter()) {
                return "Helicopter Mode needs a main rotor hub (click its TOP or BOTTOM face) and at least one blade block.";
            }
            if (driverSeat == null) return "Helicopter Mode needs a marked Driver Seat.";
            return null;
        }
        if (setup == null || setup.mode() != VehicleMode.PLANE) {
            return null;
        }
        if (!setup.isCompletePlane()) {
            return "Plane Mode needs both wing-tip markers (the nose can fall back to driver-seat facing).";
        }
        double centerX = (Math.min(min.getX(), max.getX()) + Math.max(min.getX(), max.getX()) + 1.0) * 0.5;
        double centerZ = (Math.min(min.getZ(), max.getZ()) + Math.max(min.getZ(), max.getZ()) + 1.0) * 0.5;
        double referenceX = driverSeat != null ? driverSeat.getX() + 0.5 : centerX;
        double referenceZ = driverSeat != null ? driverSeat.getZ() + 0.5 : centerZ;
        double forwardX;
        double forwardZ;
        if (setup.nose() != null) {
            forwardX = setup.nose().getX() + 0.5 - referenceX;
            forwardZ = setup.nose().getZ() + 0.5 - referenceZ;
            double forwardLength = Math.sqrt(forwardX * forwardX + forwardZ * forwardZ);
            if (forwardLength < 0.75) {
                return "The nose marker must be visibly in front of the driver/plane center.";
            }
            float snappedYaw = Math.round((float)Math.toDegrees(Math.atan2(-forwardX, forwardZ)) / 90.0f) * 90.0f;
            forwardX = -Math.sin(Math.toRadians(snappedYaw));
            forwardZ = Math.cos(Math.toRadians(snappedYaw));
        } else {
            Direction facing = fallbackFacing != null ? fallbackFacing : Direction.SOUTH;
            float yaw = directionToYaw(facing);
            forwardX = -Math.sin(Math.toRadians(yaw));
            forwardZ = Math.cos(Math.toRadians(yaw));
        }
        // The selection box is often intentionally asymmetric (long nose/tail,
        // decorations, or one extra block), so its geometric center is not a
        // reliable fuselage center. Project both the driver reference and the
        // selection center onto the explicit tip-to-tip axis; either may define
        // the fuselage on an intentionally asymmetric or side-by-side cockpit.
        double wingX = setup.rightWingTip().getX() - setup.leftWingTip().getX();
        double wingZ = setup.rightWingTip().getZ() - setup.leftWingTip().getZ();
        double horizontalWingLength = Math.sqrt(wingX * wingX + wingZ * wingZ);
        if (horizontalWingLength < 1.5) {
            return "Wing tips need at least 1.5 blocks of horizontal separation.";
        }
        double wingAxisX = wingX / horizontalWingLength;
        double wingAxisZ = wingZ / horizontalWingLength;
        double leftSide = (setup.leftWingTip().getX() + 0.5 - referenceX) * wingAxisX
            + (setup.leftWingTip().getZ() + 0.5 - referenceZ) * wingAxisZ;
        double rightSide = (setup.rightWingTip().getX() + 0.5 - referenceX) * wingAxisX
            + (setup.rightWingTip().getZ() + 0.5 - referenceZ) * wingAxisZ;
        double leftCenterSide = (setup.leftWingTip().getX() + 0.5 - centerX) * wingAxisX
            + (setup.leftWingTip().getZ() + 0.5 - centerZ) * wingAxisZ;
        double rightCenterSide = (setup.rightWingTip().getX() + 0.5 - centerX) * wingAxisX
            + (setup.rightWingTip().getZ() + 0.5 - centerZ) * wingAxisZ;
        boolean straddlesReference = Math.abs(leftSide) >= 0.20 && Math.abs(rightSide) >= 0.20
            && leftSide * rightSide < 0.0;
        boolean straddlesSelection = Math.abs(leftCenterSide) >= 0.20 && Math.abs(rightCenterSide) >= 0.20
            && leftCenterSide * rightCenterSide < 0.0;
        if (!straddlesReference && !straddlesSelection) {
            return "Left and right wing tips must be on opposite sides of the fuselage.";
        }
        double dx = setup.leftWingTip().getX() - setup.rightWingTip().getX();
        double dy = setup.leftWingTip().getY() - setup.rightWingTip().getY();
        double dz = setup.leftWingTip().getZ() - setup.rightWingTip().getZ();
        if (dx * dx + dy * dy + dz * dz < 2.25) {
            return "Wing tips need at least 1.5 blocks of separation.";
        }
        return null;
    }

    public static String validateSelection(ServerWorld world, PlaneSetup setup, BlockPos min, BlockPos max,
                                           BlockPos customDriverSeat) {
        BlockVehicleConfig.Values config = BlockVehicleConfig.get();
        int width = Math.abs(max.getX() - min.getX()) + 1;
        int height = Math.abs(max.getY() - min.getY()) + 1;
        int length = Math.abs(max.getZ() - min.getZ()) + 1;
        if (width > config.maxVehicleAxis || height > config.maxVehicleAxis || length > config.maxVehicleAxis) {
            return "Each vehicle dimension is limited to " + config.maxVehicleAxis
                + " blocks to prevent huge collision/tracking regions.";
        }
        if (setup == null || !setup.isAircraft()) return null;
        if (!arePlaneMarkersInside(min, max, setup)) {
            return "Every aircraft marker and rotor/propeller block must be inside the selected region.";
        }
        if (!hasDriverSeat(world, min, max, customDriverSeat)) {
            return "Aircraft Mode needs a marked Driver Seat, Driver Seat block, or stair seat inside the selection.";
        }
        if (setup.propellerHubs().size() > config.maxPropellers) {
            return "Aircraft are limited to " + config.maxPropellers + " rotor/propeller assemblies.";
        }
        if (setup.propellerBlades().size() > config.maxPropellerBladeBlocks) {
            return "Aircraft rotors are limited to " + config.maxPropellerBladeBlocks + " blade blocks.";
        }
        if (setup.propellerHubs().isEmpty() != setup.propellerBlades().isEmpty()) {
            return setup.propellerHubs().isEmpty()
                ? "Propeller blade blocks need at least one hub."
                : "Each propeller hub needs at least one blade block (or remove all hubs for a glider).";
        }
        if (setup.mode() == VehicleMode.PLANE) {
            for (BlockPos marker : List.of(setup.leftWingTip(), setup.rightWingTip())) {
                if (world.getBlockState(marker).isAir()) return "Wing-tip markers must point to real selected blocks.";
            }
        }
        if (setup.nose() != null && world.getBlockState(setup.nose()).isAir()) {
            return "The nose marker must point to a real selected block.";
        }
        for (BlockPos hub : setup.propellerHubs()) {
            if (world.getBlockState(hub).isAir()) return "Every propeller hub must be a real selected block.";
            if (setup.propellerBlades().contains(hub)) return "A propeller hub cannot also be selected as a blade block.";
        }
        for (BlockPos blade : setup.propellerBlades()) {
            if (world.getBlockState(blade).isAir()) return "Every propeller blade marker must point to a real selected block.";
        }
        if (!setup.propellerHubs().isEmpty()) {
            java.util.HashMap<BlockPos, Integer> assigned = new java.util.HashMap<>();
            for (BlockPos hub : setup.propellerHubs()) assigned.put(hub, 0);
            for (BlockPos blade : setup.propellerBlades()) {
                BlockPos closest = setup.propellerHubs().stream().min(Comparator.comparingDouble(hub -> hub.getSquaredDistance(blade))).orElse(null);
                if (closest != null) assigned.put(closest, assigned.get(closest) + 1);
            }
            if (assigned.values().stream().anyMatch(count -> count == 0)) {
                return "Every propeller hub must have at least one nearby blade assigned to it.";
            }
        }
        if (setup.mode() == VehicleMode.HELICOPTER) {
            double centerX = (Math.min(min.getX(), max.getX()) + Math.max(min.getX(), max.getX()) + 1.0) * 0.5;
            double centerZ = (Math.min(min.getZ(), max.getZ()) + Math.max(min.getZ(), max.getZ()) + 1.0) * 0.5;
            double allowed = Math.max(2.5, Math.max(width, length) * 0.38);
            boolean centeredMainRotor = setup.propellerHubs().stream().anyMatch(hub -> {
                Direction axis = setup.propellerAxes().getOrDefault(hub, Direction.UP);
                double dx = hub.getX() + 0.5 - centerX;
                double dz = hub.getZ() + 0.5 - centerZ;
                return axis.getAxis() == Direction.Axis.Y && dx * dx + dz * dz <= allowed * allowed;
            });
            if (!centeredMainRotor) return "A vertical-axis main rotor hub must be near the helicopter's center.";
        }
        return null;
    }

    public static boolean arePlaneMarkersInside(BlockPos a, BlockPos b, PlaneSetup setup) {
        int minX = Math.min(a.getX(), b.getX());
        int minY = Math.min(a.getY(), b.getY());
        int minZ = Math.min(a.getZ(), b.getZ());
        int maxX = Math.max(a.getX(), b.getX());
        int maxY = Math.max(a.getY(), b.getY());
        int maxZ = Math.max(a.getZ(), b.getZ());
        java.util.function.Predicate<BlockPos> inside = pos -> pos != null && pos.getX() >= minX && pos.getX() <= maxX
            && pos.getY() >= minY && pos.getY() <= maxY && pos.getZ() >= minZ && pos.getZ() <= maxZ;
        if (setup.nose() != null && !inside.test(setup.nose())) return false;
        if (setup.mode() == VehicleMode.PLANE
            && (!inside.test(setup.leftWingTip()) || !inside.test(setup.rightWingTip()))) return false;
        for (BlockPos pos : setup.propellerHubs()) if (!inside.test(pos)) return false;
        for (BlockPos pos : setup.propellerBlades()) if (!inside.test(pos)) return false;
        return true;
    }

    private static boolean hasDriverSeat(ServerWorld world, BlockPos min, BlockPos max, BlockPos customDriverSeat) {
        int x0 = Math.min(min.getX(), max.getX());
        int y0 = Math.min(min.getY(), max.getY());
        int z0 = Math.min(min.getZ(), max.getZ());
        int x1 = Math.max(min.getX(), max.getX());
        int y1 = Math.max(min.getY(), max.getY());
        int z1 = Math.max(min.getZ(), max.getZ());
        if (customDriverSeat != null) {
            return customDriverSeat.getX() >= x0 && customDriverSeat.getX() <= x1
                && customDriverSeat.getY() >= y0 && customDriverSeat.getY() <= y1
                && customDriverSeat.getZ() >= z0 && customDriverSeat.getZ() <= z1
                && !world.getBlockState(customDriverSeat).isAir();
        }
        for (int x = x0; x <= x1; ++x) for (int y = y0; y <= y1; ++y) for (int z = z0; z <= z1; ++z) {
            BlockState state = world.getBlockState(new BlockPos(x, y, z));
            if (state.isOf(ModBlocks.DRIVER_SEAT) || state.getBlock() instanceof StairsBlock) return true;
        }
        return false;
    }

    private static float resolvePlaneYaw(PlaneSetup setup, double centerX, double centerZ,
                                         SeatData driverSeat, float fallbackYaw) {
        double referenceX = driverSeat != null ? driverSeat.rx : 0.0;
        double referenceZ = driverSeat != null ? driverSeat.rz : 0.0;
        if (setup.nose() == null) {
            return MathHelper.wrapDegrees(Math.round(fallbackYaw / 90.0f) * 90.0f);
        }
        PlaneDefinition.Point nose = localPoint(setup.nose(), centerX, 0, centerZ);
        double forwardX = nose.rx() - referenceX;
        double forwardZ = nose.rz() - referenceZ;
        if (forwardX * forwardX + forwardZ * forwardZ < 0.25) {
            forwardX = nose.rx();
            forwardZ = nose.rz();
        }
        if (forwardX * forwardX + forwardZ * forwardZ < 1.0E-6) {
            return fallbackYaw;
        }
        float rawYaw = MathHelper.wrapDegrees((float)Math.toDegrees(Math.atan2(-forwardX, forwardZ)));
        return MathHelper.wrapDegrees(Math.round(rawYaw / 90.0f) * 90.0f);
    }

    public static BlockRotation getBlockRotation(float relativeYaw) {
        float normalized = MathHelper.wrapDegrees((float)relativeYaw);
        int quadrant = Math.round(normalized / 90.0f);
        quadrant = (quadrant % 4 + 4) % 4;
        return switch (quadrant) {
            case 1 -> BlockRotation.CLOCKWISE_90;
            case 2 -> BlockRotation.CLOCKWISE_180;
            case 3 -> BlockRotation.COUNTERCLOCKWISE_90;
            default -> BlockRotation.NONE;
        };
    }

    public static boolean canPlaceAt(ServerWorld world, VehicleStructure structure, Vec3d vehiclePos, float yawDegrees) {
        if (structure == null || structure.getBlocks().isEmpty()) {
            return false;
        }
        float relativeYaw = yawDegrees - structure.getInitialYaw();
        float yawRad = (float)Math.toRadians(relativeYaw);
        double cosY = Math.cos(yawRad);
        double sinY = Math.sin(yawRad);
        java.util.HashSet<BlockPos> targets = new java.util.HashSet<>();
        for (VehicleStructure.StoredBlock block : structure.getBlocks()) {
            double wx = vehiclePos.x + (block.rx() * cosY - block.rz() * sinY);
            double wy = vehiclePos.y + block.ry();
            double wz = vehiclePos.z + (block.rx() * sinY + block.rz() * cosY);
            BlockPos pos = BlockPos.ofFloored(wx, wy, wz);
            if (!targets.add(pos) || !world.isInBuildLimit(pos) || !world.getWorldBorder().contains(pos)
                || !world.isChunkLoaded(pos) || !world.getBlockState(pos).isReplaceable()) {
                return false;
            }
        }
        return true;
    }

    public static void deactivate(ServerWorld world, VehicleStructure structure, Vec3d vehiclePos, float yawDegrees) {
        BlockState rotatedState;
        BlockPos worldPos;
        double wz;
        double wy;
        double wx;
        if (structure == null) {
            return;
        }
        float relativeYaw = yawDegrees - structure.getInitialYaw();
        float yawRad = (float)Math.toRadians(relativeYaw);
        double cosY = Math.cos(yawRad);
        double sinY = Math.sin(yawRad);
        BlockRotation blockRotation = VehicleActivator.getBlockRotation(relativeYaw);
        for (VehicleStructure.StoredBlock sb : structure.getBlocks()) {
            if (VehicleActivator.isAttachedBlock(sb.state())) continue;
            wx = vehiclePos.x + (sb.rx() * cosY - sb.rz() * sinY);
            wy = vehiclePos.y + sb.ry();
            wz = vehiclePos.z + (sb.rx() * sinY + sb.rz() * cosY);
            worldPos = BlockPos.ofFloored((double)wx, (double)wy, (double)wz);
            rotatedState = sb.state().rotate(blockRotation);
            world.setBlockState(worldPos, rotatedState, 51);
        }
        for (VehicleStructure.StoredBlock sb : structure.getBlocks()) {
            if (!VehicleActivator.isAttachedBlock(sb.state())) continue;
            wx = vehiclePos.x + (sb.rx() * cosY - sb.rz() * sinY);
            wy = vehiclePos.y + sb.ry();
            wz = vehiclePos.z + (sb.rx() * sinY + sb.rz() * cosY);
            worldPos = BlockPos.ofFloored((double)wx, (double)wy, (double)wz);
            rotatedState = sb.state().rotate(blockRotation);
            world.setBlockState(worldPos, rotatedState, 51);
        }
        for (VehicleStructure.StoredBlock sb : structure.getBlocks()) {
            BlockEntity be;
            if (sb.blockEntityNbt() == null || (be = world.getBlockEntity(worldPos = BlockPos.ofFloored((double)(wx = vehiclePos.x + (sb.rx() * cosY - sb.rz() * sinY)), (double)(wy = vehiclePos.y + sb.ry()), (double)(wz = vehiclePos.z + (sb.rx() * sinY + sb.rz() * cosY))))) == null) continue;
            be.read(sb.blockEntityNbt(), (RegistryWrapper.WrapperLookup)world.getRegistryManager());
            be.markDirty();
        }
        for (VehicleStructure.StoredItemFrame sf : structure.getItemFrames()) {
            wx = vehiclePos.x + (sf.rx() * cosY - sf.rz() * sinY);
            wy = vehiclePos.y + sf.ry();
            wz = vehiclePos.z + (sf.rx() * sinY + sf.rz() * cosY);
            worldPos = BlockPos.ofFloored((double)wx, (double)wy, (double)wz);
            Direction originalFacing = Direction.byName(sf.facingName());
            if (originalFacing == null) {
                originalFacing = Direction.NORTH;
            }
            Direction newFacing = originalFacing;
            if (originalFacing.getAxis().isHorizontal()) {
                int steps = Math.round(relativeYaw / 90.0f);
                for (int s = 0; s < (steps % 4 + 4) % 4; ++s) {
                    newFacing = newFacing.rotateYClockwise();
                }
            }
            try {
                ItemFrameEntity frame = sf.isGlow() ? new GlowItemFrameEntity(world, worldPos, newFacing) : new ItemFrameEntity(world, worldPos, newFacing);
                if (sf.itemTag() != null) {
                    ItemStack stack = ItemStack.fromNbtOrEmpty((RegistryWrapper.WrapperLookup)world.getRegistryManager(), (NbtCompound)sf.itemTag());
                    frame.setHeldItemStack(stack, false);
                }
                frame.setRotation(sf.rotation());
                world.spawnEntity(frame);
            }
            catch (Exception exception) {}
        }
    }
}
