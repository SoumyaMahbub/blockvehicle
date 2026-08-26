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
public class VehicleIdleSoundInstance
extends MovingSoundInstance {
    private final VehicleEntity vehicle;
    private int fadeOutTicks = 0;

    public VehicleIdleSoundInstance(VehicleEntity vehicle) {
        super(ModSounds.ENGINE_IDLE, SoundCategory.NEUTRAL, SoundInstance.createRandom());
        this.vehicle = vehicle;
        this.repeat = true;
        this.repeatDelay = 0;
        this.volume = 0.5f;
        this.pitch = 1.0f;
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
        float targetVolume = 0.65f * (1.0f - speedFrac * 0.45f);
        float targetPitch = 0.95f + speedFrac * 0.35f;
        this.pitch = MathHelper.lerp((float)0.12f, (float)this.pitch, (float)targetPitch);
        this.volume = MathHelper.lerp((float)0.12f, (float)this.volume, (float)targetVolume);
    }
}

