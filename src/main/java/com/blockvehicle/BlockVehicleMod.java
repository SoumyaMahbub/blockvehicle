package com.blockvehicle;

import com.blockvehicle.ModBlocks;
import com.blockvehicle.ModEntities;
import com.blockvehicle.ModItems;
import com.blockvehicle.command.RemoveVehicleCommand;
import com.blockvehicle.network.ModNetworking;
import com.blockvehicle.sound.ModSounds;
import com.blockvehicle.vehicle.ActivationConfirmManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlockVehicleMod
implements ModInitializer {
    public static final String MOD_ID = "blockvehicle";
    public static final Logger LOGGER = LoggerFactory.getLogger("blockvehicle");
    private int tickCounter = 0;

    public void onInitialize() {
        LOGGER.info("BlockVehicle mod initializing...");
        ModBlocks.register();
        ModItems.register();
        ModEntities.register();
        ModSounds.register();
        ModNetworking.registerServerHandlers();
        CommandRegistrationCallback.EVENT.register(RemoveVehicleCommand::register);
        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            String text = message.getContent().getString();
            ActivationConfirmManager.onChatMessage(sender, text);
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ++this.tickCounter;
            if (this.tickCounter >= 20) {
                this.tickCounter = 0;
                ActivationConfirmManager.tickCleanup();
            }
        });
        LOGGER.info("BlockVehicle mod initialized.");
    }
}

