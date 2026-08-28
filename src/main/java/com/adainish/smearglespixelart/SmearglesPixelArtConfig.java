package com.adainish.smearglespixelart;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class SmearglesPixelArtConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DEFAULT_DIMENSION = "minecraft:overworld";
    private static final int DEFAULT_CANVAS_Y = 80;
    private static final int DEFAULT_TICKS_PER_PLACEMENT = 20;

    private String dimension = DEFAULT_DIMENSION;
    private CanvasOrigin canvasOrigin = new CanvasOrigin();
    private int ticksPerPlacement = DEFAULT_TICKS_PER_PLACEMENT;

    public static SmearglesPixelArtConfig load(Path path) throws IOException {
        Files.createDirectories(path.getParent());
        if (Files.notExists(path)) {
            SmearglesPixelArtConfig defaults = new SmearglesPixelArtConfig();
            defaults.save(path);
            return defaults;
        }

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            SmearglesPixelArtConfig loaded = GSON.fromJson(reader, SmearglesPixelArtConfig.class);
            if (loaded == null) {
                loaded = new SmearglesPixelArtConfig();
            }
            loaded.normalize();
            return loaded;
        } catch (JsonParseException exception) {
            throw new IOException("Unable to parse config file " + path, exception);
        }
    }

    public void save(Path path) throws IOException {
        Files.createDirectories(path.getParent());
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(this, writer);
        }
    }

    public RegistryKey<World> dimensionKey() {
        Identifier identifier = Identifier.tryParse(this.dimension);
        if (identifier == null) {
            identifier = Identifier.of("minecraft", "overworld");
        }
        return RegistryKey.of(RegistryKeys.WORLD, identifier);
    }

    public String dimension() {
        Identifier identifier = Identifier.tryParse(this.dimension);
        return identifier != null ? identifier.toString() : DEFAULT_DIMENSION;
    }

    public BlockPos canvasOrigin() {
        normalize();
        return new BlockPos(this.canvasOrigin.x, this.canvasOrigin.y, this.canvasOrigin.z);
    }

    public int ticksPerPlacement() {
        return Math.max(1, this.ticksPerPlacement);
    }

    private void normalize() {
        if (Identifier.tryParse(this.dimension) == null) {
            this.dimension = DEFAULT_DIMENSION;
        }
        if (this.canvasOrigin == null) {
            this.canvasOrigin = new CanvasOrigin();
        }
        this.ticksPerPlacement = Math.max(1, this.ticksPerPlacement);
    }

    private static final class CanvasOrigin {
        private int x = 0;
        private int y = DEFAULT_CANVAS_Y;
        private int z = 0;
    }
}
