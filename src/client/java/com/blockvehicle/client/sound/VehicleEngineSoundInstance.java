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
    private float smoothedRpm = 0.0f;

    public VehicleEngineSoundInstance(VehicleEntity vehicle) {
        super(soundFor(profileFor(vehicle)), SoundCategory.NEUTRAL, SoundInstance.createRandom());
        this.vehicle = vehicle;
        this.profile = profileFor(vehicle);
        this.repeat = true;
        this.repeatDelay = 0;
        this.volume = 0.0f;
        this.pitch = this.profile.basePitch;
        MinecraftClient client = MinecraftClient.getInstance();
        this.attenuationType = client.player != null && client.player.getVehicle() == vehicle
            ? SoundInstance.AttenuationType.NONE : SoundInstance.AttenuationType.LINEAR;
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
        boolean isThrottling = this.vehicle.isHelicopter() ? this.vehicle.getInputState().brake
            : this.vehicle.getInputState().forward || this.vehicle.getInputState().backward;
        float rawRpm = this.vehicle.isAircraft() ? this.vehicle.getEngineRpm()
            : MathHelper.clamp(Math.abs(this.vehicle.getSpeed()) / 0.85f
                + (isThrottling ? 0.14f : 0.0f), 0.0f, 1.0f);
        this.smoothedRpm = MathHelper.lerp(rawRpm > this.smoothedRpm ? 0.10f : 0.055f,
            this.smoothedRpm, rawRpm);
        float targetPitch = this.profile.basePitch + this.smoothedRpm * this.profile.pitchRange
            + (isThrottling ? this.profile.throttlePitch : 0.0f);
        float targetVolume = this.profile.baseVolume + this.smoothedRpm * this.profile.volumeRange
            + (isThrottling ? 0.025f : 0.0f);
        this.pitch = MathHelper.lerp(0.085f, this.pitch, targetPitch);
        this.volume = MathHelper.lerp(0.075f, this.volume, targetVolume);
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
        BIKE(0.94f, 0.48f, 0.055f, 0.20f, 0.18f),
        CAR(0.80f, 0.40f, 0.045f, 0.21f, 0.18f),
        HEAVY(0.69f, 0.29f, 0.030f, 0.23f, 0.19f),
        PLANE(0.77f, 0.40f, 0.035f, 0.23f, 0.20f),
        HELICOPTER(0.70f, 0.23f, 0.025f, 0.25f, 0.18f);

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
