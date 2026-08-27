package com.adainish.smearglespixelart;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SmearglesPixelArtMod implements ModInitializer {
    public static final String MOD_ID = "smearglespixelart";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final SmearglesPixelArtManager MANAGER = new SmearglesPixelArtManager(PixelArtTemplateRegistry.loadBuiltins());

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register(SmearglesPixelArtCommands::register);
        ServerTickEvents.END_SERVER_TICK.register(MANAGER::tick);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> MANAGER.stop(server, "<gray>Server shutdown stopped the active Smeargle round.</gray>"));
        LOGGER.info("Initialized Smeargle's Mystery Pixel Art for Cobblemon.");
    }

    public static SmearglesPixelArtManager getManager() {
        return MANAGER;
    }
}
