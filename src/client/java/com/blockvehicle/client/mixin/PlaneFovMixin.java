package com.blockvehicle.client.mixin;

import com.blockvehicle.config.BlockVehicleConfig;
import com.blockvehicle.entity.VehicleEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A small size-aware FOV boost complements the much larger chase-camera distance. */
@Mixin(GameRenderer.class)
public abstract class PlaneFovMixin {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void blockvehicle$fitLargePlane(Camera camera, float tickDelta, boolean changingFov,
                                             CallbackInfoReturnable<Float> cir) {
        if (!camera.isThirdPerson() || !(camera.getFocusedEntity().getVehicle() instanceof VehicleEntity plane)
            || !plane.isAircraft() || plane.getStructure() == null) return;
        double planSize = Math.max(plane.getStructure().getWidth(), plane.getStructure().getLength());
        double boost = MathHelper.clamp((planSize - 6.0) * 0.45, 0.0,
            BlockVehicleConfig.get().cameraMaxFovBoost);
        cir.setReturnValue((float)(cir.getReturnValue() + boost));
    }
}
