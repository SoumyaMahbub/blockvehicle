package com.blockvehicle.vehicle;

import com.blockvehicle.ModBlocks;
import com.blockvehicle.block.DriverSeatBlock;
import com.blockvehicle.block.PassengerSeatBlock;
import com.blockvehicle.vehicle.SeatData;
import com.blockvehicle.vehicle.VehicleStructure;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
        return VehicleActivator.activate(world, min, max, null, null, Set.of(), Set.of(), Direction.SOUTH);
    }

    public static VehicleStructure activate(ServerWorld world, BlockPos min, BlockPos max, BlockPos customDriverSeat, Direction customDriverFacing, Set<BlockPos> customPassengerSeats, Direction defaultPlayerFacing) {
        return VehicleActivator.activate(world, min, max, customDriverSeat, customDriverFacing, customPassengerSeats, Set.of(), defaultPlayerFacing);
    }

    public static VehicleStructure activate(ServerWorld world, BlockPos min, BlockPos max, BlockPos customDriverSeat, Direction customDriverFacing, Set<BlockPos> customPassengerSeats, Set<BlockPos> customWheels, Direction defaultPlayerFacing) {
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
        if (driverSeat == null) {
            driverSeat = new SeatData(0.0, 0.1, 0.0, true, driverYaw);
        }
        seats.add(0, driverSeat);
        float yawRad = (float)Math.toRadians(driverYaw);
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
        return new VehicleStructure(storedBlocks, seats, wheelDataList, storedFrames, width, height, length, origin, driverYaw);
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

