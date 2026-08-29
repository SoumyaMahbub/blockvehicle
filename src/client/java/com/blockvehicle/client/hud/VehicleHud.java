package com.blockvehicle.client.hud;

import com.blockvehicle.entity.VehicleEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;

@Environment(value=EnvType.CLIENT)
public final class VehicleHud {
    private VehicleHud() {
    }

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }
        Entity vehicle = client.player.getVehicle();
        if (!(vehicle instanceof VehicleEntity)) {
            return;
        }
        VehicleEntity ve = (VehicleEntity)vehicle;
        if (ve.isPlane()) {
            renderPlane(context, client, ve);
            return;
        }
        if (ve.isHelicopter()) {
            renderHelicopter(context, client, ve);
            return;
        }
        int screenW = context.getScaledWindowWidth();
        int screenH = context.getScaledWindowHeight();
        float speedBPT = Math.abs(ve.getSpeed());
        float speedBPS = speedBPT * 20.0f;
        int panelW = 160;
        int panelH = 50;
        int panelX = 10;
        int panelY = screenH - panelH - 10;
        context.fill(panelX, panelY, panelX + panelW, panelY + panelH, -2013265920);
        int barW = 140;
        int barH = 6;
        int barX = panelX + 10;
        int barY = panelY + 10;
        float fraction = Math.min(speedBPT / 0.75f, 1.0f);
        context.fill(barX, barY, barX + barW, barY + barH, -13421773);
        int barColor = fraction < 0.5f ? VehicleHud.blendColor(-16724924, -13312, fraction * 2.0f) : VehicleHud.blendColor(-13312, -3399168, (fraction - 0.5f) * 2.0f);
        context.fill(barX, barY, barX + (int)((float)barW * fraction), barY + barH, barColor);
        context.drawText(client.textRenderer, (Text)Text.literal(String.format("%.1f m/s", Float.valueOf(speedBPS))), barX, barY + 10, -1, true);
        boolean isReversing = ve.getSpeed() < -0.005f;
        String dir = ve.getDriftAmount() > 0.22f ? "\u00a7eDRIFTING" : (isReversing ? "\u00a7cREVERSING" : "\u00a7aDRIVING");
        context.drawText(client.textRenderer, (Text)Text.literal(dir), barX, barY + 20, -1, true);
        context.drawText(client.textRenderer, (Text)Text.literal("\u00a77[SHIFT] Exit  [SPACE] Brake / Drift"), panelX + 5, panelY + panelH - 12, -5592406, false);
    }

    private static void renderPlane(DrawContext context, MinecraftClient client, VehicleEntity plane) {
        int screenH = context.getScaledWindowHeight();
        int panelX = 10;
        int panelY = screenH - 90;
        int panelW = 310;
        int panelH = 80;
        context.fill(panelX, panelY, panelX + panelW, panelY + panelH, -2013265920);
        int accent = plane.getStallAmount() > 0.55f ? 0xFFFF3A2F : 0xFF35CFFF;
        context.fill(panelX, panelY, panelX + 3, panelY + panelH, accent);
        boolean localStunt = plane.getControllingPassenger() == client.player && plane.getInputState().stunt;
        String status = plane.getStallAmount() > 0.55f ? "\u00a7c\u00a7lSTALL"
            : localStunt ? "\u00a7d\u00a7lSTUNT \u00a7r\u00a7d✦"
            : "\u00a7b\u2708 " + plane.getPlaneFlightState().name().replace('_', ' ');
        context.drawText(client.textRenderer, Text.literal(status), panelX + 9, panelY + 7, -1, true);
        context.drawText(client.textRenderer, Text.literal(String.format("\u00a77Throttle: \u00a7f%3.0f%%   \u00a77Airspeed: \u00a7f%.1f m/s",
            plane.getThrottle() * 100.0f, plane.getPlaneVelocity().length() * 20.0)), panelX + 9, panelY + 20, -1, false);
        context.drawText(client.textRenderer, Text.literal(String.format("\u00a77Altitude: \u00a7f%.0f   \u00a77Vertical: %s%.1f m/s",
            plane.getY(), plane.getPlaneVelocity().y >= 0.0 ? "\u00a7a+" : "\u00a7c", plane.getPlaneVelocity().y * 20.0)), panelX + 9, panelY + 31, -1, false);
        context.drawText(client.textRenderer, Text.literal(String.format("\u00a77AoA: \u00a7f%.0f\u00b0   \u00a77RPM: \u00a7f%3.0f%%",
            plane.getAngleOfAttack(), plane.getEngineRpm() * 100.0f)), panelX + 9, panelY + 42, -1, false);
        context.drawText(client.textRenderer, Text.literal("\u00a78W/S Throttle  A/D Roll  LMB/RMB Rudder"), panelX + 7, panelY + 56, -5592406, false);
        context.drawText(client.textRenderer, Text.literal("\u00a78\u2191/\u2193 Pitch  Hold Alt Stunt  Space Brake  Shift Exit"), panelX + 7, panelY + 67, -5592406, false);
    }

    private static void renderHelicopter(DrawContext context, MinecraftClient client, VehicleEntity helicopter) {
        int screenH = context.getScaledWindowHeight();
        int panelX = 10, panelY = screenH - 92, panelW = 320, panelH = 82;
        context.fill(panelX, panelY, panelX + panelW, panelY + panelH, -2013265920);
        int accent = helicopter.getStallAmount() > 0.50f ? 0xFFFF593D : 0xFF41E88B;
        context.fill(panelX, panelY, panelX + 3, panelY + panelH, accent);
        String status = helicopter.getStallAmount() > 0.50f ? "\u00a7c\u00a7lVORTEX RING - LOWER COLLECTIVE + MOVE"
            : "\u00a7a HELICOPTER " + helicopter.getPlaneFlightState().name().replace('_', ' ');
        context.drawText(client.textRenderer, Text.literal(status), panelX + 9, panelY + 7, -1, true);
        context.drawText(client.textRenderer, Text.literal(String.format("\u00a77Collective: \u00a7f%3.0f%%   \u00a77Rotor RPM: \u00a7f%3.0f%%   \u00a77Speed: \u00a7f%.1f m/s",
            helicopter.getThrottle() * 100.0f, helicopter.getEngineRpm() * 100.0f,
            helicopter.getPlaneVelocity().length() * 20.0)), panelX + 9, panelY + 20, -1, false);
        context.drawText(client.textRenderer, Text.literal(String.format("\u00a77Altitude: \u00a7f%.0f   \u00a77Vertical: %s%.1f m/s   \u00a77Disk tilt: \u00a7f%.0f\u00b0",
            helicopter.getY(), helicopter.getPlaneVelocity().y >= 0.0 ? "\u00a7a+" : "\u00a7c",
            helicopter.getPlaneVelocity().y * 20.0, helicopter.getAngleOfAttack())), panelX + 9, panelY + 32, -1, false);
        context.drawText(client.textRenderer, Text.literal("\u00a78W/S Pitch  A/D Roll  LMB/RMB Yaw  Alt Precision"), panelX + 7, panelY + 55, -5592406, false);
        context.drawText(client.textRenderer, Text.literal("\u00a78Space Collective Up  Left Ctrl Down  Shift Exit"), panelX + 7, panelY + 68, -5592406, false);
    }

    private static int blendColor(int a, int b, float t) {
        int ar = a >> 16 & 0xFF;
        int ag = a >> 8 & 0xFF;
        int ab = a & 0xFF;
        int br = b >> 16 & 0xFF;
        int bg = b >> 8 & 0xFF;
        int bb = b & 0xFF;
        int r = (int)((float)ar + (float)(br - ar) * t);
        int g = (int)((float)ag + (float)(bg - ag) * t);
        int bv = (int)((float)ab + (float)(bb - ab) * t);
        return 0xFF000000 | r << 16 | g << 8 | bv;
    }
}
