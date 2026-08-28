package com.adainish.smearglespixelart;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class CanvasFootprintTest {
    @Test
    void expandsToCoverPreviousAndNextSpriteBounds() {
        PixelArtTemplate smaller = new PixelArtTemplate(
            "Voltorb",
            "voltorb",
            8,
            8,
            List.of(new PixelArtTemplate.BlockPlacement(0, 0, 0, "minecraft:red_concrete"))
        );
        PixelArtTemplate wider = new PixelArtTemplate(
            "Rayquaza",
            "rayquaza",
            12,
            10,
            List.of(new PixelArtTemplate.BlockPlacement(0, 0, 0, "minecraft:green_concrete"))
        );

        CanvasFootprint combined = CanvasFootprint.of(smaller).covering(CanvasFootprint.of(wider));

        assertEquals(11, combined.maxX());
        assertEquals(9, combined.maxY());
        assertEquals(0, combined.maxZ());
    }
}
