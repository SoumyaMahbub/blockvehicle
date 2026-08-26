package com.blockvehicle.client.sound;

import com.blockvehicle.client.sound.VehicleEngineSoundInstance;
import com.blockvehicle.client.sound.VehicleIdleSoundInstance;
import com.blockvehicle.entity.VehicleEntity;
import com.blockvehicle.sound.ModSounds;
import com.blockvehicle.vehicle.VehicleInputState;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.sound.SoundCategory;

@Environment(value=EnvType.CLIENT)
public final class VehicleSoundManager {
    private static final Set<Integer> playingVehicleIds = new HashSet<Integer>();
    private static final Map<Integer, Boolean> lastDriverState = new HashMap<Integer, Boolean>();
    private static final Map<Integer, Integer> skidCooldowns = new HashMap<Integer, Integer>();

    private VehicleSoundManager() {
    }

    public static void onClientTick(MinecraftClient client) {
        ClientWorld world = client.world;
        if (world == null) {
            playingVehicleIds.clear();
            lastDriverState.clear();
            skidCooldowns.clear();
            return;
        }
        for (Entity entity : world.getEntities()) {
            boolean isHardTurning;
            if (!(entity instanceof VehicleEntity)) continue;
            VehicleEntity vehicle = (VehicleEntity)entity;
            int id2 = vehicle.getId();
            boolean hasDriver = vehicle.hasDriver();
            boolean hadDriver = lastDriverState.getOrDefault(id2, false);
            if (hasDriver && !hadDriver) {
                world.playSound(vehicle.getX(), vehicle.getY(), vehicle.getZ(), ModSounds.CAR_DOOR_CLOSE, SoundCategory.NEUTRAL, 0.85f, 1.0f, false);
                world.playSound(vehicle.getX(), vehicle.getY(), vehicle.getZ(), ModSounds.ENGINE_START, SoundCategory.NEUTRAL, 0.9f, 1.0f, false);
            } else if (!hasDriver && hadDriver) {
                world.playSound(vehicle.getX(), vehicle.getY(), vehicle.getZ(), ModSounds.ENGINE_STOP, SoundCategory.NEUTRAL, 0.8f, 1.0f, false);
                world.playSound(vehicle.getX(), vehicle.getY(), vehicle.getZ(), ModSounds.CAR_DOOR_OPEN, SoundCategory.NEUTRAL, 0.75f, 1.0f, false);
            }
            lastDriverState.put(id2, hasDriver);
            if (hasDriver && !playingVehicleIds.contains(id2)) {
                playingVehicleIds.add(id2);
                client.getSoundManager().play((SoundInstance)new VehicleEngineSoundInstance(vehicle));
                client.getSoundManager().play((SoundInstance)new VehicleIdleSoundInstance(vehicle));
            } else if (!hasDriver) {
                playingVehicleIds.remove(id2);
            }
            if (!hasDriver) continue;
            VehicleInputState input = vehicle.getInputState();
            float speed = Math.abs(vehicle.getSpeed());
            int cd = skidCooldowns.getOrDefault(id2, 0);
            if (cd > 0) {
                skidCooldowns.put(id2, cd - 1);
                continue;
            }
            boolean isBraking = input.brake && speed > 0.08f;
            boolean bl = isHardTurning = (input.left || input.right) && speed > 0.22f;
            if (!isBraking && !isHardTurning) continue;
            float pitch = 0.95f + speed * 0.5f;
            float vol = Math.min(0.9f, 0.4f + speed * 1.2f);
            world.playSound(vehicle.getX(), vehicle.getY(), vehicle.getZ(), ModSounds.TIRE_SKID, SoundCategory.NEUTRAL, vol, pitch, false);
            skidCooldowns.put(id2, 14);
        }
        lastDriverState.keySet().removeIf(id -> world.getEntityById(id.intValue()) == null);
        playingVehicleIds.removeIf(id -> world.getEntityById(id.intValue()) == null);
        skidCooldowns.keySet().removeIf(id -> world.getEntityById(id.intValue()) == null);
    }
}

