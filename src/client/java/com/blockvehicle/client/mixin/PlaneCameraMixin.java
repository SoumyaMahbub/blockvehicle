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
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds a restrained aircraft attitude cue without forcing a nausea-heavy cockpit camera. */
@Mixin(Camera.class)
public abstract class PlaneCameraMixin {
    @Shadow @Final private Quaternionf rotation;
    @Shadow private Entity focusedEntity;

    @Shadow protected abstract void setRotation(float yaw, float pitch);

    @ModifyArg(method = "update", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/render/Camera;clipToSpace(F)F"), index = 0)
    private float blockvehicle$frameLargePlane(float vanillaDistance) {
        if (!(this.focusedEntity != null && this.focusedEntity.getVehicle() instanceof VehicleEntity plane)
            || !plane.isPlane() || plane.getStructure() == null) return vanillaDistance;
        double width = plane.getStructure().getWidth();
        double height = plane.getStructure().getHeight();
        double length = plane.getStructure().getLength();
        double radius = Math.sqrt(width * width + length * length + height * height) * 0.5;
        BlockVehicleConfig.Values config = BlockVehicleConfig.get();
        return (float)MathHelper.clamp(4.0 + radius * config.cameraSizeDistanceMultiplier,
            vanillaDistance, config.cameraMaxDistance);
    }

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
