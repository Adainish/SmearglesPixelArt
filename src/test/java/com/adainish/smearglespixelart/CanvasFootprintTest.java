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

        CanvasFootprint combined = CanvasFootprint.of(smaller, CanvasDirection.NORTHWEST)
            .covering(CanvasFootprint.of(wider, CanvasDirection.NORTHWEST));

        assertEquals(0, combined.minX());
        assertEquals(11, combined.maxX());
        assertEquals(9, combined.maxY());
        assertEquals(-11, combined.minZ());
        assertEquals(0, combined.maxZ());
    }

    @Test
    void rotatesSpriteBoundsForCardinalDirections() {
        PixelArtTemplate template = new PixelArtTemplate(
            "Pikachu",
            "pikachu",
            3,
            2,
            List.of(new PixelArtTemplate.BlockPlacement(0, 0, 0, "minecraft:yellow_concrete"))
        );

        CanvasFootprint east = CanvasFootprint.of(template, CanvasDirection.EAST);

        assertEquals(0, east.minX());
        assertEquals(0, east.maxX());
        assertEquals(0, east.minY());
        assertEquals(1, east.maxY());
        assertEquals(0, east.minZ());
        assertEquals(2, east.maxZ());
    }
}
