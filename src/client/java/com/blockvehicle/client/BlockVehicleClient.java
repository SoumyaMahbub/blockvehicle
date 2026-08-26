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
import com.blockvehicle.network.WandSyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
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
                vehicle.applyServerSync(payload.x(), payload.y(), payload.z(), payload.yaw(), payload.speed(), payload.pitch(), payload.roll(), payload.angularVelocity());
            }
        }));
        ClientPlayNetworking.registerGlobalReceiver(WandSyncPayload.ID, (payload, context) -> context.client().execute(() -> ClientWandStore.applySync(payload)));
        ClientTickEvents.START_CLIENT_TICK.register(VehicleInputHandler::onStartTick);
        ClientTickEvents.END_CLIENT_TICK.register(VehicleInputHandler::onEndTick);
        ClientTickEvents.END_CLIENT_TICK.register(VehicleSoundManager::onClientTick);
    }
}

