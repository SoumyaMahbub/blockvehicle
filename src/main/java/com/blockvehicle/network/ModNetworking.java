package com.blockvehicle.network;

import com.blockvehicle.BlockVehicleMod;
import com.blockvehicle.entity.VehicleEntity;
import com.blockvehicle.network.VehicleHornPayload;
import com.blockvehicle.network.VehicleInputPayload;
import com.blockvehicle.network.VehicleSyncPayload;
import com.blockvehicle.network.WandSyncPayload;
import com.blockvehicle.sound.ModSounds;
import com.blockvehicle.vehicle.VehicleInputState;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;

public class ModNetworking {
    public static void registerServerHandlers() {
        PayloadTypeRegistry.playC2S().register(VehicleInputPayload.ID, VehicleInputPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(VehicleHornPayload.ID, VehicleHornPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(VehicleSyncPayload.ID, VehicleSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(WandSyncPayload.ID, WandSyncPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(VehicleHornPayload.ID, (payload, context) -> context.server().execute(() -> {
            VehicleEntity ve;
            ServerPlayerEntity player = context.player();
            Entity vehicle = player.getVehicle();
            if (vehicle instanceof VehicleEntity && (ve = (VehicleEntity)vehicle).getId() == payload.vehicleId()) {
                player.getServerWorld().playSound(null, ve.getX(), ve.getY(), ve.getZ(), ModSounds.CAR_HORN, SoundCategory.PLAYERS, 1.2f, 1.0f);
            }
        }));
        ServerPlayNetworking.registerGlobalReceiver(VehicleInputPayload.ID, (payload, context) -> context.server().execute(() -> {
            ServerPlayerEntity player = context.player();
            Entity vehicle = player.getVehicle();
            if (vehicle instanceof VehicleEntity) {
                VehicleEntity ve = (VehicleEntity)vehicle;
                ve.setInputState(new VehicleInputState(payload.forward(), payload.backward(), payload.left(), payload.right(), payload.brake()));
                ve.applyClientDriverUpdate(payload.x(), payload.y(), payload.z(), payload.yaw(), payload.speed(), payload.pitch(), payload.roll());
            }
        }));
        BlockVehicleMod.LOGGER.info("ModNetworking: server handlers registered.");
    }
}

