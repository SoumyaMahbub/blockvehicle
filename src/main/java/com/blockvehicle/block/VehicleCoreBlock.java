package com.blockvehicle.block;

import com.blockvehicle.ModItems;
import com.blockvehicle.entity.VehicleEntity;
import com.blockvehicle.item.VehicleWandItem;
import com.blockvehicle.vehicle.ActivationConfirmManager;
import com.blockvehicle.vehicle.VehicleActivator;
import com.blockvehicle.vehicle.VehicleStructure;
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
        ActivationConfirmManager.requestConfirmation(serverWorld, player, corner1, corner2, customDriver, customDriverFacing, customPassengers, customWheels);
    }

    public static boolean activateVehicle(ServerWorld serverWorld, PlayerEntity player, BlockPos corner1, BlockPos corner2, BlockPos customDriver, Direction customDriverFacing, Set<BlockPos> customPassengers, Set<BlockPos> customWheels, Direction playerFacing) {
        VehicleStructure structure = VehicleActivator.activate(serverWorld, corner1, corner2, customDriver, customDriverFacing, customPassengers, customWheels, playerFacing);
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
        player.sendMessage((Text)Text.literal(("\u00a7aVehicle activated! \u00a77(" + structure.getBlocks().size() + " blocks, " + structure.getSeats().size() + " seats, " + structure.getWheels().size() + " wheels). Right-click vehicle to drive.")), false);
        return true;
    }

    public static boolean activateVehicle(ServerWorld serverWorld, PlayerEntity player, BlockPos corner1, BlockPos corner2, BlockPos customDriver, Direction customDriverFacing, Set<BlockPos> customPassengers, Direction playerFacing) {
        return VehicleCoreBlock.activateVehicle(serverWorld, player, corner1, corner2, customDriver, customDriverFacing, customPassengers, Set.of(), playerFacing);
    }

    public static boolean activateVehicle(ServerWorld serverWorld, PlayerEntity player, BlockPos corner1, BlockPos corner2) {
        BlockPos customDriver = VehicleWandItem.getDriverSeat(player);
        Direction customDriverFacing = VehicleWandItem.getDriverFacing(player);
        Set<BlockPos> customPassengers = VehicleWandItem.getPassengerSeats(player);
        Set<BlockPos> customWheels = VehicleWandItem.getCustomWheels(player);
        return VehicleCoreBlock.activateVehicle(serverWorld, player, corner1, corner2, customDriver, customDriverFacing, customPassengers, customWheels, player.getHorizontalFacing());
    }
}

