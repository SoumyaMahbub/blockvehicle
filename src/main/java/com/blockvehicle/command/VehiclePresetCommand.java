package com.blockvehicle.command;

import com.blockvehicle.item.VehicleWandItem;
import com.blockvehicle.item.PlayerDataStore;
import com.blockvehicle.config.BlockVehicleConfig;
import com.blockvehicle.vehicle.PlaneDefinition;
import com.blockvehicle.vehicle.PlaneSetup;
import com.blockvehicle.vehicle.VehicleMode;
import com.blockvehicle.vehicle.VehicleActivator;
import com.blockvehicle.vehicle.VehiclePresetState;
import com.blockvehicle.vehicle.VehicleStructure;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.List;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;

public final class VehiclePresetCommand {
    private static final int MAX_PRESETS = 64;
    private static final int MAX_NAME_LENGTH = 64;

    private VehiclePresetCommand() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register((LiteralArgumentBuilder<ServerCommandSource>)CommandManager.literal("vehiclepreset")
            .then(CommandManager.literal("save")
                .then(CommandManager.argument("name", StringArgumentType.greedyString()).executes(VehiclePresetCommand::save)))
            .then(CommandManager.literal("spawn")
                .then(CommandManager.argument("name", StringArgumentType.greedyString()).executes(VehiclePresetCommand::spawn)))
            .then(CommandManager.literal("delete")
                .then(CommandManager.argument("name", StringArgumentType.greedyString()).executes(VehiclePresetCommand::delete)))
            .then(CommandManager.literal("list").executes(VehiclePresetCommand::list)));
    }

    private static int save(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendError(Text.literal("This command can only be used by a player."));
            return 0;
        }
        String name = cleanName(StringArgumentType.getString(context, "name"));
        if (name == null) {
            player.sendMessage(Text.literal("\u00a7cPreset names must contain 1-64 characters."), false);
            return 0;
        }
        BlockPos corner1 = VehicleWandItem.getCorner1(player);
        BlockPos corner2 = VehicleWandItem.getCorner2(player);
        if (corner1 == null || corner2 == null) {
            player.sendMessage(Text.literal("\u00a7cSelect Corner 1 and Corner 2 with the Vehicle Wand first."), false);
            return 0;
        }
        long volume = (long)(Math.abs(corner1.getX() - corner2.getX()) + 1)
            * (long)(Math.abs(corner1.getY() - corner2.getY()) + 1)
            * (long)(Math.abs(corner1.getZ() - corner2.getZ()) + 1);
        if (volume > 32768L) {
            player.sendMessage(Text.literal("\u00a7cThat selection is too large. Preset selection volume is limited to 32,768 blocks."), false);
            return 0;
        }
        PlaneSetup planeSetup = VehicleWandItem.getPlaneSetup(player);
        BlockPos customDriver = VehicleWandItem.getDriverSeat(player);
        Direction driverFacing = VehicleWandItem.getDriverFacing(player);
        String planeError = VehicleActivator.validatePlaneSetup(planeSetup, corner1, corner2, customDriver,
            driverFacing != null ? driverFacing : player.getHorizontalFacing());
        if (planeError == null) planeError = VehicleActivator.validateSelection(player.getServerWorld(), planeSetup,
            corner1, corner2, customDriver);
        if (planeError != null) {
            player.sendMessage(Text.literal("\u00a7c" + planeError), false);
            return 0;
        }
        VehicleStructure structure = VehicleActivator.capturePreset(
            player.getServerWorld(), corner1, corner2,
            customDriver, driverFacing,
            VehicleWandItem.getPassengerSeats(player), VehicleWandItem.getCustomWheels(player),
            planeSetup,
            player.getHorizontalFacing());
        int blockCount = structure.getBlocks().size();
        if (blockCount == 0) {
            player.sendMessage(Text.literal("\u00a7cNo blocks were found in the selected region."), false);
            return 0;
        }
        int maxBlocks = BlockVehicleConfig.get().maxVehicleBlocks;
        if (blockCount > maxBlocks) {
            player.sendMessage(Text.literal("\u00a7cThat build has " + blockCount + " blocks; presets are limited to " + maxBlocks + "."), false);
            return 0;
        }
        VehiclePresetState presetState = VehiclePresetState.get(player.getServer());
        if (presetState.get(name) == null && presetState.getNames().size() >= MAX_PRESETS) {
            player.sendMessage(Text.literal("\u00a7cThis world already has the maximum of " + MAX_PRESETS + " saved vehicle presets."), false);
            return 0;
        }
        presetState.put(name, structure);
        player.sendMessage(Text.literal("\u00a7aSaved vehicle preset \u00a7f" + name + "\u00a7a (" + blockCount + " blocks)."), false);
        return 1;
    }

    private static int spawn(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendError(Text.literal("This command can only be used by a player."));
            return 0;
        }
        String name = StringArgumentType.getString(context, "name").trim();
        VehicleStructure structure = VehiclePresetState.get(player.getServer()).get(name);
        if (structure == null) {
            player.sendMessage(Text.literal("\u00a7cUnknown vehicle preset: \u00a7f" + name), false);
            return 0;
        }
        int maxAxis = BlockVehicleConfig.get().maxVehicleAxis;
        if (structure.getWidth() > maxAxis || structure.getHeight() > maxAxis || structure.getLength() > maxAxis) {
            player.sendMessage(Text.literal("\u00a7cThat legacy preset exceeds the safe " + maxAxis + "-block axis limit."), false);
            return 0;
        }
        ServerWorld world = player.getServerWorld();
        Direction facing = player.getHorizontalFacing();
        double distance = Math.max(structure.getWidth(), structure.getLength()) / 2.0 + 3.0;
        double targetX = player.getX() + facing.getOffsetX() * distance;
        double targetZ = player.getZ() + facing.getOffsetZ() * distance;
        int minX = (int)Math.floor(targetX - structure.getWidth() / 2.0);
        int minZ = (int)Math.floor(targetZ - structure.getLength() / 2.0);
        int surfaceY = world.getBottomY();
        for (int x = minX; x < minX + structure.getWidth(); ++x) {
            for (int z = minZ; z < minZ + structure.getLength(); ++z) {
                surfaceY = Math.max(surfaceY, world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z));
            }
        }
        Vec3d origin = new Vec3d(minX + structure.getWidth() / 2.0, surfaceY, minZ + structure.getLength() / 2.0);
        if (!VehicleActivator.canPlaceAt(world, structure, origin, structure.getInitialYaw())) {
            player.sendMessage(Text.literal("\u00a7cThe preset cannot spawn there because the space is occupied or outside the world."), false);
            return 0;
        }
        VehicleActivator.deactivate(world, structure, origin, structure.getInitialYaw());
        restorePresetMarkers(player, structure, origin);
        player.sendMessage(Text.literal("\u00a7aSpawned preset \u00a7f" + name + "\u00a7a on the surface in front of you."), false);
        return 1;
    }

    private static int delete(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendError(Text.literal("This command can only be used by a player."));
            return 0;
        }
        String name = StringArgumentType.getString(context, "name").trim();
        if (!VehiclePresetState.get(player.getServer()).remove(name)) {
            player.sendMessage(Text.literal("\u00a7cUnknown vehicle preset: \u00a7f" + name), false);
            return 0;
        }
        player.sendMessage(Text.literal("\u00a7aDeleted vehicle preset \u00a7f" + name + "\u00a7a."), false);
        return 1;
    }

    private static int list(CommandContext<ServerCommandSource> context) {
        List<String> names = VehiclePresetState.get(context.getSource().getServer()).getNames();
        if (names.isEmpty()) {
            context.getSource().sendFeedback(() -> Text.literal("\u00a77No vehicle presets have been saved."), false);
        } else {
            context.getSource().sendFeedback(() -> Text.literal("\u00a76Vehicle presets (" + names.size() + "): \u00a7f" + String.join("\u00a77, \u00a7f", names)), false);
        }
        return names.size();
    }

    private static String cleanName(String raw) {
        String name = raw.trim();
        return name.isEmpty() || name.length() > MAX_NAME_LENGTH ? null : name;
    }

    private static void restorePresetMarkers(ServerPlayerEntity player, VehicleStructure structure, Vec3d origin) {
        java.util.UUID uuid = player.getUuid();
        PlayerDataStore.clear(uuid);
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (VehicleStructure.StoredBlock block : structure.getBlocks()) {
            BlockPos pos = worldPos(origin, block.rx(), block.ry(), block.rz());
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }
        if (minX != Integer.MAX_VALUE) {
            PlayerDataStore.setCorner1(uuid, new BlockPos(minX, minY, minZ));
            PlayerDataStore.setCorner2(uuid, new BlockPos(maxX, maxY, maxZ));
        }
        for (com.blockvehicle.vehicle.SeatData seat : structure.getSeats()) {
            BlockPos pos = worldPos(origin, seat.rx, seat.ry - 0.1, seat.rz);
            if (seat.isDriver) PlayerDataStore.setDriverSeat(uuid, pos, yawDirection(seat.yawOffset));
            else PlayerDataStore.togglePassengerSeat(uuid, pos);
        }
        for (VehicleStructure.WheelData wheel : structure.getWheels()) {
            PlayerDataStore.toggleWheel(uuid, worldPos(origin, wheel.rx(), wheel.ry(), wheel.rz()));
        }
        if (structure.getMode() == VehicleMode.PLANE && structure.getPlaneDefinition() != null) {
            PlaneDefinition plane = structure.getPlaneDefinition();
            PlayerDataStore.setPlaneNose(uuid, worldPos(origin, plane.nose().rx(), plane.nose().ry(), plane.nose().rz()));
            PlayerDataStore.setLeftWingTip(uuid, worldPos(origin, plane.leftWingTip().rx(), plane.leftWingTip().ry(), plane.leftWingTip().rz()));
            PlayerDataStore.setRightWingTip(uuid, worldPos(origin, plane.rightWingTip().rx(), plane.rightWingTip().ry(), plane.rightWingTip().rz()));
            for (PlaneDefinition.PropellerAssembly propeller : plane.propellers()) {
                PlayerDataStore.setPropellerHub(uuid,
                    worldPos(origin, propeller.hub().rx(), propeller.hub().ry(), propeller.hub().rz()),
                    axisDirection(propeller.axis()), propeller.clockwise());
                for (PlaneDefinition.Point blade : propeller.blades()) {
                    PlayerDataStore.togglePropellerBlade(uuid, worldPos(origin, blade.rx(), blade.ry(), blade.rz()));
                }
            }
        } else if (structure.getMode() == VehicleMode.HELICOPTER && structure.getPlaneDefinition() != null) {
            PlaneDefinition helicopter = structure.getPlaneDefinition();
            PlayerDataStore.setHelicopterNose(uuid, worldPos(origin, helicopter.nose().rx(), helicopter.nose().ry(), helicopter.nose().rz()));
            for (PlaneDefinition.PropellerAssembly rotor : helicopter.propellers()) {
                PlayerDataStore.setPropellerHub(uuid,
                    worldPos(origin, rotor.hub().rx(), rotor.hub().ry(), rotor.hub().rz()),
                    axisDirection(rotor.axis()), rotor.clockwise());
                for (PlaneDefinition.Point blade : rotor.blades()) {
                    PlayerDataStore.togglePropellerBlade(uuid, worldPos(origin, blade.rx(), blade.ry(), blade.rz()));
                }
            }
        }
        PlayerDataStore.syncToPlayer(player);
    }

    private static BlockPos worldPos(Vec3d origin, double rx, double ry, double rz) {
        return BlockPos.ofFloored(origin.x + rx, origin.y + ry, origin.z + rz);
    }

    private static Direction yawDirection(float yaw) {
        int quadrant = Math.floorMod(Math.round(yaw / 90.0f), 4);
        return switch (quadrant) {
            case 1 -> Direction.WEST;
            case 2 -> Direction.NORTH;
            case 3 -> Direction.EAST;
            default -> Direction.SOUTH;
        };
    }

    private static Direction axisDirection(Vec3d axis) {
        double ax = Math.abs(axis.x);
        double ay = Math.abs(axis.y);
        double az = Math.abs(axis.z);
        if (ay >= ax && ay >= az) return axis.y >= 0.0 ? Direction.UP : Direction.DOWN;
        if (ax >= az) return axis.x >= 0.0 ? Direction.EAST : Direction.WEST;
        return axis.z >= 0.0 ? Direction.SOUTH : Direction.NORTH;
    }
}
