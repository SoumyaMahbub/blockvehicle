package com.blockvehicle.client.mixin;

import com.blockvehicle.entity.VehicleEntity;
import com.blockvehicle.vehicle.AircraftOrientation;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.RotationAxis;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps rider models attached to the aircraft attitude during banks and inverted flight. */
@Mixin(LivingEntityRenderer.class)
public abstract class PlanePassengerRendererMixin {
    @Unique private static final Map<LivingEntityRenderState, Quaternionf> BLOCKVEHICLE_TRANSFORMS = new WeakHashMap<>();

    @Inject(method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V",
        at = @At("TAIL"))
    private void blockvehicle$captureAircraftTilt(LivingEntity entity, LivingEntityRenderState state,
                                                   float tickDelta, CallbackInfo ci) {
        if (entity.getVehicle() instanceof VehicleEntity vehicle
            && vehicle.getControllingPassenger() == entity
            && state instanceof BipedEntityRenderState bipedState) {
            bipedState.handSwingProgress = 0.0f;
            bipedState.isUsingItem = false;
            if (state instanceof PlayerEntityRenderState playerState) {
                playerState.handSwinging = false;
            }
        }
        if (!(entity.getVehicle() instanceof VehicleEntity plane) || !plane.isAircraft() || !entity.isAlive()) {
            BLOCKVEHICLE_TRANSFORMS.remove(state);
            return;
        }
        // Minecraft normally uses Ry(180 - bodyYaw). Replace that root with the
        // full aircraft frame, then apply the seat's local facing and the same
        // 180-degree model-facing correction. This order remains correct upside down.
        Quaternionf aircraft = plane.getPrevAircraftOrientation().slerp(plane.getAircraftOrientation(), tickDelta).normalize();
        Quaternionf modelTransform = aircraft
            .mul(AircraftOrientation.fromYaw(plane.getPassengerSeatRelativeYaw(entity)))
            .mul(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f))
            .normalize();
        BLOCKVEHICLE_TRANSFORMS.put(state, modelTransform);
    }

    @Inject(method = "setupTransforms", at = @At("HEAD"), cancellable = true)
    private void blockvehicle$rotateWithAircraft(LivingEntityRenderState state, MatrixStack matrices,
                                                  float bodyYaw, float baseScale, CallbackInfo ci) {
        Quaternionf transform = BLOCKVEHICLE_TRANSFORMS.get(state);
        if (transform != null) {
            matrices.multiply(transform);
            ci.cancel();
        }
    }
}
