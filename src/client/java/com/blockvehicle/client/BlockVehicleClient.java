package com.blockvehicle.client;

import com.blockvehicle.ModEntities;
import com.blockvehicle.client.hud.VehicleHud;
import com.blockvehicle.client.hud.WandHud;
import com.blockvehicle.client.input.VehicleInputHandler;
import com.blockvehicle.client.renderer.VehicleEntityRenderer;
import com.blockvehicle.client.renderer.WandWorldRenderer;
import com.blockvehicle.client.sound.VehicleSoundManager;
import com.blockvehicle.client.wand.ClientWandStore;
import com.blockvehicle.entity.VehicleEntity;
import com.blockvehicle.network.VehicleSyncPayload;
import com.blockvehicle.network.VehicleImpactPayload;
import com.blockvehicle.network.PlaneSyncPayload;
import com.blockvehicle.network.WandSyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;

@Environment(value=EnvType.CLIENT)
public class BlockVehicleClient
implements ClientModInitializer {
    public static final Identifier HUD_LAYER = Identifier.of("blockvehicle", "vehicle_hud");

    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.VEHICLE, VehicleEntityRenderer::new);
        VehicleInputHandler.register();
        HudRenderCallback.EVENT.register(VehicleHud::render);
        HudRenderCallback.EVENT.register(WandHud::render);
        WorldRenderEvents.AFTER_TRANSLUCENT.register(WandWorldRenderer::render);
        ClientPlayNetworking.registerGlobalReceiver(VehicleSyncPayload.ID, (payload, context) -> context.client().execute(() -> {
            MinecraftClient client = context.client();
            if (client.world == null) {
                return;
            }
            Entity entity = client.world.getEntityById(payload.entityId());
            if (entity instanceof VehicleEntity) {
                VehicleEntity vehicle = (VehicleEntity)entity;
                vehicle.applyServerTelemetry(payload.speed(), payload.pitch(), payload.roll(), payload.angularVelocity());
            }
        }));
        ClientPlayNetworking.registerGlobalReceiver(VehicleImpactPayload.ID, (payload, context) -> context.client().execute(() -> {
            MinecraftClient client = context.client();
            if (client.world == null) {
                return;
            }
            Entity entity = client.world.getEntityById(payload.entityId());
            if (entity instanceof VehicleEntity vehicle) {
                vehicle.setSpeed(payload.resultingSpeed());
                vehicle.applyCollisionImpulse(payload.impulseX(), payload.impulseY(), payload.impulseZ(), payload.angularImpulse());
            }
        }));
        ClientPlayNetworking.registerGlobalReceiver(PlaneSyncPayload.ID, (payload, context) -> context.client().execute(() -> {
            MinecraftClient client = context.client();
            if (client.world == null) return;
            Entity entity = client.world.getEntityById(payload.entityId());
            if (entity instanceof VehicleEntity vehicle) vehicle.applyPlaneServerTelemetry(payload);
        }));
        ClientPlayNetworking.registerGlobalReceiver(WandSyncPayload.ID, (payload, context) -> context.client().execute(() -> ClientWandStore.applySync(payload)));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientWandStore.reset());
        ClientTickEvents.START_CLIENT_TICK.register(VehicleInputHandler::onStartTick);
        ClientTickEvents.END_CLIENT_TICK.register(VehicleInputHandler::onEndTick);
        ClientTickEvents.END_CLIENT_TICK.register(VehicleSoundManager::onClientTick);
    }
}
