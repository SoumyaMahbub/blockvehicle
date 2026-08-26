package com.blockvehicle.client.input;

import com.blockvehicle.entity.VehicleEntity;
import com.blockvehicle.network.VehicleHornPayload;
import com.blockvehicle.network.VehicleInputPayload;
import com.blockvehicle.vehicle.VehicleInputState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.CustomPayload;

@Environment(value=EnvType.CLIENT)
public final class VehicleInputHandler {
    public static KeyBinding HORN_KEY;
    private static VehicleInputState lastSentInput = VehicleInputState.EMPTY;
    private static int lastSentVehicleId = -1;
    private static int sendCooldown = 0;

    private VehicleInputHandler() {
    }

    public static void register() {
        HORN_KEY = KeyBindingHelper.registerKeyBinding((KeyBinding)new KeyBinding("key.blockvehicle.horn", InputUtil.Type.KEYSYM, 72, "key.categories.misc"));
    }

    public static void onStartTick(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            return;
        }
        Entity vehicle = client.player.getVehicle();
        if (!(vehicle instanceof VehicleEntity)) {
            lastSentVehicleId = -1;
            lastSentInput = VehicleInputState.EMPTY;
            sendCooldown = 0;
            return;
        }
        VehicleEntity ve = (VehicleEntity)vehicle;
        if (ve.getControllingPassenger() != client.player) {
            return;
        }
        GameOptions options = client.options;
        boolean forward = options.forwardKey.isPressed();
        boolean backward = options.backKey.isPressed();
        boolean left = options.leftKey.isPressed();
        boolean right = options.rightKey.isPressed();
        boolean brake = options.jumpKey.isPressed();
        if (HORN_KEY != null) {
            while (HORN_KEY.wasPressed()) {
                if (!ClientPlayNetworking.canSend(VehicleHornPayload.ID)) continue;
                ClientPlayNetworking.send((CustomPayload)new VehicleHornPayload(ve.getId()));
            }
        }
        VehicleInputState input = new VehicleInputState(forward, backward, left, right, brake);
        ve.setInputState(input);
    }

    public static void onEndTick(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            return;
        }
        Entity vehicle = client.player.getVehicle();
        if (!(vehicle instanceof VehicleEntity)) {
            return;
        }
        VehicleEntity ve = (VehicleEntity)vehicle;
        if (ve.getControllingPassenger() != client.player) {
            return;
        }
        VehicleInputState input = ve.getInputState();
        if (input == null) {
            input = VehicleInputState.EMPTY;
        }
        boolean vehicleChanged = lastSentVehicleId != ve.getId();
        boolean controlsChanged = !input.sameControls(lastSentInput);
        int interval = input.hasAnyInput() || Math.abs(ve.getSpeed()) > 0.003f || !ve.isOnGround() ? 1 : 5;
        ++sendCooldown;
        if (!vehicleChanged && !controlsChanged && sendCooldown < interval) {
            return;
        }
        if (ClientPlayNetworking.canSend(VehicleInputPayload.ID)) {
            ClientPlayNetworking.send((CustomPayload)new VehicleInputPayload(input.forward, input.backward, input.left, input.right, input.brake, ve.getX(), ve.getY(), ve.getZ(), ve.getYaw(), ve.getSpeed(), ve.getVehiclePitch(), ve.getVehicleRoll()));
            lastSentVehicleId = ve.getId();
            lastSentInput = input;
            sendCooldown = 0;
        }
    }
}
