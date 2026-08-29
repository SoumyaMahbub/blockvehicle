package com.blockvehicle.client.sound;

import com.blockvehicle.entity.VehicleEntity;
import com.blockvehicle.sound.ModSounds;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.MathHelper;

@Environment(value=EnvType.CLIENT)
public class VehicleEngineSoundInstance
extends MovingSoundInstance {
    private final VehicleEntity vehicle;
    private final EngineProfile profile;
    private int fadeOutTicks = 0;

    public VehicleEngineSoundInstance(VehicleEntity vehicle) {
        super(soundFor(profileFor(vehicle)), SoundCategory.NEUTRAL, SoundInstance.createRandom());
        this.vehicle = vehicle;
        this.profile = profileFor(vehicle);
        this.repeat = true;
        this.repeatDelay = 0;
        this.volume = 0.0f;
        this.pitch = this.profile.basePitch;
        this.attenuationType = SoundInstance.AttenuationType.LINEAR;
        this.x = vehicle.getX();
        this.y = vehicle.getY();
        this.z = vehicle.getZ();
    }

    public void tick() {
        if (this.vehicle.isRemoved() || !this.vehicle.isAlive() || this.isTooFarAway()) {
            this.setDone();
            return;
        }
        this.x = this.vehicle.getX();
        this.y = this.vehicle.getY();
        this.z = this.vehicle.getZ();
        boolean hasDriver = this.vehicle.hasDriver();
        if (!hasDriver) {
            ++this.fadeOutTicks;
            this.volume = Math.max(0.0f, this.volume - 0.018f);
            if (this.volume <= 0.002f || this.fadeOutTicks > 36) {
                this.setDone();
            }
            return;
        }
        this.fadeOutTicks = 0;
        float speedFrac = this.vehicle.isAircraft() ? this.vehicle.getEngineRpm()
            : Math.min(Math.abs(this.vehicle.getSpeed()) / 0.85f, 1.0f);
        boolean isThrottling = this.vehicle.isHelicopter() ? this.vehicle.getInputState().brake
            : this.vehicle.getInputState().forward || this.vehicle.getInputState().backward;
        float targetPitch = this.profile.basePitch + speedFrac * this.profile.pitchRange
            + (isThrottling ? this.profile.throttlePitch : 0.0f);
        float targetVolume = this.profile.baseVolume + speedFrac * this.profile.volumeRange
            + (isThrottling ? 0.018f : 0.0f);
        this.pitch = MathHelper.lerp(0.075f, this.pitch, targetPitch);
        this.volume = MathHelper.lerp(0.055f, this.volume, targetVolume);
    }

    private boolean isTooFarAway() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.player.getVehicle() == this.vehicle) return false;
        return client.getCameraEntity() != null
            && client.getCameraEntity().squaredDistanceTo(this.vehicle) > 128.0 * 128.0;
    }

    private static EngineProfile profileFor(VehicleEntity vehicle) {
        if (vehicle.isHelicopter()) return EngineProfile.HELICOPTER;
        if (vehicle.isPlane()) return EngineProfile.PLANE;
        int wheels = vehicle.getStructure() != null ? vehicle.getStructure().getWheels().size() : 4;
        if (wheels >= 2 && wheels <= 3) return EngineProfile.BIKE;
        if (wheels > 4) return EngineProfile.HEAVY;
        return EngineProfile.CAR;
    }

    private static SoundEvent soundFor(EngineProfile profile) {
        return switch (profile) {
            case BIKE -> ModSounds.BIKE_ENGINE;
            case CAR -> ModSounds.CAR_ENGINE_LOOP;
            case HEAVY -> ModSounds.HEAVY_ENGINE;
            case PLANE -> ModSounds.PLANE_ENGINE;
            case HELICOPTER -> ModSounds.HELICOPTER_ROTOR;
        };
    }

    private enum EngineProfile {
        BIKE(0.94f, 0.56f, 0.07f, 0.085f, 0.13f),
        CAR(0.82f, 0.43f, 0.05f, 0.095f, 0.13f),
        HEAVY(0.72f, 0.31f, 0.035f, 0.11f, 0.14f),
        PLANE(0.78f, 0.47f, 0.04f, 0.105f, 0.15f),
        HELICOPTER(0.72f, 0.28f, 0.035f, 0.10f, 0.145f);

        private final float basePitch;
        private final float pitchRange;
        private final float throttlePitch;
        private final float baseVolume;
        private final float volumeRange;

        EngineProfile(float basePitch, float pitchRange, float throttlePitch,
                      float baseVolume, float volumeRange) {
            this.basePitch = basePitch;
            this.pitchRange = pitchRange;
            this.throttlePitch = throttlePitch;
            this.baseVolume = baseVolume;
            this.volumeRange = volumeRange;
        }
    }
}
