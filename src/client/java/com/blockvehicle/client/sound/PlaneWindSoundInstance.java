package com.blockvehicle.client.sound;

import com.blockvehicle.entity.VehicleEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;

/** One lightweight wind loop per loaded plane; volume follows airspeed. */
@Environment(EnvType.CLIENT)
public final class PlaneWindSoundInstance extends MovingSoundInstance {
    private final VehicleEntity plane;

    public PlaneWindSoundInstance(VehicleEntity plane) {
        super(SoundEvents.ITEM_ELYTRA_FLYING, SoundCategory.NEUTRAL, SoundInstance.createRandom());
        this.plane = plane;
        this.repeat = true;
        this.repeatDelay = 0;
        this.volume = 0.0f;
        this.pitch = 0.85f;
        this.attenuationType = SoundInstance.AttenuationType.LINEAR;
    }

    @Override
    public void tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        boolean localPlane = client.player != null && client.player.getVehicle() == this.plane;
        boolean tooFar = !localPlane && client.getCameraEntity() != null
            && client.getCameraEntity().squaredDistanceTo(this.plane) > 128.0 * 128.0;
        if (this.plane.isRemoved() || !this.plane.isAlive() || !this.plane.isAircraft() || tooFar) {
            this.setDone();
            return;
        }
        this.x = this.plane.getX();
        this.y = this.plane.getY();
        this.z = this.plane.getZ();
        float speed = MathHelper.clamp((float)this.plane.getPlaneVelocity().length() / 1.35f, 0.0f, 1.0f);
        this.volume = MathHelper.lerp(0.075f, this.volume, speed * 0.20f);
        this.pitch = MathHelper.lerp(0.065f, this.pitch, 0.82f + speed * 0.26f);
    }
}
