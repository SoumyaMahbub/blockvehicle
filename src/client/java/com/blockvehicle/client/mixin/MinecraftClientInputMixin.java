package com.blockvehicle.client.mixin;

import com.blockvehicle.entity.VehicleEntity;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Prevents the plane rudder defaults from also dropping an item/opening inventory. */
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientInputMixin {
    @Inject(method = "handleInputEvents", at = @At("HEAD"))
    private void blockvehicle$consumePlaneRudderBindings(CallbackInfo ci) {
        MinecraftClient client = (MinecraftClient)(Object)this;
        if (client.player == null || !(client.player.getVehicle() instanceof VehicleEntity plane)
            || !plane.isPlane() || plane.getControllingPassenger() != client.player) {
            return;
        }
        while (client.options.dropKey.wasPressed()) {
            // Q remains available to BlockVehicle's continuous keybinding.
        }
        while (client.options.inventoryKey.wasPressed()) {
            // E remains available to BlockVehicle's continuous keybinding.
        }
    }
}
