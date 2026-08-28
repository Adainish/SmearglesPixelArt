package com.adainish.smearglespixelart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
              "ticksPerPlacement": 0
            }
            """
        );

        SmearglesPixelArtConfig config = SmearglesPixelArtConfig.load(path);

        assertEquals("minecraft:overworld", config.dimension());
        assertEquals(CanvasDirection.NORTHWEST, config.direction());
        assertEquals(new BlockPos(12, 64, -8), config.canvasOrigin());
        assertEquals(1, config.ticksPerPlacement());
    }
}
