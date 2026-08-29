package com.blockvehicle.client.sound;

import com.blockvehicle.entity.VehicleEntity;
import com.blockvehicle.sound.ModSounds;
import com.blockvehicle.vehicle.VehicleInputState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.MathHelper;

/** Smooth continuous tire friction layer; replaces repeated one-shot chatter. */
@Environment(EnvType.CLIENT)
public final class VehicleTireSoundInstance extends MovingSoundInstance {
    public enum Mode { BRAKE, DRIFT }

    private final VehicleEntity vehicle;
    private final Mode mode;
    private int quietTicks;

    public VehicleTireSoundInstance(VehicleEntity vehicle, Mode mode) {
        super(mode == Mode.BRAKE ? ModSounds.BRAKE_SOFT : ModSounds.TIRE_SKID,
            SoundCategory.NEUTRAL, SoundInstance.createRandom());
        this.vehicle = vehicle;
        this.mode = mode;
        this.repeat = true;
        this.repeatDelay = 0;
        this.volume = 0.0f;
        this.pitch = mode == Mode.BRAKE ? 0.88f : 0.96f;
        MinecraftClient client = MinecraftClient.getInstance();
        this.attenuationType = client.player != null && client.player.getVehicle() == vehicle
            ? SoundInstance.AttenuationType.NONE : SoundInstance.AttenuationType.LINEAR;
        this.x = vehicle.getX();
        this.y = vehicle.getY();
        this.z = vehicle.getZ();
    }

    @Override
    public void tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        boolean localDriver = client.player != null && client.player.getVehicle() == this.vehicle
            && this.vehicle.getControllingPassenger() == client.player;
        if (this.vehicle.isRemoved() || !this.vehicle.isAlive() || this.vehicle.isAircraft()
            || !this.vehicle.hasDriver() || !localDriver) {
            this.fadeOrFinish();
            return;
        }
        this.x = this.vehicle.getX();
        this.y = this.vehicle.getY();
        this.z = this.vehicle.getZ();
        VehicleInputState input = this.vehicle.getInputState();
        float speed = Math.abs(this.vehicle.getSpeed());
        float speedMix = MathHelper.clamp((speed - 0.035f) / 0.62f, 0.0f, 1.0f);
        float drift = MathHelper.clamp(this.vehicle.getDriftAmount(), 0.0f, 1.0f);
        boolean active;
        float targetVolume;
        float targetPitch;
        if (this.mode == Mode.BRAKE) {
            active = input.brake && speed > 0.045f && drift < 0.28f;
            targetVolume = active ? 0.17f + speedMix * 0.15f : 0.0f;
            targetPitch = 0.84f + speedMix * 0.25f;
        } else {
            boolean hardTurn = (input.left || input.right) && speed > 0.38f;
            active = speed > 0.10f && (drift > 0.12f || hardTurn);
            float friction = Math.max(drift, hardTurn ? speedMix * 0.45f : 0.0f);
            targetVolume = active ? 0.16f + speedMix * 0.12f + friction * 0.09f : 0.0f;
            targetPitch = 0.91f + speedMix * 0.26f;
        }
        this.pitch = MathHelper.lerp(0.10f, this.pitch, targetPitch);
        this.volume = MathHelper.lerp(active ? 0.16f : 0.24f, this.volume, targetVolume);
        if (active) this.quietTicks = 0;
        else if (++this.quietTicks > 18 && this.volume < 0.006f) this.setDone();
    }

    private void fadeOrFinish() {
        this.volume *= 0.74f;
        if (++this.quietTicks > 18 || this.volume < 0.004f) this.setDone();
    }
}
