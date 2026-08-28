package com.blockvehicle.client.mixin;

import com.blockvehicle.config.BlockVehicleConfig;
import com.blockvehicle.entity.VehicleEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.BlockView;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds a restrained aircraft attitude cue without forcing a nausea-heavy cockpit camera. */
@Mixin(Camera.class)
public abstract class PlaneCameraMixin {
    @Shadow @Final private Quaternionf rotation;

    @Shadow protected abstract void setRotation(float yaw, float pitch);

    @Inject(method = "update", at = @At("TAIL"))
    private void blockvehicle$applyPlaneCamera(BlockView area, Entity focusedEntity, boolean thirdPerson,
                                                boolean inverseView, float tickDelta, CallbackInfo ci) {
        if (!(focusedEntity.getVehicle() instanceof VehicleEntity plane) || !plane.isPlane()) return;
        BlockVehicleConfig.Values config = BlockVehicleConfig.get();
        float planePitch = MathHelper.lerp(tickDelta, plane.getPrevVehiclePitch(), plane.getVehiclePitch());
        float planeRoll = MathHelper.lerp(tickDelta, plane.getPrevVehicleRoll(), plane.getVehicleRoll());
        Camera camera = (Camera)(Object)this;
        float pitchFollow = (float)((1.0 - config.cameraHorizonStabilization) * (thirdPerson ? 0.32 : 0.55));
        this.setRotation(camera.getYaw(), MathHelper.clamp(camera.getPitch() + planePitch * pitchFollow, -89.5f, 89.5f));
        float rollFollow = (float)(config.cameraRollStrength
            * (1.0 - config.cameraHorizonStabilization * (thirdPerson ? 0.55 : 0.25)));
        this.rotation.rotateZ((float)Math.toRadians(-planeRoll * rollFollow)).normalize();
    }
}
