package com.blockvehicle.client.input;

import com.blockvehicle.entity.VehicleEntity;
import com.blockvehicle.network.VehicleHornPayload;
import com.blockvehicle.network.VehicleInputPayload;
import com.blockvehicle.network.PlaneInputPayload;
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
    public static KeyBinding PLANE_PITCH_UP_KEY;
    public static KeyBinding PLANE_PITCH_DOWN_KEY;
    public static KeyBinding PLANE_YAW_LEFT_KEY;
    public static KeyBinding PLANE_YAW_RIGHT_KEY;
    public static KeyBinding PLANE_STUNT_KEY;
    private static VehicleInputState lastSentInput = VehicleInputState.EMPTY;
    private static int lastSentVehicleId = -1;
    private static int sendCooldown = 0;
    private static int planeSequence = 0;

    private VehicleInputHandler() {
    }

    public static void register() {
        HORN_KEY = KeyBindingHelper.registerKeyBinding((KeyBinding)new KeyBinding("key.blockvehicle.horn", InputUtil.Type.KEYSYM, 72, "key.categories.misc"));
        PLANE_PITCH_UP_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.blockvehicle.plane_pitch_up", InputUtil.Type.KEYSYM, 265, "category.blockvehicle"));
        PLANE_PITCH_DOWN_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.blockvehicle.plane_pitch_down", InputUtil.Type.KEYSYM, 264, "category.blockvehicle"));
        PLANE_YAW_LEFT_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.blockvehicle.plane_yaw_left", InputUtil.Type.KEYSYM, 81, "category.blockvehicle"));
        PLANE_YAW_RIGHT_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.blockvehicle.plane_yaw_right", InputUtil.Type.KEYSYM, 69, "category.blockvehicle"));
        PLANE_STUNT_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.blockvehicle.plane_stunt", InputUtil.Type.KEYSYM, 342, "category.blockvehicle"));
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
        boolean pitchUp = ve.isPlane() && PLANE_PITCH_UP_KEY.isPressed();
        boolean pitchDown = ve.isPlane() && PLANE_PITCH_DOWN_KEY.isPressed();
        boolean yawLeft = ve.isPlane() && PLANE_YAW_LEFT_KEY.isPressed();
        boolean yawRight = ve.isPlane() && PLANE_YAW_RIGHT_KEY.isPressed();
        boolean stunt = ve.isPlane() && PLANE_STUNT_KEY.isPressed();
        VehicleInputState input = new VehicleInputState(forward, backward, left, right, brake,
            pitchUp, pitchDown, yawLeft, yawRight, stunt, client.player.getYaw(), client.player.getPitch());
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
        if (ve.isPlane() && ClientPlayNetworking.canSend(PlaneInputPayload.ID)) {
            int flags = 0;
            if (input.forward) flags |= 1;
            if (input.backward) flags |= 2;
            if (input.left) flags |= 4;
            if (input.right) flags |= 8;
            if (input.brake) flags |= 16;
            if (input.pitchUp) flags |= 32;
            if (input.pitchDown) flags |= 64;
            if (input.yawLeft) flags |= 128;
            if (input.yawRight) flags |= 256;
            if (input.stunt) flags |= 512;
            int sequence = ++planeSequence;
            ve.recordLocalPlaneInputSequence(sequence);
            ClientPlayNetworking.send(new PlaneInputPayload(sequence, flags, input.lookYaw, input.lookPitch));
            lastSentVehicleId = ve.getId();
            lastSentInput = input;
            sendCooldown = 0;
        } else if (ClientPlayNetworking.canSend(VehicleInputPayload.ID)) {
            ClientPlayNetworking.send((CustomPayload)new VehicleInputPayload(input.forward, input.backward, input.left, input.right, input.brake, ve.getX(), ve.getY(), ve.getZ(), ve.getYaw(), ve.getSpeed(), ve.getVehiclePitch(), ve.getVehicleRoll()));
            lastSentVehicleId = ve.getId();
            lastSentInput = input;
            sendCooldown = 0;
        }
    }
}
