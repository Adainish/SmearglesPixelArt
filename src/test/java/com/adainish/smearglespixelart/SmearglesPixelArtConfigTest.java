package com.adainish.smearglespixelart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SmearglesPixelArtConfigTest {
    @Test
    void createsDefaultConfigWhenMissing(@TempDir Path tempDir) throws IOException {
        Path path = tempDir.resolve("config.json");

        SmearglesPixelArtConfig config = SmearglesPixelArtConfig.load(path);

        assertTrue(Files.exists(path));
        assertEquals("minecraft:overworld", config.dimension());
        assertEquals(CanvasDirection.NORTH, config.direction());
        assertEquals(new BlockPos(0, 80, 0), config.canvasOrigin());
        assertEquals(20, config.ticksPerPlacement());
        assertEquals(3, config.angerMessagesForStage(1).size());
        assertEquals(3, config.angerMessagesForStage(2).size());
        assertEquals(3, config.angerMessagesForStage(3).size());
    }

    @Test
    void normalizesInvalidValues(@TempDir Path tempDir) throws IOException {
        Path path = tempDir.resolve("config.json");
        Files.writeString(
            path,
            """
            {
              "dimension": "not a valid id",
              "direction": "north-west",
              "canvasOrigin": {
                "x": 12,
                "y": 64,
                "z": -8
              },
              "ticksPerPlacement": 0,
              "angerMessages": {
                "annoyed": ["  <yellow>Custom annoyed</yellow>  ", "", null],
                "frustrated": [],
                "furious": ["<red>Custom furious</red>"]
              }
            }
            """
        );

        SmearglesPixelArtConfig config = SmearglesPixelArtConfig.load(path);

        assertEquals("minecraft:overworld", config.dimension());
        assertEquals(CanvasDirection.NORTHWEST, config.direction());
        assertEquals(new BlockPos(12, 64, -8), config.canvasOrigin());
        assertEquals(1, config.ticksPerPlacement());
        assertEquals(java.util.List.of("<yellow>Custom annoyed</yellow>"), config.angerMessagesForStage(1));
        assertEquals(3, config.angerMessagesForStage(2).size());
        assertEquals(java.util.List.of("<red>Custom furious</red>"), config.angerMessagesForStage(3));
    }

    @Test
    void selectsConfiguredMessageForStage(@TempDir Path tempDir) throws IOException {
        Path path = tempDir.resolve("config.json");
        Files.writeString(
            path,
            """
            {
              "angerMessages": {
                "annoyed": ["<yellow>First</yellow>", "<yellow>Second</yellow>"],
                "frustrated": ["<gold>Only frustrated</gold>"],
                "furious": ["<red>Only furious</red>"]
              }
            }
            """
        );

        SmearglesPixelArtConfig config = SmearglesPixelArtConfig.load(path);

        assertEquals("<yellow>First</yellow>", config.angerMessage(0, new FixedRandom(0)));
        assertEquals("<yellow>Second</yellow>", config.angerMessage(1, new FixedRandom(1)));
        assertEquals("<gold>Only frustrated</gold>", config.angerMessage(2, new FixedRandom(0)));
        assertEquals("<red>Only furious</red>", config.angerMessage(99, new FixedRandom(0)));
    }

    private static final class FixedRandom extends Random {
        private final int value;

        private FixedRandom(int value) {
            this.value = value;
        }

        @Override
        public int nextInt(int bound) {
            return Math.floorMod(value, bound);
        }
    }
}
