package com.blockvehicle.vehicle;

import com.blockvehicle.block.VehicleCoreBlock;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public final class ActivationConfirmManager {
    private static final long TIMEOUT_MS = 30000L;
    private static final Map<UUID, PendingActivation> pendingMap = new ConcurrentHashMap<UUID, PendingActivation>();

    private ActivationConfirmManager() {
    }

    public static void requestConfirmation(ServerWorld world, PlayerEntity player, BlockPos corner1, BlockPos corner2, BlockPos customDriverSeat, Direction customDriverFacing, Set<BlockPos> customPassengerSeats, Set<BlockPos> customWheels, PlaneSetup planeSetup) {
        UUID uuid = player.getUuid();
        pendingMap.put(uuid, new PendingActivation(world, corner1, corner2, customDriverSeat, customDriverFacing,
            Set.copyOf(customPassengerSeats), Set.copyOf(customWheels), planeSetup, player.getHorizontalFacing(), System.currentTimeMillis()));
        MutableText button = Text.literal("  \u00a7a\u00a7l[ \u26a1 CLICK HERE TO ACTIVATE VEHICLE ]\u00a7r").styled(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "confirm")).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("\u00a7aClick to instantly assemble and activate your vehicle!"))).withUnderline(Boolean.valueOf(true)));
        player.sendMessage((Text)Text.literal("\u00a76\u26a1 Vehicle Assembly Ready:"), false);
        player.sendMessage((Text)button, false);
        player.sendMessage((Text)Text.literal("\u00a77(Click button above, right-click with Wand, or type \u00a7aconfirm \u00a77in chat)"), false);
        if (player instanceof ServerPlayerEntity) {
            ServerPlayerEntity spe = (ServerPlayerEntity)player;
            spe.getServerWorld().playSound(null, spe.getX(), spe.getY(), spe.getZ(), SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 0.9f, 1.2f);
        }
    }

    public static boolean confirmNow(ServerPlayerEntity player) {
        return ActivationConfirmManager.onChatMessage(player, "confirm");
    }

    public static void requestConfirmation(ServerWorld world, PlayerEntity player, BlockPos corner1, BlockPos corner2, BlockPos customDriverSeat, Direction customDriverFacing, Set<BlockPos> customPassengerSeats) {
        ActivationConfirmManager.requestConfirmation(world, player, corner1, corner2, customDriverSeat, customDriverFacing, customPassengerSeats, Set.of(), PlaneSetup.GROUND);
    }

    public static boolean onChatMessage(ServerPlayerEntity player, String message) {
        if (!message.trim().equalsIgnoreCase("confirm")) {
            return false;
        }
        UUID uuid = player.getUuid();
        PendingActivation pending = pendingMap.remove(uuid);
        if (pending == null) {
            return false;
        }
        if (System.currentTimeMillis() - pending.timestamp > 30000L) {
            player.sendMessage((Text)Text.literal("\u00a7cActivation expired! Trigger activation again and type \u00a7lconfirm\u00a7c."), true);
            return true;
        }
        if (player.getServerWorld() != pending.world) {
            player.sendMessage(Text.literal("\u00a7cActivation cancelled because you changed dimensions."), true);
            return true;
        }
        double centerX = (pending.corner1.getX() + pending.corner2.getX() + 1.0) * 0.5;
        double centerY = (pending.corner1.getY() + pending.corner2.getY() + 1.0) * 0.5;
        double centerZ = (pending.corner1.getZ() + pending.corner2.getZ() + 1.0) * 0.5;
        if (player.squaredDistanceTo(centerX, centerY, centerZ) > 1024.0) {
            player.sendMessage(Text.literal("\u00a7cActivation cancelled because you moved too far from the build."), true);
            return true;
        }
        boolean success = VehicleCoreBlock.activateVehicle(pending.world, (PlayerEntity)player, pending.corner1, pending.corner2, pending.customDriverSeat, pending.customDriverFacing, pending.customPassengerSeats, pending.customWheels, pending.planeSetup, pending.playerFacing);
        if (!success) {
            player.sendMessage((Text)Text.literal("\u00a7cVehicle activation failed! Check your region selection."), true);
        }
        return true;
    }

    public static void tickCleanup() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, PendingActivation>> it = pendingMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, PendingActivation> entry = it.next();
            if (now - entry.getValue().timestamp <= 30000L) continue;
            it.remove();
        }
    }

    public static boolean hasPending(UUID uuid) {
        PendingActivation p = pendingMap.get(uuid);
        if (p == null) {
            return false;
        }
        if (System.currentTimeMillis() - p.timestamp > 30000L) {
            pendingMap.remove(uuid);
            return false;
        }
        return true;
    }

    public static void cancel(UUID uuid) {
        pendingMap.remove(uuid);
    }

    private record PendingActivation(ServerWorld world, BlockPos corner1, BlockPos corner2, BlockPos customDriverSeat, Direction customDriverFacing, Set<BlockPos> customPassengerSeats, Set<BlockPos> customWheels, PlaneSetup planeSetup, Direction playerFacing, long timestamp) {
    }
}
