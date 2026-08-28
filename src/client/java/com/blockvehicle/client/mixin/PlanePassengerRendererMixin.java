package com.blockvehicle.client.mixin;

import com.blockvehicle.entity.VehicleEntity;
import com.blockvehicle.vehicle.AircraftOrientation;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps rider models attached to the aircraft attitude during banks and inverted flight. */
@Mixin(LivingEntityRenderer.class)
public abstract class PlanePassengerRendererMixin {
    @Unique private static final Map<LivingEntityRenderState, Quaternionf> BLOCKVEHICLE_TILTS = new IdentityHashMap<>();

    @Inject(method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V",
        at = @At("TAIL"))
    private void blockvehicle$captureAircraftTilt(LivingEntity entity, LivingEntityRenderState state,
                                                   float tickDelta, CallbackInfo ci) {
        if (!(entity.getVehicle() instanceof VehicleEntity plane) || !plane.isPlane()) {
            BLOCKVEHICLE_TILTS.remove(state);
            return;
        }
        Quaternionf aircraft = plane.getPrevAircraftOrientation().slerp(plane.getAircraftOrientation(), tickDelta).normalize();
        float yaw = AircraftOrientation.yawDegrees(aircraft);
        Quaternionf tilt = AircraftOrientation.fromYaw(yaw).conjugate().mul(aircraft).normalize();
        BLOCKVEHICLE_TILTS.put(state, tilt);
    }

    @Inject(method = "setupTransforms", at = @At("TAIL"))
    private void blockvehicle$rotateWithAircraft(LivingEntityRenderState state, MatrixStack matrices,
                                                  float bodyYaw, float baseScale, CallbackInfo ci) {
        Quaternionf tilt = BLOCKVEHICLE_TILTS.get(state);
        if (tilt != null) matrices.multiply(tilt);
    }
}
