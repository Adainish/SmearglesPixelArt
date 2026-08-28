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
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class SmearglesPixelArtConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DEFAULT_DIMENSION = "minecraft:overworld";
    private static final String DEFAULT_DIRECTION = "north";
    private static final int DEFAULT_CANVAS_Y = 80;
    private static final int DEFAULT_TICKS_PER_PLACEMENT = 20;

    private String dimension = DEFAULT_DIMENSION;
    private String direction = DEFAULT_DIRECTION;
    private CanvasOrigin canvasOrigin = new CanvasOrigin();
    private int ticksPerPlacement = DEFAULT_TICKS_PER_PLACEMENT;
    private AngerMessages angerMessages = new AngerMessages();

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
        CanvasOrigin origin = this.canvasOrigin != null ? this.canvasOrigin : new CanvasOrigin();
        return new BlockPos(origin.x, origin.y, origin.z);
    }

    public CanvasDirection direction() {
        return CanvasDirection.parse(this.direction);
    }

    public int ticksPerPlacement() {
        return Math.max(1, this.ticksPerPlacement);
    }

    public String angerMessage(int stage, Random random) {
        return angerMessages.messageForStage(stage, random);
    }

    List<String> angerMessagesForStage(int stage) {
        return angerMessages.messagesForStage(stage);
    }

    private void normalize() {
        if (Identifier.tryParse(this.dimension) == null) {
            this.dimension = DEFAULT_DIMENSION;
        }
        this.direction = CanvasDirection.parse(this.direction).id();
        if (this.canvasOrigin == null) {
            this.canvasOrigin = new CanvasOrigin();
        }
        this.ticksPerPlacement = Math.max(1, this.ticksPerPlacement);
        if (this.angerMessages == null) {
            this.angerMessages = new AngerMessages();
        }
        this.angerMessages.normalize();
    }

    private static final class CanvasOrigin {
        private int x = 0;
        private int y = DEFAULT_CANVAS_Y;
        private int z = 0;
    }

    private static final class AngerMessages {
        private static final List<String> DEFAULT_ANNOYED_MESSAGES = List.of(
            "<yellow>Smeargle huffs and shoots the players a suspicious look.</yellow>",
            "<yellow>Smeargle taps his tail like he expects better guesses.</yellow>",
            "<yellow>Smeargle mutters and keeps one eye on the crowd.</yellow>"
        );
        private static final List<String> DEFAULT_FRUSTRATED_MESSAGES = List.of(
            "<gold>Smeargle stomps in place and glares back at the players.</gold>",
            "<gold>Smeargle grumbles loudly and jabs his tail at the canvas.</gold>",
            "<gold>Smeargle throws his paws up like the answer should be obvious.</gold>"
        );
        private static final List<String> DEFAULT_FURIOUS_MESSAGES = List.of(
            "<red><bold>Smeargle is furious and demands a better guess right now!</bold></red>",
            "<red><bold>Smeargle screeches, points at the artwork, and looks ready to explode!</bold></red>",
            "<red><bold>Smeargle is absolutely livid and kicks off another angry fit!</bold></red>"
        );

        private List<String> annoyed = new ArrayList<>(DEFAULT_ANNOYED_MESSAGES);
        private List<String> frustrated = new ArrayList<>(DEFAULT_FRUSTRATED_MESSAGES);
        private List<String> furious = new ArrayList<>(DEFAULT_FURIOUS_MESSAGES);

        private String messageForStage(int stage, Random random) {
            List<String> messages = messagesForStage(stage);
            return messages.get(random.nextInt(messages.size()));
        }

        private List<String> messagesForStage(int stage) {
            return switch (Math.max(1, Math.min(SmeargleAngerMeter.MAX_STAGE, stage))) {
                case 1 -> List.copyOf(normalizeList(annoyed, DEFAULT_ANNOYED_MESSAGES));
                case 2 -> List.copyOf(normalizeList(frustrated, DEFAULT_FRUSTRATED_MESSAGES));
                default -> List.copyOf(normalizeList(furious, DEFAULT_FURIOUS_MESSAGES));
            };
        }

        private void normalize() {
            this.annoyed = normalizeList(this.annoyed, DEFAULT_ANNOYED_MESSAGES);
            this.frustrated = normalizeList(this.frustrated, DEFAULT_FRUSTRATED_MESSAGES);
            this.furious = normalizeList(this.furious, DEFAULT_FURIOUS_MESSAGES);
        }

        private static List<String> normalizeList(List<String> source, List<String> defaults) {
            if (source == null) {
                return new ArrayList<>(defaults);
            }
            List<String> normalized = new ArrayList<>();
            for (String message : source) {
                if (message == null) {
                    continue;
                }
                String trimmed = message.trim();
                if (!trimmed.isEmpty()) {
                    normalized.add(trimmed);
                }
            }
            return normalized.isEmpty() ? new ArrayList<>(defaults) : normalized;
        }
    }
}
