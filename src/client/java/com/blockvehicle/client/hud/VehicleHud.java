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
        String dir = isReversing ? "\u00a7cREVERSING" : "\u00a7aDRIVING";
        context.drawText(client.textRenderer, (Text)Text.literal(dir), barX, barY + 20, -1, true);
        context.drawText(client.textRenderer, (Text)Text.literal("\u00a77[SHIFT] Exit  [SPACE] Brake"), panelX + 5, panelY + panelH - 12, -5592406, false);
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

