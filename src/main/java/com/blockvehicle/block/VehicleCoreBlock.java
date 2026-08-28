package com.blockvehicle.block;

import com.blockvehicle.ModItems;
import com.blockvehicle.entity.VehicleEntity;
import com.blockvehicle.config.BlockVehicleConfig;
import com.blockvehicle.item.VehicleWandItem;
import com.blockvehicle.vehicle.ActivationConfirmManager;
import com.blockvehicle.vehicle.VehicleActivator;
import com.blockvehicle.vehicle.VehicleStructure;
import com.blockvehicle.vehicle.PlaneSetup;
import com.blockvehicle.vehicle.VehicleMode;
import java.util.Set;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Property;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class VehicleCoreBlock
extends Block {
    private static final long MAX_SELECTION_VOLUME = 32768L;
    public static final BooleanProperty ACTIVATED = BooleanProperty.of("activated");

    public VehicleCoreBlock(AbstractBlock.Settings settings) {
        super(settings);
        this.setDefaultState((this.getStateManager().getDefaultState()).with(ACTIVATED, Boolean.valueOf(false)));
    }

    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{ACTIVATED});
    }

    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }
        ServerWorld serverWorld = (ServerWorld)world;
        BlockPos corner1 = VehicleWandItem.getCorner1(player);
        BlockPos corner2 = VehicleWandItem.getCorner2(player);
        if (corner1 == null || corner2 == null) {
            player.sendMessage((Text)Text.literal("\u00a7eUse the \u00a7bVehicle Wand \u00a7eto select \u00a7lCorner 1 \u00a7eand \u00a7lCorner 2\u00a7e first!"), true);
            if (!player.getInventory().contains(new ItemStack(ModItems.VEHICLE_WAND))) {
                player.getInventory().offerOrDrop(new ItemStack(ModItems.VEHICLE_WAND));
                player.sendMessage((Text)Text.literal("\u00a7aGiven \u00a7bVehicle Wand\u00a7a. Right-click = Corner 1, Shift+Right-click = Corner 2."), false);
            }
            return ActionResult.SUCCESS;
        }
        VehicleCoreBlock.requestActivationConfirmation(serverWorld, player, corner1, corner2);
        return ActionResult.SUCCESS;
    }

    public static void requestActivationConfirmation(ServerWorld serverWorld, PlayerEntity player, BlockPos corner1, BlockPos corner2) {
        BlockPos customDriver = VehicleWandItem.getDriverSeat(player);
        Direction customDriverFacing = VehicleWandItem.getDriverFacing(player);
        Set<BlockPos> customPassengers = VehicleWandItem.getPassengerSeats(player);
        Set<BlockPos> customWheels = VehicleWandItem.getCustomWheels(player);
        PlaneSetup planeSetup = VehicleWandItem.getPlaneSetup(player);
        Direction setupFacing = customDriver != null && customDriverFacing != null ? customDriverFacing : player.getHorizontalFacing();
        String planeError = VehicleActivator.validatePlaneSetup(planeSetup, corner1, corner2, customDriver, setupFacing);
        if (planeError != null) {
            player.sendMessage(Text.literal("\u00a7c" + planeError), false);
            return;
        }
        String selectionError = VehicleActivator.validateSelection(serverWorld, planeSetup, corner1, corner2, customDriver);
        if (selectionError != null) {
            player.sendMessage(Text.literal("\u00a7c" + selectionError), false);
            return;
        }
        if (planeSetup.mode() == VehicleMode.PLANE) {
            long volume = (long)(Math.abs(corner1.getX() - corner2.getX()) + 1)
                * (Math.abs(corner1.getY() - corner2.getY()) + 1L)
                * (Math.abs(corner1.getZ() - corner2.getZ()) + 1L);
            if (volume <= MAX_SELECTION_VOLUME && VehicleActivator.arePlaneMarkersInside(corner1, corner2, planeSetup)) {
                VehicleStructure preview = VehicleActivator.capturePreset(serverWorld, corner1, corner2, customDriver,
                    customDriverFacing, customPassengers, customWheels, planeSetup, setupFacing);
                if (preview.isPlane()) {
                    long passengers = preview.getSeats().stream().filter(seat -> !seat.isDriver).count();
                    player.sendMessage(Text.literal(String.format("\u00a7b\u2708 PLANE \u00a77| \u00a7f%d blocks \u00a77| \u00a7f%.1f mass \u00a77| \u00a7f1 driver + %d passengers \u00a77| \u00a7f%d gear",
                        preview.getBlocks().size(), preview.getTotalMass(), passengers, preview.getWheels().size())), false);
                    player.sendMessage(Text.literal(String.format("\u00a77Wing span: \u00a7f%.1f \u00a77| propellers: \u00a7f%d \u00a77| takeoff: ~\u00a7f%.1f m/s \u00a77| balance: \u00a7f%+.1f",
                        preview.getPlaneDefinition().wingSpan(), preview.getPlaneDefinition().propellers().size(),
                        preview.getPlaneDefinition().takeoffSpeed() * 20.0f, preview.getPlaneDefinition().balanceOffset())), false);
                    if (!preview.getPlaneDefinition().hasEngines()) player.sendMessage(Text.literal("\u00a7eWarning: no valid propeller; this will be a glider."), false);
                    if (preview.getWheels().isEmpty()) player.sendMessage(Text.literal("\u00a7eWarning: no landing gear markers; body contacts will be used."), false);
                    if (preview.getPlaneDefinition().asymmetry() > 0.28f) player.sendMessage(Text.literal("\u00a7eWarning: the wing layout is strongly asymmetric."), false);
                }
            }
        }
        ActivationConfirmManager.requestConfirmation(serverWorld, player, corner1, corner2, customDriver, customDriverFacing, customPassengers, customWheels, planeSetup);
    }

    public static boolean activateVehicle(ServerWorld serverWorld, PlayerEntity player, BlockPos corner1, BlockPos corner2, BlockPos customDriver, Direction customDriverFacing, Set<BlockPos> customPassengers, Set<BlockPos> customWheels, PlaneSetup planeSetup, Direction playerFacing) {
        long volume = (long)(Math.abs(corner1.getX() - corner2.getX()) + 1)
            * (long)(Math.abs(corner1.getY() - corner2.getY()) + 1)
            * (long)(Math.abs(corner1.getZ() - corner2.getZ()) + 1);
        if (volume > MAX_SELECTION_VOLUME) {
            player.sendMessage(Text.literal("\u00a7cVehicle selection volume is limited to " + MAX_SELECTION_VOLUME + " blocks for server stability."), false);
            return false;
        }
        int maxBlocks = BlockVehicleConfig.get().maxVehicleBlocks;
        int selectedBlocks = VehicleActivator.countNonAirBlocks(serverWorld, corner1, corner2, maxBlocks);
        if (selectedBlocks > maxBlocks) {
            player.sendMessage(Text.literal("\u00a7cVehicles are limited to " + maxBlocks + " non-air blocks for multiplayer stability."), false);
            return false;
        }
        String planeError = VehicleActivator.validatePlaneSetup(planeSetup, corner1, corner2, customDriver, playerFacing);
        if (planeError != null) {
            player.sendMessage(Text.literal("\u00a7c" + planeError), false);
            return false;
        }
        String selectionError = VehicleActivator.validateSelection(serverWorld, planeSetup, corner1, corner2, customDriver);
        if (selectionError != null) {
            player.sendMessage(Text.literal("\u00a7c" + selectionError), false);
            return false;
        }
        VehicleStructure structure = VehicleActivator.activate(serverWorld, corner1, corner2, customDriver, customDriverFacing, customPassengers, customWheels, planeSetup, playerFacing);
        if (structure.getBlocks().isEmpty()) {
            player.sendMessage((Text)Text.literal("\u00a7cNo blocks found in selected region!"), true);
            return false;
        }
        Vec3d spawnPos = structure.getLocalOrigin();
        float spawnYaw = structure.getInitialYaw();
        VehicleEntity vehicle = new VehicleEntity(serverWorld, spawnPos, spawnYaw, structure);
        serverWorld.spawnEntity(vehicle);
        player.startRiding(vehicle);
        vehicle.mountAsDriver(player);
        VehicleWandItem.clearCorners(player);
        String type = structure.isPlane() ? "Plane" : "Vehicle";
        String planeDetails = structure.isPlane() ? ", " + structure.getPlaneDefinition().propellers().size() + " propellers" : "";
        player.sendMessage((Text)Text.literal(("\u00a7a" + type + " activated! \u00a77(" + structure.getBlocks().size() + " blocks, " + structure.getSeats().size() + " seats, " + structure.getWheels().size() + " wheels" + planeDetails + "). Right-click vehicle to drive.")), false);
        return true;
    }

    public static boolean activateVehicle(ServerWorld serverWorld, PlayerEntity player, BlockPos corner1, BlockPos corner2, BlockPos customDriver, Direction customDriverFacing, Set<BlockPos> customPassengers, Direction playerFacing) {
        return VehicleCoreBlock.activateVehicle(serverWorld, player, corner1, corner2, customDriver, customDriverFacing, customPassengers, Set.of(), PlaneSetup.GROUND, playerFacing);
    }

    public static boolean activateVehicle(ServerWorld serverWorld, PlayerEntity player, BlockPos corner1, BlockPos corner2) {
        BlockPos customDriver = VehicleWandItem.getDriverSeat(player);
        Direction customDriverFacing = VehicleWandItem.getDriverFacing(player);
        Set<BlockPos> customPassengers = VehicleWandItem.getPassengerSeats(player);
        Set<BlockPos> customWheels = VehicleWandItem.getCustomWheels(player);
        return VehicleCoreBlock.activateVehicle(serverWorld, player, corner1, corner2, customDriver, customDriverFacing, customPassengers, customWheels, VehicleWandItem.getPlaneSetup(player), player.getHorizontalFacing());
    }
}
