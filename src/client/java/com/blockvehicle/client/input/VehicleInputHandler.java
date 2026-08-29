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
import org.lwjgl.glfw.GLFW;

@Environment(value=EnvType.CLIENT)
public final class VehicleInputHandler {
    public static KeyBinding HORN_KEY;
    public static KeyBinding PLANE_PITCH_UP_KEY;
    public static KeyBinding PLANE_PITCH_DOWN_KEY;
    public static KeyBinding PLANE_STUNT_KEY;
    public static KeyBinding HELICOPTER_DESCEND_KEY;
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
        PLANE_STUNT_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.blockvehicle.plane_stunt", InputUtil.Type.KEYSYM, 342, "category.blockvehicle"));
        HELICOPTER_DESCEND_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.blockvehicle.helicopter_descend", InputUtil.Type.KEYSYM, 341, "category.blockvehicle"));
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
        // Rudder uses the physical mouse buttons, but vanilla must not leave an
        // old mining/item-use action or hand swing active after mounting.
        if (client.interactionManager != null) {
            client.interactionManager.cancelBlockBreaking();
            if (client.player.isUsingItem()) {
                client.interactionManager.stopUsingItem(client.player);
            }
        }
        client.player.handSwinging = false;
        client.player.handSwingTicks = 0;
        client.player.lastHandSwingProgress = 0.0f;
        client.player.handSwingProgress = 0.0f;
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
        long window = client.getWindow().getHandle();
        boolean rawControl = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
            || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
        boolean pitchDown = ve.isPlane() && PLANE_PITCH_DOWN_KEY.isPressed()
            || ve.isHelicopter() && (HELICOPTER_DESCEND_KEY.isPressed() || rawControl);
        boolean yawLeft = ve.isAircraft() && GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean yawRight = ve.isAircraft() && GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        boolean stunt = ve.isAircraft() && PLANE_STUNT_KEY.isPressed();
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
        if (ve.isAircraft() && ClientPlayNetworking.canSend(PlaneInputPayload.ID)) {
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
