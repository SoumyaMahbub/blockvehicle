package com.blockvehicle.client.sound;

import com.blockvehicle.entity.VehicleEntity;
import com.blockvehicle.sound.ModSounds;
import com.blockvehicle.vehicle.VehicleInputState;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.sound.SoundCategory;

@Environment(value=EnvType.CLIENT)
public final class VehicleSoundManager {
    private static final Map<Integer, EngineLoops> activeEngineLoops = new HashMap<>();
    private static final Map<Integer, Boolean> lastDriverState = new HashMap<Integer, Boolean>();
    private static final Map<Integer, VehicleTireSoundInstance> activeBrakeLoops = new HashMap<>();
    private static final Map<Integer, VehicleTireSoundInstance> activeDriftLoops = new HashMap<>();
    private static final Map<Integer, PlaneWindSoundInstance> activeWindLoops = new HashMap<>();
    private static final Map<Integer, Integer> stallCooldowns = new HashMap<>();
    private static int scanCooldown = 0;

    private VehicleSoundManager() {
    }

    public static void onClientTick(MinecraftClient client) {
        ClientWorld world = client.world;
        if (world == null) {
            activeEngineLoops.clear();
            lastDriverState.clear();
            activeBrakeLoops.clear();
            activeDriftLoops.clear();
            activeWindLoops.clear();
            stallCooldowns.clear();
            scanCooldown = 0;
            return;
        }
        if (++scanCooldown < 4) {
            return;
        }
        scanCooldown = 0;
        for (Entity entity : world.getEntities()) {
            if (!(entity instanceof VehicleEntity)) continue;
            VehicleEntity vehicle = (VehicleEntity)entity;
            int id2 = vehicle.getId();
            boolean audible = isAudible(client, vehicle);
            boolean hasDriver = vehicle.hasDriver();
            boolean engineCapable = !vehicle.isAircraft() || vehicle.getStructure() != null
                && vehicle.getStructure().getPlaneDefinition() != null
                && vehicle.getStructure().getPlaneDefinition().hasEngines();
            boolean hadDriver = lastDriverState.getOrDefault(id2, false);
            if (audible && hasDriver && !hadDriver) {
                world.playSound(vehicle.getX(), vehicle.getY(), vehicle.getZ(), ModSounds.CAR_DOOR_CLOSE,
                    SoundCategory.NEUTRAL, 0.24f, 1.0f, false);
                if (engineCapable) world.playSound(vehicle.getX(), vehicle.getY(), vehicle.getZ(),
                    ModSounds.ENGINE_START, SoundCategory.NEUTRAL, 0.30f, 1.0f, false);
            } else if (audible && !hasDriver && hadDriver) {
                if (engineCapable) world.playSound(vehicle.getX(), vehicle.getY(), vehicle.getZ(),
                    ModSounds.ENGINE_STOP, SoundCategory.NEUTRAL, 0.27f, 1.0f, false);
                world.playSound(vehicle.getX(), vehicle.getY(), vehicle.getZ(), ModSounds.CAR_DOOR_OPEN,
                    SoundCategory.NEUTRAL, 0.22f, 1.0f, false);
            }
            lastDriverState.put(id2, hasDriver);
            EngineLoops loops = activeEngineLoops.get(id2);
            if (audible && hasDriver && engineCapable && (loops == null || loops.isDone())) {
                VehicleEngineSoundInstance engine = new VehicleEngineSoundInstance(vehicle);
                activeEngineLoops.put(id2, new EngineLoops(engine));
                client.getSoundManager().play((SoundInstance)engine);
            }
            if (vehicle.isAircraft()) {
                PlaneWindSoundInstance wind = activeWindLoops.get(id2);
                if (audible && vehicle.getPlaneVelocity().lengthSquared() > 0.01 && (wind == null || wind.isDone())) {
                    wind = new PlaneWindSoundInstance(vehicle);
                    activeWindLoops.put(id2, wind);
                    client.getSoundManager().play(wind);
                }
                int stallCd = Math.max(0, stallCooldowns.getOrDefault(id2, 0) - 4);
                if (client.player != null && vehicle == client.player.getVehicle()
                    && vehicle.getStallAmount() > 0.58f && stallCd == 0) {
                    world.playSound(vehicle.getX(), vehicle.getY(), vehicle.getZ(),
                        net.minecraft.sound.SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), SoundCategory.NEUTRAL,
                        0.14f, 1.65f, false);
                    stallCd = 20;
                }
                stallCooldowns.put(id2, stallCd);
            }
            if (!hasDriver || vehicle.isAircraft()) continue;
            VehicleInputState input = vehicle.getInputState();
            float speed = Math.abs(vehicle.getSpeed());
            boolean isBraking = input.brake && speed > 0.045f;
            boolean isDrifting = vehicle.getDriftAmount() > 0.12f
                || (input.left || input.right) && speed > 0.38f;
            VehicleTireSoundInstance brake = activeBrakeLoops.get(id2);
            if (audible && isBraking && (brake == null || brake.isDone())) {
                brake = new VehicleTireSoundInstance(vehicle, VehicleTireSoundInstance.Mode.BRAKE);
                activeBrakeLoops.put(id2, brake);
                client.getSoundManager().play(brake);
            }
            VehicleTireSoundInstance drift = activeDriftLoops.get(id2);
            if (audible && isDrifting && (drift == null || drift.isDone())) {
                drift = new VehicleTireSoundInstance(vehicle, VehicleTireSoundInstance.Mode.DRIFT);
                activeDriftLoops.put(id2, drift);
                client.getSoundManager().play(drift);
            }
        }
        lastDriverState.keySet().removeIf(id -> world.getEntityById(id.intValue()) == null);
        activeEngineLoops.entrySet().removeIf(entry -> world.getEntityById(entry.getKey()) == null || entry.getValue().isDone());
        activeBrakeLoops.entrySet().removeIf(entry -> world.getEntityById(entry.getKey()) == null || entry.getValue().isDone());
        activeDriftLoops.entrySet().removeIf(entry -> world.getEntityById(entry.getKey()) == null || entry.getValue().isDone());
        activeWindLoops.entrySet().removeIf(entry -> world.getEntityById(entry.getKey()) == null || entry.getValue().isDone());
        stallCooldowns.keySet().removeIf(id -> world.getEntityById(id.intValue()) == null);
    }

    private static boolean isAudible(MinecraftClient client, VehicleEntity vehicle) {
        if (client.player != null && client.player.getVehicle() == vehicle) return true;
        return client.getCameraEntity() == null
            || client.getCameraEntity().squaredDistanceTo(vehicle) <= 112.0 * 112.0;
    }

    private record EngineLoops(VehicleEngineSoundInstance engine) {
        private boolean isDone() {
            return this.engine.isDone();
        }
    }
}
