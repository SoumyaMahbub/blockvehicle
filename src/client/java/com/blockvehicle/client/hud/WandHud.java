package com.blockvehicle.client.hud;

import com.blockvehicle.ModItems;
import com.blockvehicle.client.wand.ClientWandStore;
import com.blockvehicle.item.PlayerDataStore;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

@Environment(value=EnvType.CLIENT)
public final class WandHud {
    private WandHud() {
    }

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }
        boolean holdingMain = client.player.getMainHandStack().isOf(ModItems.VEHICLE_WAND);
        boolean holdingOff = client.player.getOffHandStack().isOf(ModItems.VEHICLE_WAND);
        if (!holdingMain && !holdingOff) {
            return;
        }
        if (client.player.getVehicle() != null) {
            return;
        }
        int screenW = context.getScaledWindowWidth();
        TextRenderer tr = client.textRenderer;
        PlayerDataStore.WandMode mode = ClientWandStore.getMode();
        BlockPos c1 = ClientWandStore.getCorner1();
        BlockPos c2 = ClientWandStore.getCorner2();
        BlockPos driver = ClientWandStore.getDriverSeat();
        Direction driverFacing = ClientWandStore.getDriverFacing();
        int passCount = ClientWandStore.getPassengerSeats().size();
        int wheelCount = ClientWandStore.getCustomWheels().size();
        boolean ready = ClientWandStore.isReadyToActivate();
        int cardW = 195;
        int cardH = 115;
        int cardX = screenW - cardW - 12;
        int cardY = 12;
        context.fill(cardX, cardY, cardX + cardW, cardY + cardH, -586213590);
        context.fill(cardX, cardY, cardX + cardW, cardY + 1, 1429782008);
        context.fill(cardX, cardY + cardH - 1, cardX + cardW, cardY + cardH, 1429782008);
        context.fill(cardX, cardY, cardX + 1, cardY + cardH, 1429782008);
        context.fill(cardX + cardW - 1, cardY, cardX + cardW, cardY + cardH, 1429782008);
        int themeColor = switch (mode) {
            default -> throw new MatchException(null, null);
            case PlayerDataStore.WandMode.SELECT_REGION -> -16718337;
            case PlayerDataStore.WandMode.SET_DRIVER_SEAT -> -19712;
            case PlayerDataStore.WandMode.SET_PASSENGER_SEAT -> -2080517;
            case PlayerDataStore.WandMode.SET_WHEEL -> -28416;
            case PlayerDataStore.WandMode.ACTIVATE -> -16718218;
        };
        context.fill(cardX + 4, cardY + 4, cardX + cardW - 4, cardY + 6, themeColor);
        context.drawText(tr, (Text)Text.literal("\u00a7l\u26a1 VEHICLE WAND"), cardX + 8, cardY + 10, -1, true);
        String modeName = switch (mode) {
            default -> throw new MatchException(null, null);
            case PlayerDataStore.WandMode.SELECT_REGION -> "\ud83d\udcd0 SELECT REGION";
            case PlayerDataStore.WandMode.SET_DRIVER_SEAT -> "\ud83d\udcba DRIVER SEAT";
            case PlayerDataStore.WandMode.SET_PASSENGER_SEAT -> "\ud83d\udc65 PASSENGER SEAT";
            case PlayerDataStore.WandMode.SET_WHEEL -> "\ud83d\ude97 WHEEL CONFIG";
            case PlayerDataStore.WandMode.ACTIVATE -> "\u26a1 ACTIVATE";
        };
        int badgeW = tr.getWidth(modeName) + 10;
        int badgeX = cardX + cardW - badgeW - 8;
        int badgeY = cardY + 8;
        context.fill(badgeX, badgeY, badgeX + badgeW, badgeY + 12, themeColor & 0xFFFFFF | 0x33000000);
        context.fill(badgeX, badgeY, badgeX + badgeW, badgeY + 1, themeColor);
        context.fill(badgeX, badgeY + 11, badgeX + badgeW, badgeY + 12, themeColor);
        context.drawText(tr, (Text)Text.literal(modeName), badgeX + 5, badgeY + 2, themeColor, true);
        context.fill(cardX + 8, cardY + 23, cardX + cardW - 8, cardY + 24, 0x33FFFFFF);
        int rowY = cardY + 28;
        if (c1 != null && c2 != null) {
            int w = ClientWandStore.getWidth();
            int h = ClientWandStore.getHeight();
            int l = ClientWandStore.getLength();
            int vol = ClientWandStore.getBlockVolume();
            context.drawText(tr, (Text)Text.literal(("\u00a77Region: \u00a7b" + w + "\u00a77\u00d7\u00a7b" + h + "\u00a77\u00d7\u00a7b" + l + " \u00a78(" + vol + " blk)")), cardX + 8, rowY, -1, false);
        } else if (c1 != null) {
            context.drawText(tr, (Text)Text.literal("\u00a77Region: \u00a7bC1 \u00a7aset \u00a77| \u00a7cSet C2 (Sneak+R-Click)"), cardX + 8, rowY, -1, false);
        } else {
            context.drawText(tr, (Text)Text.literal("\u00a77Region: \u00a7eNo region selected"), cardX + 8, rowY, -1, false);
        }
        rowY += 12;
        if (driver != null) {
            context.drawText(tr, (Text)Text.literal(("\u00a77Driver: \u00a76" + driver.toShortString() + " \u00a78(" + driverFacing.asString().toUpperCase() + ")")), cardX + 8, rowY, -1, false);
        } else {
            context.drawText(tr, (Text)Text.literal("\u00a77Driver: \u00a78Auto (stairs/front)"), cardX + 8, rowY, -1, false);
        }
        Object passStr = passCount > 0 ? "\u00a7d" + passCount + " seats" : "\u00a780 seats";
        Object wheelStr = wheelCount > 0 ? "\u00a7e" + wheelCount + " wheels" : "\u00a78auto";
        context.drawText(tr, (Text)Text.literal(("\u00a77Config: " + passStr + " \u00a77| " + wheelStr)), cardX + 8, rowY += 12, -1, false);
        rowY += 14;
        if (ready) {
            context.fill(cardX + 8, rowY, cardX + cardW - 8, rowY + 14, 855697014);
            context.fill(cardX + 8, rowY, cardX + 10, rowY + 14, -16718218);
            context.drawText(tr, (Text)Text.literal("\u00a7a\u2714 READY TO ACTIVATE"), cardX + 14, rowY + 3, -16718218, true);
        } else {
            context.fill(cardX + 8, rowY, cardX + cardW - 8, rowY + 14, 872395520);
            context.fill(cardX + 8, rowY, cardX + 10, rowY + 14, -19712);
            context.drawText(tr, (Text)Text.literal("\u00a7e\u25cb Select C1 & C2 to build"), cardX + 14, rowY + 3, -19712, true);
        }
        context.drawText(tr, (Text)Text.literal("\u00a78[Sneak+R-Click] Mode  [R-Click] Action"), cardX + 8, rowY += 18, -7035976, false);
        if (ready) {
            context.drawText(tr, Text.literal("\u00a78[/vehiclepreset save <name>] Save"), cardX + 8, rowY + 12, -7035976, false);
        }
    }
}
