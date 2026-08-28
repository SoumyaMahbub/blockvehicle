package com.blockvehicle.command;

import com.blockvehicle.entity.VehicleEntity;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.List;
import java.util.Optional;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class RemoveVehicleCommand {
    private static final double REACH = 5.0;

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register((LiteralArgumentBuilder<ServerCommandSource>)CommandManager.literal("removevehicle")
            .executes(context -> execute(context, false))
            .then(CommandManager.literal("force")
                .requires(source -> source.hasPermissionLevel(2))
                .executes(context -> execute(context, true))));
    }

    private static int execute(CommandContext<ServerCommandSource> context, boolean force) {
        ServerCommandSource source = (ServerCommandSource)context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError((Text)Text.literal("This command can only be used by a player."));
            return 0;
        }
        ServerWorld world = player.getServerWorld();
        VehicleEntity target = RemoveVehicleCommand.findTargetVehicle(player, world);
        if (target == null) {
            player.sendMessage((Text)Text.literal("\u00a7cNo vehicle found! Look at a vehicle and try again."), true);
            return 0;
        }
        target.dismantleBackToBlocks(world, (PlayerEntity)player, force);
        return 1;
    }

    private static VehicleEntity findTargetVehicle(ServerPlayerEntity player, ServerWorld world) {
        Vec3d eyePos = player.getEyePos();
        Vec3d lookDir = player.getRotationVec(1.0f);
        Vec3d reachEnd = eyePos.add(lookDir.multiply(5.0));
        Box searchBox = new Box(eyePos, reachEnd).expand(1.0);
        List<VehicleEntity> vehicles = world.getEntitiesByClass(VehicleEntity.class, searchBox, v -> !v.isRemoved());
        VehicleEntity closest = null;
        double closestDist = Double.MAX_VALUE;
        for (VehicleEntity vehicle : vehicles) {
            double dist;
            Box vehicleBox = vehicle.getBoundingBox().expand(0.3);
            Optional hit = vehicleBox.raycast(eyePos, reachEnd);
            if (!hit.isPresent() || !((dist = eyePos.squaredDistanceTo((Vec3d)hit.get())) < closestDist)) continue;
            closestDist = dist;
            closest = vehicle;
        }
        return closest;
    }
}
