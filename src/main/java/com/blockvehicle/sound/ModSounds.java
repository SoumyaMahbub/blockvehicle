package com.blockvehicle.sound;

import com.blockvehicle.BlockVehicleMod;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class ModSounds {
    public static final SoundEvent ENGINE_IDLE = ModSounds.register("engine_idle");
    public static final SoundEvent ENGINE_REV = ModSounds.register("engine_rev");
    public static final SoundEvent ENGINE_START = ModSounds.register("engine_start");
    public static final SoundEvent ENGINE_STOP = ModSounds.register("engine_stop");
    public static final SoundEvent CAR_HORN = ModSounds.register("car_horn");
    public static final SoundEvent TIRE_SKID = ModSounds.register("tire_skid");
    public static final SoundEvent CAR_IMPACT = ModSounds.register("car_impact");
    public static final SoundEvent CAR_DOOR_CLOSE = ModSounds.register("car_door_close");
    public static final SoundEvent CAR_DOOR_OPEN = ModSounds.register("car_door_open");

    private static SoundEvent register(String name) {
        Identifier id = Identifier.of("blockvehicle", name);
        return (SoundEvent)Registry.register((Registry)Registries.SOUND_EVENT, (Identifier)id, SoundEvent.of((Identifier)id));
    }

    public static void register() {
        BlockVehicleMod.LOGGER.info("Registering ModSounds for blockvehicle");
    }
}

