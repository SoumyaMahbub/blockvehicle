package com.blockvehicle.client.sound;

import com.blockvehicle.client.sound.VehicleEngineSoundInstance;
import com.blockvehicle.client.sound.VehicleIdleSoundInstance;
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
    private static final Map<Integer, Integer> skidCooldowns = new HashMap<Integer, Integer>();
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
            skidCooldowns.clear();
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
            boolean isHardTurning;
            if (!(entity instanceof VehicleEntity)) continue;
            VehicleEntity vehicle = (VehicleEntity)entity;
            int id2 = vehicle.getId();
            boolean hasDriver = vehicle.hasDriver();
            boolean engineCapable = !vehicle.isPlane() || vehicle.getStructure() != null
                && vehicle.getStructure().getPlaneDefinition() != null
                && vehicle.getStructure().getPlaneDefinition().hasEngines();
            boolean hadDriver = lastDriverState.getOrDefault(id2, false);
            if (hasDriver && !hadDriver) {
                world.playSound(vehicle.getX(), vehicle.getY(), vehicle.getZ(), ModSounds.CAR_DOOR_CLOSE, SoundCategory.NEUTRAL, 0.85f, 1.0f, false);
                if (engineCapable) world.playSound(vehicle.getX(), vehicle.getY(), vehicle.getZ(), ModSounds.ENGINE_START, SoundCategory.NEUTRAL, 0.9f, 1.0f, false);
            } else if (!hasDriver && hadDriver) {
                if (engineCapable) world.playSound(vehicle.getX(), vehicle.getY(), vehicle.getZ(), ModSounds.ENGINE_STOP, SoundCategory.NEUTRAL, 0.8f, 1.0f, false);
                world.playSound(vehicle.getX(), vehicle.getY(), vehicle.getZ(), ModSounds.CAR_DOOR_OPEN, SoundCategory.NEUTRAL, 0.75f, 1.0f, false);
            }
            lastDriverState.put(id2, hasDriver);
            EngineLoops loops = activeEngineLoops.get(id2);
            if (hasDriver && engineCapable && (loops == null || loops.isDone())) {
                VehicleEngineSoundInstance engine = new VehicleEngineSoundInstance(vehicle);
                VehicleIdleSoundInstance idle = new VehicleIdleSoundInstance(vehicle);
                activeEngineLoops.put(id2, new EngineLoops(engine, idle));
                client.getSoundManager().play((SoundInstance)engine);
                client.getSoundManager().play((SoundInstance)idle);
            }
            if (vehicle.isPlane()) {
                PlaneWindSoundInstance wind = activeWindLoops.get(id2);
                if (vehicle.getPlaneVelocity().lengthSquared() > 0.01 && (wind == null || wind.isDone())) {
                    wind = new PlaneWindSoundInstance(vehicle);
                    activeWindLoops.put(id2, wind);
                    client.getSoundManager().play(wind);
                }
                int stallCd = Math.max(0, stallCooldowns.getOrDefault(id2, 0) - 4);
                if (vehicle == client.player.getVehicle() && vehicle.getStallAmount() > 0.58f && stallCd == 0) {
                    world.playSound(vehicle.getX(), vehicle.getY(), vehicle.getZ(),
                        net.minecraft.sound.SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), SoundCategory.NEUTRAL,
                        0.42f, 1.65f, false);
                    stallCd = 20;
                }
                stallCooldowns.put(id2, stallCd);
            }
            if (!hasDriver || vehicle.isPlane()) continue;
            VehicleInputState input = vehicle.getInputState();
            float speed = Math.abs(vehicle.getSpeed());
            int cd = skidCooldowns.getOrDefault(id2, 0);
            if (cd > 0) {
                skidCooldowns.put(id2, cd - 1);
                continue;
            }
            boolean isBraking = input.brake && speed > 0.08f;
            boolean isDrifting = vehicle.getDriftAmount() > 0.22f;
            boolean bl = isHardTurning = (input.left || input.right) && speed > 0.42f;
            if (!isBraking && !isDrifting && !isHardTurning) continue;
            float pitch = 0.95f + speed * 0.5f;
            float vol = Math.min(1.0f, 0.35f + speed * 0.9f + vehicle.getDriftAmount() * 0.3f);
            world.playSound(vehicle.getX(), vehicle.getY(), vehicle.getZ(), ModSounds.TIRE_SKID, SoundCategory.NEUTRAL, vol, pitch, false);
            skidCooldowns.put(id2, 4);
        }
        lastDriverState.keySet().removeIf(id -> world.getEntityById(id.intValue()) == null);
        activeEngineLoops.entrySet().removeIf(entry -> world.getEntityById(entry.getKey()) == null || entry.getValue().isDone());
        skidCooldowns.keySet().removeIf(id -> world.getEntityById(id.intValue()) == null);
        activeWindLoops.entrySet().removeIf(entry -> world.getEntityById(entry.getKey()) == null || entry.getValue().isDone());
        stallCooldowns.keySet().removeIf(id -> world.getEntityById(id.intValue()) == null);
    }

    private record EngineLoops(VehicleEngineSoundInstance engine, VehicleIdleSoundInstance idle) {
        private boolean isDone() {
            return this.engine.isDone() && this.idle.isDone();
        }
    }
}
