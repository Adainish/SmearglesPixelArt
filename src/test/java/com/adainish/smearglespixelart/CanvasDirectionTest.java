package com.adainish.smearglespixelart;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

class CanvasDirectionTest {
    @Test
    void parsesCardinalAndDiagonalAliases() {
        assertEquals(CanvasDirection.NORTH, CanvasDirection.parse("north"));
        assertEquals(CanvasDirection.NORTHWEST, CanvasDirection.parse("north-west"));
        assertEquals(CanvasDirection.NORTHEAST, CanvasDirection.parse("NE"));
        assertEquals(CanvasDirection.SOUTHWEST, CanvasDirection.parse("south_west"));
    }

    @Test
    void rotatesTemplateOffsets() {
        PixelArtTemplate.BlockPlacement placement = new PixelArtTemplate.BlockPlacement(2, 3, 0, "minecraft:white_concrete");

        assertEquals(new BlockPos(2, 3, 0), CanvasDirection.NORTH.worldOffset(placement));
        assertEquals(new BlockPos(0, 3, 2), CanvasDirection.EAST.worldOffset(placement));
        assertEquals(new BlockPos(2, 3, -2), CanvasDirection.NORTHWEST.worldOffset(placement));
    }
}
