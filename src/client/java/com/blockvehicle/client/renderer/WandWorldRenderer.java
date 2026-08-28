package com.blockvehicle.client.renderer;

import com.blockvehicle.ModItems;
import com.blockvehicle.client.wand.ClientWandStore;
import com.blockvehicle.item.PlayerDataStore;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

@Environment(value=EnvType.CLIENT)
public final class WandWorldRenderer {
    private WandWorldRenderer() {
    }

    public static void render(WorldRenderContext context) {
        BlockPos driver;
        BlockPos c2;
        BlockPos c1;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }
        boolean holdingMain = client.player.getMainHandStack().isOf(ModItems.VEHICLE_WAND);
        boolean holdingOff = client.player.getOffHandStack().isOf(ModItems.VEHICLE_WAND);
        if (!holdingMain && !holdingOff) {
            return;
        }
        Camera camera = context.camera();
        if (camera == null) {
            return;
        }
        Vec3d camPos = camera.getPos();
        MatrixStack matrices = context.matrixStack();
        if (matrices == null) {
            return;
        }
        matrices.push();
        matrices.translate(-camPos.x, -camPos.y, -camPos.z);
        MatrixStack.Entry entry = matrices.peek();
        Matrix4f mat = entry.getPositionMatrix();
        VertexConsumer lines = context.consumers().getBuffer(RenderLayer.getLines());
        VertexConsumer quads = context.consumers().getBuffer(RenderLayer.getDebugQuads());
        Box selBox = ClientWandStore.getSelectionBox();
        if (selBox != null) {
            WandWorldRenderer.drawBoxOutline(mat, lines, selBox.minX, selBox.minY, selBox.minZ, selBox.maxX, selBox.maxY, selBox.maxZ, 0.0f, 0.9f, 1.0f, 1.0f);
            WandWorldRenderer.drawBoxFilled(mat, quads, selBox.minX, selBox.minY, selBox.minZ, selBox.maxX, selBox.maxY, selBox.maxZ, 0.0f, 0.85f, 1.0f, 0.09f);
        }
        if ((c1 = ClientWandStore.getCorner1()) != null) {
            WandWorldRenderer.drawBlockBox(mat, lines, quads, c1, 0.15f, 0.65f, 1.0f, 0.9f, 0.18f);
        }
        if ((c2 = ClientWandStore.getCorner2()) != null) {
            WandWorldRenderer.drawBlockBox(mat, lines, quads, c2, 0.0f, 1.0f, 0.85f, 0.9f, 0.18f);
        }
        if ((driver = ClientWandStore.getDriverSeat()) != null) {
            WandWorldRenderer.drawBlockBox(mat, lines, quads, driver, 1.0f, 0.8f, 0.0f, 1.0f, 0.25f);
        }
        List<BlockPos> passengers = ClientWandStore.getPassengerSeats();
        for (BlockPos blockPos : passengers) {
            WandWorldRenderer.drawBlockBox(mat, lines, quads, blockPos, 0.92f, 0.25f, 1.0f, 0.95f, 0.22f);
        }
        List<BlockPos> wheels = ClientWandStore.getCustomWheels();
        for (BlockPos wPos : wheels) {
            WandWorldRenderer.drawBlockBox(mat, lines, quads, wPos, 1.0f, 0.55f, 0.0f, 0.95f, 0.22f);
        }
        if (ClientWandStore.getPlaneNose() != null) {
            WandWorldRenderer.drawBlockBox(mat, lines, quads, ClientWandStore.getPlaneNose(), 0.1f, 0.8f, 1.0f, 1.0f, 0.25f);
        }
        if (ClientWandStore.getLeftWingTip() != null) {
            WandWorldRenderer.drawBlockBox(mat, lines, quads, ClientWandStore.getLeftWingTip(), 1.0f, 0.2f, 0.8f, 1.0f, 0.22f);
        }
        if (ClientWandStore.getRightWingTip() != null) {
            WandWorldRenderer.drawBlockBox(mat, lines, quads, ClientWandStore.getRightWingTip(), 0.3f, 0.4f, 1.0f, 1.0f, 0.22f);
        }
        for (BlockPos hub : ClientWandStore.getPropellerHubs()) {
            WandWorldRenderer.drawBlockBox(mat, lines, quads, hub, 1.0f, 0.75f, 0.0f, 1.0f, 0.3f);
        }
        for (BlockPos blade : ClientWandStore.getPropellerBlades()) {
            WandWorldRenderer.drawBlockBox(mat, lines, quads, blade, 1.0f, 0.95f, 0.35f, 0.9f, 0.18f);
        }
        HitResult hitResult = client.crosshairTarget;
        if (hitResult instanceof BlockHitResult) {
            BlockHitResult bhr = (BlockHitResult)hitResult;
            if (hitResult.getType() == HitResult.Type.BLOCK) {
                BlockPos targetPos = bhr.getBlockPos();
                PlayerDataStore.WandMode mode = ClientWandStore.getMode();
                float tr = 0.0f;
                float tg = 0.9f;
                float tb = 1.0f;
                switch (mode) {
                    case SET_DRIVER_SEAT: {
                        tr = 1.0f;
                        tg = 0.75f;
                        tb = 0.0f;
                        break;
                    }
                    case SET_PASSENGER_SEAT: {
                        tr = 0.9f;
                        tg = 0.25f;
                        tb = 1.0f;
                        break;
                    }
                    case SET_WHEEL: {
                        tr = 1.0f;
                        tg = 0.55f;
                        tb = 0.0f;
                        break;
                    }
                    case SET_PLANE_NOSE: {
                        tr = 0.1f;
                        tg = 0.8f;
                        tb = 1.0f;
                        break;
                    }
                    case SET_WING_TIPS: {
                        tr = 0.95f;
                        tg = 0.25f;
                        tb = 0.8f;
                        break;
                    }
                    case SET_PROPELLER: {
                        tr = 1.0f;
                        tg = 0.8f;
                        tb = 0.1f;
                        break;
                    }
                    case ACTIVATE: {
                        tr = 0.0f;
                        tg = 1.0f;
                        tb = 0.45f;
                    }
                }
                WandWorldRenderer.drawBlockBox(mat, lines, quads, targetPos, tr, tg, tb, 0.85f, 0.12f);
            }
        }
        matrices.pop();
    }

    private static void drawBlockBox(Matrix4f mat, VertexConsumer lines, VertexConsumer quads, BlockPos pos, float r, float g, float b, float lineA, float fillA) {
        double minX = (double)pos.getX() - 0.002;
        double minY = (double)pos.getY() - 0.002;
        double minZ = (double)pos.getZ() - 0.002;
        double maxX = (double)pos.getX() + 1.002;
        double maxY = (double)pos.getY() + 1.002;
        double maxZ = (double)pos.getZ() + 1.002;
        WandWorldRenderer.drawBoxOutline(mat, lines, minX, minY, minZ, maxX, maxY, maxZ, r, g, b, lineA);
        WandWorldRenderer.drawBoxFilled(mat, quads, minX, minY, minZ, maxX, maxY, maxZ, r, g, b, fillA);
    }

    private static void drawBoxOutline(Matrix4f mat, VertexConsumer lines, double x1, double y1, double z1, double x2, double y2, double z2, float r, float g, float b, float a) {
        float minX = (float)x1;
        float minY = (float)y1;
        float minZ = (float)z1;
        float maxX = (float)x2;
        float maxY = (float)y2;
        float maxZ = (float)z2;
        WandWorldRenderer.line(mat, lines, minX, minY, minZ, maxX, minY, minZ, r, g, b, a);
        WandWorldRenderer.line(mat, lines, maxX, minY, minZ, maxX, minY, maxZ, r, g, b, a);
        WandWorldRenderer.line(mat, lines, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a);
        WandWorldRenderer.line(mat, lines, minX, minY, maxZ, minX, minY, minZ, r, g, b, a);
        WandWorldRenderer.line(mat, lines, minX, maxY, minZ, maxX, maxY, minZ, r, g, b, a);
        WandWorldRenderer.line(mat, lines, maxX, maxY, minZ, maxX, maxY, maxZ, r, g, b, a);
        WandWorldRenderer.line(mat, lines, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a);
        WandWorldRenderer.line(mat, lines, minX, maxY, maxZ, minX, maxY, minZ, r, g, b, a);
        WandWorldRenderer.line(mat, lines, minX, minY, minZ, minX, maxY, minZ, r, g, b, a);
        WandWorldRenderer.line(mat, lines, maxX, minY, minZ, maxX, maxY, minZ, r, g, b, a);
        WandWorldRenderer.line(mat, lines, maxX, minY, maxZ, maxX, maxY, maxZ, r, g, b, a);
        WandWorldRenderer.line(mat, lines, minX, minY, maxZ, minX, maxY, maxZ, r, g, b, a);
    }

    private static void line(Matrix4f mat, VertexConsumer consumer, float x1, float y1, float z1, float x2, float y2, float z2, float r, float g, float b, float a) {
        consumer.vertex(mat, x1, y1, z1).color(r, g, b, a).normal(0.0f, 1.0f, 0.0f);
        consumer.vertex(mat, x2, y2, z2).color(r, g, b, a).normal(0.0f, 1.0f, 0.0f);
    }

    private static void drawBoxFilled(Matrix4f mat, VertexConsumer quads, double x1, double y1, double z1, double x2, double y2, double z2, float r, float g, float b, float a) {
        float minX = (float)x1;
        float minY = (float)y1;
        float minZ = (float)z1;
        float maxX = (float)x2;
        float maxY = (float)y2;
        float maxZ = (float)z2;
        WandWorldRenderer.quad(mat, quads, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a);
        WandWorldRenderer.quad(mat, quads, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, minX, maxY, minZ, r, g, b, a);
        WandWorldRenderer.quad(mat, quads, minX, maxY, minZ, maxX, maxY, minZ, maxX, minY, minZ, minX, minY, minZ, r, g, b, a);
        WandWorldRenderer.quad(mat, quads, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a);
        WandWorldRenderer.quad(mat, quads, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, r, g, b, a);
        WandWorldRenderer.quad(mat, quads, maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ, maxX, minY, minZ, r, g, b, a);
    }

    private static void quad(Matrix4f mat, VertexConsumer consumer, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, float r, float g, float b, float a) {
        consumer.vertex(mat, x1, y1, z1).color(r, g, b, a);
        consumer.vertex(mat, x2, y2, z2).color(r, g, b, a);
        consumer.vertex(mat, x3, y3, z3).color(r, g, b, a);
        consumer.vertex(mat, x4, y4, z4).color(r, g, b, a);
    }
}
