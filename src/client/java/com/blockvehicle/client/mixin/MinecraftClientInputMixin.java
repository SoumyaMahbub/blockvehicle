package com.blockvehicle.client.mixin;

import com.blockvehicle.entity.VehicleEntity;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Reserves attack/use input for vehicle controls while the local player drives. */
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientInputMixin {
    @Unique
    private boolean blockvehicle$isControllingVehicle() {
        MinecraftClient client = (MinecraftClient)(Object)this;
        return client.player != null
            && client.player.getVehicle() instanceof VehicleEntity vehicle
            && vehicle.getControllingPassenger() == client.player;
    }

    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void blockvehicle$preventVehicleAttack(CallbackInfoReturnable<Boolean> cir) {
        if (this.blockvehicle$isControllingVehicle()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "doItemUse", at = @At("HEAD"), cancellable = true)
    private void blockvehicle$preventVehicleItemUse(CallbackInfo ci) {
        if (this.blockvehicle$isControllingVehicle()) {
            ci.cancel();
        }
    }

    @Inject(method = "handleBlockBreaking", at = @At("HEAD"), cancellable = true)
    private void blockvehicle$preventVehicleMining(boolean breaking, CallbackInfo ci) {
        if (this.blockvehicle$isControllingVehicle()) {
            MinecraftClient client = (MinecraftClient)(Object)this;
            if (client.interactionManager != null) {
                client.interactionManager.cancelBlockBreaking();
            }
            ci.cancel();
        }
    }
}
