package com.adainish.smearglespixelart;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import java.io.IOException;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SmearglesPixelArtMod implements ModInitializer {
    public static final String MOD_ID = "smearglespixelart";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final Path CONFIG_DIRECTORY = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID);
    private static final Path TEMPLATE_DIRECTORY = CONFIG_DIRECTORY.resolve("templates");
    private static final Path CONFIG_PATH = CONFIG_DIRECTORY.resolve("config.json");

    private static final PixelArtTemplateRegistry TEMPLATE_REGISTRY = PixelArtTemplateRegistry.loadBuiltins();
    private static volatile SmearglesPixelArtConfig config = new SmearglesPixelArtConfig();
    private static final SmearglesPixelArtManager MANAGER = new SmearglesPixelArtManager(TEMPLATE_REGISTRY, TEMPLATE_DIRECTORY, () -> config);

    @Override
    public void onInitialize() {
        try {
            config = SmearglesPixelArtConfig.load(CONFIG_PATH);
            TEMPLATE_REGISTRY.loadCustomTemplates(TEMPLATE_DIRECTORY);
        } catch (IOException | RuntimeException exception) {
            LOGGER.error("Unable to load Smeargle's Pixel Art configuration from {}", CONFIG_DIRECTORY, exception);
        }
        CommandRegistrationCallback.EVENT.register(SmearglesPixelArtCommands::register);
        ServerTickEvents.END_SERVER_TICK.register(MANAGER::tick);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> MANAGER.stop(server, "<gray>Server shutdown stopped the active Smeargle round.</gray>"));
        LOGGER.info("Initialized Smeargle's Mystery Pixel Art for Cobblemon.");
    }

    public static SmearglesPixelArtManager getManager() {
        return MANAGER;
    }

    public static SmearglesPixelArtConfig getConfig() {
        return config;
    }

    public static Path getConfigPath() {
        return CONFIG_PATH;
    }

    public static void reloadConfig() throws IOException {
        config = SmearglesPixelArtConfig.load(CONFIG_PATH);
    }
}
