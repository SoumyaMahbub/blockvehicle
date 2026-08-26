package com.blockvehicle.item;

import com.blockvehicle.ModBlocks;
import com.blockvehicle.block.VehicleCoreBlock;
import com.blockvehicle.item.PlayerDataStore;
import com.blockvehicle.vehicle.ActivationConfirmManager;
import com.blockvehicle.vehicle.VehicleActivator;
import java.util.List;
import java.util.Set;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class VehicleWandItem
extends Item {
    public VehicleWandItem(Item.Settings settings) {
        super(settings);
    }

    public ActionResult useOnBlock(ItemUsageContext context) {
        ServerPlayerEntity spe;
        World world = context.getWorld();
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }
        PlayerEntity player = context.getPlayer();
        if (player == null) {
            return ActionResult.PASS;
        }
        BlockPos pos = context.getBlockPos();
        BlockState state = world.getBlockState(pos);
        PlayerDataStore.WandMode mode = PlayerDataStore.getMode(player.getUuid());
        if (state.isOf(ModBlocks.VEHICLE_CORE) && !player.isSneaking()) {
            VehicleWandItem.tryActivate(world, player);
            return ActionResult.SUCCESS;
        }
        switch (mode) {
            case SELECT_REGION: {
                if (player.isSneaking()) {
                    VehicleWandItem.setCorner2(player, pos);
                    player.sendMessage((Text)Text.literal(("\u00a7bCorner 2 \u00a7aset to \u00a7f" + pos.toShortString())), true);
                    world.playSound(null, pos, SoundEvents.BLOCK_AMETHYST_BLOCK_HIT, SoundCategory.PLAYERS, 0.8f, 1.3f);
                    break;
                }
                VehicleWandItem.setCorner1(player, pos);
                player.sendMessage((Text)Text.literal(("\u00a7bCorner 1 \u00a7aset to \u00a7f" + pos.toShortString())), true);
                world.playSound(null, pos, SoundEvents.BLOCK_AMETHYST_BLOCK_HIT, SoundCategory.PLAYERS, 0.8f, 1.0f);
                break;
            }
            case SET_DRIVER_SEAT: {
                Direction facing = VehicleActivator.getBlockHorizontalFacing(state, player.getHorizontalFacing());
                PlayerDataStore.setDriverSeat(player.getUuid(), pos, facing);
                player.sendMessage((Text)Text.literal(("\u00a76\ud83d\udcba Driver Seat \u00a7aset to \u00a7f" + pos.toShortString() + " \u00a77(Facing: \u00a7e" + facing.asString().toUpperCase() + "\u00a77)")), true);
                world.playSound(null, pos, (SoundEvent)SoundEvents.ITEM_ARMOR_EQUIP_LEATHER.value(), SoundCategory.PLAYERS, 0.9f, 1.0f);
                break;
            }
            case SET_PASSENGER_SEAT: {
                boolean added = PlayerDataStore.togglePassengerSeat(player.getUuid(), pos);
                int total = PlayerDataStore.getPassengerSeats(player.getUuid()).size();
                if (added) {
                    player.sendMessage((Text)Text.literal(("\u00a7d\ud83d\udc65 Passenger Seat \u00a7aadded at \u00a7f" + pos.toShortString() + " \u00a77(Total: " + total + ")")), true);
                    world.playSound(null, pos, SoundEvents.ITEM_BUNDLE_INSERT, SoundCategory.PLAYERS, 0.9f, 1.2f);
                    break;
                }
                player.sendMessage((Text)Text.literal(("\u00a7cPassenger Seat \u00a7eremoved from \u00a7f" + pos.toShortString() + " \u00a77(Total: " + total + ")")), true);
                world.playSound(null, pos, SoundEvents.ITEM_BUNDLE_REMOVE_ONE, SoundCategory.PLAYERS, 0.9f, 0.9f);
                break;
            }
            case SET_WHEEL: {
                boolean added = PlayerDataStore.toggleWheel(player.getUuid(), pos);
                int total = PlayerDataStore.getCustomWheels(player.getUuid()).size();
                if (added) {
                    player.sendMessage((Text)Text.literal(("\u00a7e\ud83d\ude97 Wheel \u00a7aadded at \u00a7f" + pos.toShortString() + " \u00a77(Total: " + total + ")")), true);
                    world.playSound(null, pos, (SoundEvent)SoundEvents.ITEM_ARMOR_EQUIP_IRON.value(), SoundCategory.PLAYERS, 0.9f, 1.1f);
                    break;
                }
                player.sendMessage((Text)Text.literal(("\u00a7cWheel \u00a7eremoved from \u00a7f" + pos.toShortString() + " \u00a77(Total: " + total + ")")), true);
                world.playSound(null, pos, SoundEvents.BLOCK_GRINDSTONE_USE, SoundCategory.PLAYERS, 0.8f, 1.2f);
                break;
            }
            case ACTIVATE: {
                if (player instanceof ServerPlayerEntity && ActivationConfirmManager.hasPending((spe = (ServerPlayerEntity)player).getUuid())) {
                    ActivationConfirmManager.confirmNow(spe);
                    break;
                }
                VehicleWandItem.tryActivate(world, player);
            }
        }
        if (player instanceof ServerPlayerEntity) {
            spe = (ServerPlayerEntity)player;
            PlayerDataStore.syncToPlayer(spe);
        }
        return ActionResult.SUCCESS;
    }

    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ServerPlayerEntity spe;
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }
        if (user.isSneaking()) {
            PlayerDataStore.WandMode next = PlayerDataStore.cycleMode(user.getUuid());
            user.sendMessage((Text)Text.literal((next.title + " \u00a77\u2014 " + next.hint)), true);
            world.playSound(null, user.getX(), user.getY(), user.getZ(), (SoundEvent)SoundEvents.UI_BUTTON_CLICK.value(), SoundCategory.PLAYERS, 0.7f, 1.4f);
            if (user instanceof ServerPlayerEntity) {
                ServerPlayerEntity spe2 = (ServerPlayerEntity)user;
                PlayerDataStore.syncToPlayer(spe2);
            }
            return ActionResult.SUCCESS;
        }
        PlayerDataStore.WandMode mode = PlayerDataStore.getMode(user.getUuid());
        if (mode == PlayerDataStore.WandMode.ACTIVATE) {
            if (user instanceof ServerPlayerEntity && ActivationConfirmManager.hasPending((spe = (ServerPlayerEntity)user).getUuid())) {
                ActivationConfirmManager.confirmNow(spe);
            } else {
                VehicleWandItem.tryActivate(world, user);
            }
        } else {
            this.showStatus(user);
        }
        if (user instanceof ServerPlayerEntity) {
            spe = (ServerPlayerEntity)user;
            PlayerDataStore.syncToPlayer(spe);
        }
        return ActionResult.SUCCESS;
    }

    public static boolean tryActivate(World world, PlayerEntity player) {
        if (!(world instanceof ServerWorld)) {
            return true;
        }
        ServerWorld sw = (ServerWorld)world;
        BlockPos c1 = VehicleWandItem.getCorner1(player);
        BlockPos c2 = VehicleWandItem.getCorner2(player);
        if (c1 == null || c2 == null) {
            player.sendMessage((Text)Text.literal("\u00a7eSet both \u00a7bCorner 1 \u00a7eand \u00a7bCorner 2 \u00a7ebefore activating!"), true);
            player.sendMessage((Text)Text.literal("\u00a77(Switch to \u00a7b\ud83d\udcd0 Select Region \u00a77mode: Sneak + Right-Click in air)"), false);
            return false;
        }
        VehicleCoreBlock.requestActivationConfirmation(sw, player, c1, c2);
        return true;
    }

    private void showStatus(PlayerEntity player) {
        PlayerDataStore.WandMode mode = PlayerDataStore.getMode(player.getUuid());
        BlockPos c1 = VehicleWandItem.getCorner1(player);
        BlockPos c2 = VehicleWandItem.getCorner2(player);
        BlockPos driver = VehicleWandItem.getDriverSeat(player);
        Direction driverFacing = VehicleWandItem.getDriverFacing(player);
        int passengerCount = VehicleWandItem.getPassengerSeats(player).size();
        int wheelCount = VehicleWandItem.getCustomWheels(player).size();
        String c1str = c1 != null ? "\u00a7a" + c1.toShortString() : "\u00a7cnot set";
        String c2str = c2 != null ? "\u00a7a" + c2.toShortString() : "\u00a7cnot set";
        String driverStr = driver != null ? "\u00a7a" + driver.toShortString() + " (" + String.valueOf(driverFacing) + ")" : "\u00a77auto (stairs/center)";
        Object wheelStr = wheelCount > 0 ? "\u00a7e" + wheelCount : "\u00a77auto (bottom black blocks)";
        player.sendMessage((Text)Text.literal("\u00a76\u2500\u2500\u2500 Vehicle Wand Status \u2500\u2500\u2500"), false);
        player.sendMessage((Text)Text.literal(("\u00a77Current Mode: " + mode.title)), false);
        player.sendMessage((Text)Text.literal(("\u00a77Region: Corner 1: " + c1str + " \u00a77| Corner 2: " + c2str)), false);
        player.sendMessage((Text)Text.literal(("\u00a77Driver Seat: " + driverStr + " \u00a77| Passenger Seats: \u00a7d" + passengerCount)), false);
        player.sendMessage((Text)Text.literal(("\u00a77Wheels: " + wheelStr)), false);
        if (c1 != null && c2 != null) {
            player.sendMessage((Text)Text.literal("\u00a7a\u2714 Ready to activate! Switch to \u00a7a\u26a1 Activate Vehicle \u00a7amode and right-click!"), false);
        } else {
            player.sendMessage((Text)Text.literal("\u00a77Tip: Sneak + Right-Click in air to cycle Wand modes."), false);
        }
    }

    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add((Text)Text.literal("\u00a77Tool for selecting, designing, and activating vehicles."));
        tooltip.add((Text)Text.literal(""));
        tooltip.add((Text)Text.literal("\u00a76Modes (Sneak + Right-Click):"));
        tooltip.add((Text)Text.literal("  \u00a7b\ud83d\udcd0 Select Region \u00a77(C1 / C2)"));
        tooltip.add((Text)Text.literal("  \u00a76\ud83d\udcba Set Driver Seat"));
        tooltip.add((Text)Text.literal("  \u00a7d\ud83d\udc65 Passenger Seats"));
        tooltip.add((Text)Text.literal("  \u00a7e\ud83d\ude97 Custom Wheels"));
        tooltip.add((Text)Text.literal("  \u00a7a\u26a1 Activate Vehicle"));
        tooltip.add((Text)Text.literal(""));
        tooltip.add((Text)Text.literal("\u00a78HUD overlay & 3D outlines active while holding."));
    }

    public static void setCorner1(PlayerEntity player, BlockPos pos) {
        PlayerDataStore.setCorner1(player.getUuid(), pos);
        if (player instanceof ServerPlayerEntity) {
            ServerPlayerEntity spe = (ServerPlayerEntity)player;
            PlayerDataStore.syncToPlayer(spe);
        }
    }

    public static void setCorner2(PlayerEntity player, BlockPos pos) {
        PlayerDataStore.setCorner2(player.getUuid(), pos);
        if (player instanceof ServerPlayerEntity) {
            ServerPlayerEntity spe = (ServerPlayerEntity)player;
            PlayerDataStore.syncToPlayer(spe);
        }
    }

    public static BlockPos getCorner1(PlayerEntity player) {
        return PlayerDataStore.getCorner1(player.getUuid());
    }

    public static BlockPos getCorner2(PlayerEntity player) {
        return PlayerDataStore.getCorner2(player.getUuid());
    }

    public static BlockPos getDriverSeat(PlayerEntity player) {
        return PlayerDataStore.getDriverSeat(player.getUuid());
    }

    public static Direction getDriverFacing(PlayerEntity player) {
        return PlayerDataStore.getDriverFacing(player.getUuid());
    }

    public static Set<BlockPos> getPassengerSeats(PlayerEntity player) {
        return PlayerDataStore.getPassengerSeats(player.getUuid());
    }

    public static Set<BlockPos> getCustomWheels(PlayerEntity player) {
        return PlayerDataStore.getCustomWheels(player.getUuid());
    }

    public static void clearCorners(PlayerEntity player) {
        PlayerDataStore.clear(player.getUuid());
        if (player instanceof ServerPlayerEntity) {
            ServerPlayerEntity spe = (ServerPlayerEntity)player;
            PlayerDataStore.syncToPlayer(spe);
        }
    }
}

