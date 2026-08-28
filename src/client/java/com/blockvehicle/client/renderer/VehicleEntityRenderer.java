package com.blockvehicle.client.renderer;

import com.blockvehicle.entity.VehicleEntity;
import com.blockvehicle.vehicle.VehicleStructure;
import com.blockvehicle.vehicle.PlaneDefinition;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.AbstractSignBlock;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GlUsage;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.BufferAllocator;
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
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;

@Environment(value=EnvType.CLIENT)
public class VehicleEntityRenderer
extends EntityRenderer<VehicleEntity, VehicleEntityRenderer.VehicleRenderState> {
    private final Map<VehicleStructure.StoredBlock, BlockEntity> blockEntityCache = new WeakHashMap<>();
    private final Map<Integer, CachedMesh> meshCache = new HashMap<>();
    private int renderCalls = 0;
    private World meshWorld;

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
        state.entityId = entity.getId();
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
        state.isPlane = entity.isPlane();
        state.aircraftOrientation = entity.getPrevRelativeAircraftOrientation().slerp(entity.getRelativeAircraftOrientation(), tickDelta).normalize();
        float propellerDelta = MathHelper.wrapDegrees(entity.getPropellerAngle() - entity.getPrevPropellerAngle());
        state.propellerAngle = MathHelper.wrapDegrees(entity.getPrevPropellerAngle() + propellerDelta * tickDelta);
        state.engineRpm = entity.getEngineRpm();
    }

    public void render(VehicleRenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        if (state.blocks.isEmpty() && state.itemFrames.isEmpty()) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (this.meshWorld != client.world) {
            for (CachedMesh mesh : this.meshCache.values()) {
                mesh.close();
            }
            this.meshCache.clear();
            this.blockEntityCache.clear();
            this.meshWorld = client.world;
        }
        BlockRenderManager blockRenderManager = client.getBlockRenderManager();
        matrices.push();
        VehicleStructure struct = state.structure;
        matrices.translate(0.0, (double)(state.visualYOffset + (state.isPlane ? 0.0f : state.tiltLift)), 0.0);
        if (state.isPlane && struct != null && struct.getPlaneDefinition() != null) {
            net.minecraft.util.math.Vec3d pivot = struct.getPlaneDefinition().centerOfMass();
            matrices.translate(pivot.x, pivot.y, pivot.z);
            matrices.multiply(state.aircraftOrientation);
            matrices.translate(-pivot.x, -pivot.y, -pivot.z);
        } else {
            float relativeYaw = state.vehicleYaw - state.initialYaw;
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-relativeYaw));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-state.pitch));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(state.roll));
        }
        float initYawRad = (float)Math.toRadians(state.initialYaw);
        float axleX = (float)Math.cos(initYawRad);
        float axleZ = (float)Math.sin(initYawRad);
        RotationAxis rollAxis = RotationAxis.of((Vector3f)new Vector3f(axleX, 0.0f, axleZ));
        List<VehicleStructure.StoredBlock> dynamicBlocks = state.blocks;
        if (struct != null) {
            CachedMesh cached = this.meshCache.get(state.entityId);
            if (cached == null || cached.structure != struct) {
                if (cached != null) {
                    cached.close();
                }
                cached = this.buildStaticMesh(client, struct, light);
                this.meshCache.put(state.entityId, cached);
            }
            cached.draw(matrices);
            cached.drawPropellers(matrices, state.propellerAngle);
            dynamicBlocks = cached.dynamicBlocks;
        }
        for (VehicleStructure.StoredBlock sb : dynamicBlocks) {
            boolean isSteering;
            boolean isWheel = struct != null && struct.isWheel(sb.rx(), sb.ry(), sb.rz());
            boolean bl = isSteering = struct != null && struct.isSteeringWheel(sb.rx(), sb.ry(), sb.rz());
            PlaneDefinition.PropellerAssembly propeller = struct != null && struct.getPlaneDefinition() != null
                ? struct.getPlaneDefinition().getPropellerForBlade(sb.rx(), sb.ry(), sb.rz()) : null;
            if (!isWheel && propeller == null && this.isBakedStaticBlock(sb)) {
                continue;
            }
            matrices.push();
            if (propeller != null) {
                PlaneDefinition.Point hub = propeller.hub();
                matrices.translate(hub.rx(), hub.ry() + 0.5, hub.rz());
                Vector3f axis = new Vector3f((float)propeller.axis().x, (float)propeller.axis().y, (float)propeller.axis().z);
                matrices.multiply(RotationAxis.of(axis).rotationDegrees(state.propellerAngle * (propeller.clockwise() ? 1.0f : -1.0f)));
                matrices.translate(-hub.rx(), -(hub.ry() + 0.5), -hub.rz());
                matrices.translate(sb.rx() - 0.5, sb.ry(), sb.rz() - 0.5);
            } else if (isWheel) {
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
        this.cleanupMeshCache(client);
        super.render(state, matrices, vertexConsumers, light);
    }

    private boolean isBakedStaticBlock(VehicleStructure.StoredBlock block) {
        return block.state().getRenderType() == BlockRenderType.MODEL
            && !RenderLayers.getEntityBlockLayer(block.state()).isTranslucent();
    }

    private CachedMesh buildStaticMesh(MinecraftClient client, VehicleStructure structure, int light) {
        BlockRenderManager renderer = client.getBlockRenderManager();
        LinkedHashMap<RenderLayer, MeshBuilder> builders = new LinkedHashMap<>();
        int estimatedBytes = Math.max(1_048_576, structure.getRenderableBlocks().size() * 1536);
        VertexConsumerProvider provider = layer -> {
            MeshBuilder existing = builders.get(layer);
            if (existing != null) {
                return existing.builder;
            }
            BufferAllocator allocator = new BufferAllocator(Math.max(layer.getExpectedBufferSize(), estimatedBytes));
            MeshBuilder created = new MeshBuilder(allocator, new BufferBuilder(allocator, layer.getDrawMode(), layer.getVertexFormat()));
            builders.put(layer, created);
            return created.builder;
        };
        MatrixStack buildMatrices = new MatrixStack();
        ArrayList<VehicleStructure.StoredBlock> dynamicBlocks = new ArrayList<>();
        LinkedHashMap<PlaneDefinition.PropellerAssembly, List<VehicleStructure.StoredBlock>> propellerBlocks = new LinkedHashMap<>();
        for (VehicleStructure.StoredBlock block : structure.getRenderableBlocks()) {
            PlaneDefinition.PropellerAssembly propeller = structure.getPlaneDefinition() != null
                ? structure.getPlaneDefinition().getPropellerForBlade(block.rx(), block.ry(), block.rz()) : null;
            if (propeller != null && this.isBakedStaticBlock(block)) {
                propellerBlocks.computeIfAbsent(propeller, ignored -> new ArrayList<>()).add(block);
                continue;
            }
            if (structure.isWheel(block.rx(), block.ry(), block.rz()) || !this.isBakedStaticBlock(block)) {
                dynamicBlocks.add(block);
                continue;
            }
            buildMatrices.push();
            buildMatrices.translate(block.rx() - 0.5, block.ry(), block.rz() - 0.5);
            renderer.renderBlockAsEntity(block.state(), buildMatrices, provider, light, OverlayTexture.DEFAULT_UV);
            buildMatrices.pop();
        }
        ArrayList<MeshLayer> layers = new ArrayList<>();
        for (Map.Entry<RenderLayer, MeshBuilder> entry : builders.entrySet()) {
            MeshBuilder meshBuilder = entry.getValue();
            BuiltBuffer built = meshBuilder.builder.endNullable();
            if (built != null) {
                VertexBuffer vertexBuffer = new VertexBuffer(GlUsage.STATIC_WRITE);
                vertexBuffer.bind();
                vertexBuffer.upload(built);
                VertexBuffer.unbind();
                layers.add(new MeshLayer(entry.getKey(), vertexBuffer));
            }
            meshBuilder.allocator.close();
        }
        ArrayList<CachedPropellerMesh> propellerMeshes = new ArrayList<>();
        for (Map.Entry<PlaneDefinition.PropellerAssembly, List<VehicleStructure.StoredBlock>> entry : propellerBlocks.entrySet()) {
            propellerMeshes.add(new CachedPropellerMesh(entry.getKey(), this.buildBlockMesh(client, entry.getValue(), light)));
        }
        return new CachedMesh(structure, layers, dynamicBlocks, propellerMeshes);
    }

    private List<MeshLayer> buildBlockMesh(MinecraftClient client, List<VehicleStructure.StoredBlock> blocks, int light) {
        BlockRenderManager renderer = client.getBlockRenderManager();
        LinkedHashMap<RenderLayer, MeshBuilder> builders = new LinkedHashMap<>();
        int estimatedBytes = Math.max(131_072, blocks.size() * 1536);
        VertexConsumerProvider provider = layer -> {
            MeshBuilder existing = builders.get(layer);
            if (existing != null) return existing.builder;
            BufferAllocator allocator = new BufferAllocator(Math.max(layer.getExpectedBufferSize(), estimatedBytes));
            MeshBuilder created = new MeshBuilder(allocator,
                new BufferBuilder(allocator, layer.getDrawMode(), layer.getVertexFormat()));
            builders.put(layer, created);
            return created.builder;
        };
        MatrixStack buildMatrices = new MatrixStack();
        for (VehicleStructure.StoredBlock block : blocks) {
            buildMatrices.push();
            buildMatrices.translate(block.rx() - 0.5, block.ry(), block.rz() - 0.5);
            renderer.renderBlockAsEntity(block.state(), buildMatrices, provider, light, OverlayTexture.DEFAULT_UV);
            buildMatrices.pop();
        }
        ArrayList<MeshLayer> layers = new ArrayList<>();
        for (Map.Entry<RenderLayer, MeshBuilder> entry : builders.entrySet()) {
            MeshBuilder builder = entry.getValue();
            BuiltBuffer built = builder.builder.endNullable();
            if (built != null) {
                VertexBuffer buffer = new VertexBuffer(GlUsage.STATIC_WRITE);
                buffer.bind();
                buffer.upload(built);
                VertexBuffer.unbind();
                layers.add(new MeshLayer(entry.getKey(), buffer));
            }
            builder.allocator.close();
        }
        return layers;
    }

    private void cleanupMeshCache(MinecraftClient client) {
        if (++this.renderCalls % 200 != 0) {
            return;
        }
        this.meshCache.entrySet().removeIf(entry -> {
            Entity entity = client.world != null ? client.world.getEntityById(entry.getKey()) : null;
            boolean stale = !(entity instanceof VehicleEntity vehicle) || vehicle.getStructure() != entry.getValue().structure;
            if (stale) {
                entry.getValue().close();
            }
            return stale;
        });
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
        public int entityId;
        public float vehicleYaw;
        public float initialYaw;
        public float speed;
        public float pitch;
        public float roll;
        public float wheelRollAngle;
        public float steeringAngle;
        public float visualYOffset;
        public float tiltLift;
        public boolean isPlane;
        public Quaternionf aircraftOrientation = new Quaternionf();
        public float propellerAngle;
        public float engineRpm;
    }

    private record MeshBuilder(BufferAllocator allocator, BufferBuilder builder) {
    }

    private record MeshLayer(RenderLayer layer, VertexBuffer buffer) {
        private void close() {
            this.buffer.close();
        }
    }

    private static final class CachedMesh {
        private final VehicleStructure structure;
        private final List<MeshLayer> layers;
        private final List<VehicleStructure.StoredBlock> dynamicBlocks;
        private final List<CachedPropellerMesh> propellerMeshes;

        private CachedMesh(VehicleStructure structure, List<MeshLayer> layers,
                           List<VehicleStructure.StoredBlock> dynamicBlocks,
                           List<CachedPropellerMesh> propellerMeshes) {
            this.structure = structure;
            this.layers = layers;
            this.dynamicBlocks = dynamicBlocks;
            this.propellerMeshes = propellerMeshes;
        }

        private void drawPropellers(MatrixStack matrices, float angle) {
            for (CachedPropellerMesh propeller : this.propellerMeshes) propeller.draw(matrices, angle);
        }

        private void draw(MatrixStack matrices) {
            if (this.layers.isEmpty()) {
                return;
            }
            Matrix4fStack modelView = RenderSystem.getModelViewStack();
            modelView.pushMatrix();
            modelView.mul(matrices.peek().getPositionMatrix());
            for (MeshLayer layer : this.layers) {
                layer.buffer.draw(layer.layer);
            }
            modelView.popMatrix();
        }

        private void close() {
            for (MeshLayer layer : this.layers) {
                layer.close();
            }
            for (CachedPropellerMesh propeller : this.propellerMeshes) propeller.close();
        }
    }

    private static final class CachedPropellerMesh {
        private final PlaneDefinition.PropellerAssembly assembly;
        private final List<MeshLayer> layers;

        private CachedPropellerMesh(PlaneDefinition.PropellerAssembly assembly, List<MeshLayer> layers) {
            this.assembly = assembly;
            this.layers = layers;
        }

        private void draw(MatrixStack matrices, float angle) {
            PlaneDefinition.Point hub = this.assembly.hub();
            matrices.push();
            matrices.translate(hub.rx(), hub.ry() + 0.5, hub.rz());
            Vector3f axis = new Vector3f((float)this.assembly.axis().x, (float)this.assembly.axis().y,
                (float)this.assembly.axis().z);
            matrices.multiply(RotationAxis.of(axis).rotationDegrees(angle * (this.assembly.clockwise() ? 1.0f : -1.0f)));
            matrices.translate(-hub.rx(), -(hub.ry() + 0.5), -hub.rz());
            if (!this.layers.isEmpty()) {
                Matrix4fStack modelView = RenderSystem.getModelViewStack();
                modelView.pushMatrix();
                modelView.mul(matrices.peek().getPositionMatrix());
                for (MeshLayer layer : this.layers) layer.buffer.draw(layer.layer);
                modelView.popMatrix();
            }
            matrices.pop();
        }

        private void close() {
            for (MeshLayer layer : this.layers) layer.close();
        }
    }
}
