package com.blockvehicle.client.sound;

import com.blockvehicle.entity.VehicleEntity;
import com.blockvehicle.sound.ModSounds;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.MathHelper;

@Environment(value=EnvType.CLIENT)
public class VehicleEngineSoundInstance
extends MovingSoundInstance {
    private final VehicleEntity vehicle;
    private int fadeOutTicks = 0;

    public VehicleEngineSoundInstance(VehicleEntity vehicle) {
        super(ModSounds.ENGINE_REV, SoundCategory.NEUTRAL, SoundInstance.createRandom());
        this.vehicle = vehicle;
        this.repeat = true;
        this.repeatDelay = 0;
        this.volume = 0.2f;
        this.pitch = 0.7f;
        this.attenuationType = SoundInstance.AttenuationType.LINEAR;
        this.x = vehicle.getX();
        this.y = vehicle.getY();
        this.z = vehicle.getZ();
    }

    public void tick() {
        if (this.vehicle.isRemoved() || !this.vehicle.isAlive()) {
            this.setDone();
            return;
        }
        this.x = this.vehicle.getX();
        this.y = this.vehicle.getY();
        this.z = this.vehicle.getZ();
        boolean hasDriver = this.vehicle.hasDriver();
        if (!hasDriver) {
            ++this.fadeOutTicks;
            this.volume = Math.max(0.0f, this.volume - 0.05f);
            if (this.volume <= 0.0f || this.fadeOutTicks > 30) {
                this.setDone();
            }
            return;
        }
        this.fadeOutTicks = 0;
        float speedFrac = Math.min(Math.abs(this.vehicle.getSpeed()) / 0.85f, 1.0f);
        boolean isThrottling = this.vehicle.getInputState().forward || this.vehicle.getInputState().backward;
        float targetPitch = 0.7f + speedFrac * 0.85f + (isThrottling ? 0.08f : -0.04f);
        targetPitch = MathHelper.clamp((float)targetPitch, (float)0.65f, (float)1.75f);
        float targetVolume = 0.25f + speedFrac * 0.7f + (isThrottling ? 0.15f : 0.0f);
        targetVolume = MathHelper.clamp((float)targetVolume, (float)0.2f, (float)1.0f);
        this.pitch = MathHelper.lerp((float)0.12f, (float)this.pitch, (float)targetPitch);
        this.volume = MathHelper.lerp((float)0.12f, (float)this.volume, (float)targetVolume);
    }
}

