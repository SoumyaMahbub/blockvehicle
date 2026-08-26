package com.blockvehicle.client.renderer;

import com.blockvehicle.entity.VehicleEntity;
import com.blockvehicle.vehicle.VehicleStructure;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.AbstractSignBlock;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.world.World;
import org.joml.Vector3f;

@Environment(value=EnvType.CLIENT)
public class VehicleEntityRenderer
extends EntityRenderer<VehicleEntity, VehicleEntityRenderer.VehicleRenderState> {
    private final Map<VehicleStructure.StoredBlock, BlockEntity> blockEntityCache = new HashMap<VehicleStructure.StoredBlock, BlockEntity>();

    public VehicleEntityRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.0f;
    }

    public VehicleRenderState createRenderState() {
        return new VehicleRenderState();
    }

    public void updateRenderState(VehicleEntity entity, VehicleRenderState state, float tickDelta) {
        VehicleStructure structure;
        super.updateRenderState(entity, state, tickDelta);
        state.structure = structure = entity.getStructure();
        state.blocks = structure != null ? structure.getRenderableBlocks() : List.of();
        state.itemFrames = structure != null ? structure.getItemFrames() : List.of();
        state.initialYaw = structure != null ? structure.getInitialYaw() : 0.0f;
        state.vehicleYaw = MathHelper.lerpAngleDegrees((float)tickDelta, (float)entity.prevYaw, (float)entity.getYaw());
        state.speed = entity.getSpeed();
        state.pitch = MathHelper.lerp((float)tickDelta, (float)entity.prevVehiclePitch, (float)entity.getVehiclePitch());
        state.roll = MathHelper.lerp((float)tickDelta, (float)entity.prevVehicleRoll, (float)entity.getVehicleRoll());
        state.wheelRollAngle = MathHelper.lerp((float)tickDelta, (float)entity.getPrevWheelRollAngle(), (float)entity.getWheelRollAngle());
        state.steeringAngle = MathHelper.lerp((float)tickDelta, (float)entity.getPrevSteeringAngle(), (float)entity.getSteeringAngle());
        state.visualYOffset = MathHelper.lerp((float)tickDelta, (float)entity.getPrevVisualYOffset(), (float)entity.getVisualYOffset());
        state.tiltLift = MathHelper.lerp((float)tickDelta, (float)entity.getPrevTiltLift(), (float)entity.getTiltLift());
    }

    public void render(VehicleRenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        if (state.blocks.isEmpty() && state.itemFrames.isEmpty()) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        BlockRenderManager blockRenderManager = client.getBlockRenderManager();
        matrices.push();
        matrices.translate(0.0, (double)(state.visualYOffset + state.tiltLift), 0.0);
        float relativeYaw = state.vehicleYaw - state.initialYaw;
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-relativeYaw));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-state.pitch));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(state.roll));
        VehicleStructure struct = state.structure;
        float initYawRad = (float)Math.toRadians(state.initialYaw);
        float axleX = (float)Math.cos(initYawRad);
        float axleZ = (float)Math.sin(initYawRad);
        RotationAxis rollAxis = RotationAxis.of((Vector3f)new Vector3f(axleX, 0.0f, axleZ));
        for (VehicleStructure.StoredBlock sb : state.blocks) {
            boolean isSteering;
            matrices.push();
            boolean isWheel = struct != null && struct.isWheel(sb.rx(), sb.ry(), sb.rz());
            boolean bl = isSteering = struct != null && struct.isSteeringWheel(sb.rx(), sb.ry(), sb.rz());
            if (isWheel) {
                matrices.translate(sb.rx(), sb.ry() + 0.5, sb.rz());
                if (isSteering) {
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-state.steeringAngle));
                }
                matrices.multiply(rollAxis.rotationDegrees(state.wheelRollAngle));
                matrices.translate(-0.5, -0.5, -0.5);
            } else {
                matrices.translate(sb.rx() - 0.5, sb.ry(), sb.rz() - 0.5);
            }
            if (sb.state().getRenderType() == BlockRenderType.MODEL) {
                blockRenderManager.renderBlockAsEntity(sb.state(), matrices, vertexConsumers, light, OverlayTexture.DEFAULT_UV);
            } else if (sb.state().getBlock() instanceof AbstractSignBlock) {
                this.renderSignBlock(client, sb, matrices, vertexConsumers, light);
            }
            matrices.pop();
        }
        for (VehicleStructure.StoredItemFrame frame : state.itemFrames) {
            ItemStack stack;
            matrices.push();
            matrices.translate(frame.rx(), frame.ry(), frame.rz());
            Direction facing = Direction.byName(frame.facingName());
            if (facing == null) {
                facing = Direction.NORTH;
            }
            float facingYaw = switch (facing) {
                case Direction.SOUTH -> 0.0f;
                case Direction.WEST -> 90.0f;
                case Direction.NORTH -> 180.0f;
                case Direction.EAST -> 270.0f;
                default -> 0.0f;
            };
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-facingYaw));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((float)frame.rotation() * 45.0f));
            if (frame.itemTag() != null && client.world != null && !(stack = ItemStack.fromNbtOrEmpty((RegistryWrapper.WrapperLookup)client.world.getRegistryManager(), (NbtCompound)frame.itemTag())).isEmpty()) {
                client.getItemRenderer().renderItem(stack, ModelTransformationMode.FIXED, light, OverlayTexture.DEFAULT_UV, matrices, vertexConsumers, client.world, 0);
            }
            matrices.pop();
        }
        matrices.pop();
        super.render(state, matrices, vertexConsumers, light);
    }

    private void renderSignBlock(MinecraftClient client, VehicleStructure.StoredBlock sb, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        if (client.world == null) {
            return;
        }
        BlockEntity be = this.blockEntityCache.computeIfAbsent(sb, s -> {
            SignBlockEntity sign = new SignBlockEntity(BlockPos.ORIGIN, s.state());
            if (s.blockEntityNbt() != null) {
                sign.read(s.blockEntityNbt(), (RegistryWrapper.WrapperLookup)client.world.getRegistryManager());
            }
            sign.setCachedState(s.state());
            return sign;
        });
        client.getBlockEntityRenderDispatcher().render(be, 0.0f, matrices, vertexConsumers);
    }

    public Identifier getTexture(VehicleRenderState state) {
        return Identifier.of("minecraft", "textures/misc/white.png");
    }

    @Environment(value=EnvType.CLIENT)
    public static class VehicleRenderState
    extends EntityRenderState {
        public List<VehicleStructure.StoredBlock> blocks = List.of();
        public List<VehicleStructure.StoredItemFrame> itemFrames = List.of();
        public VehicleStructure structure;
        public float vehicleYaw;
        public float initialYaw;
        public float speed;
        public float pitch;
        public float roll;
        public float wheelRollAngle;
        public float steeringAngle;
        public float visualYOffset;
        public float tiltLift;
    }
}

