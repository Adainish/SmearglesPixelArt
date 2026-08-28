package com.adainish.smearglespixelart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

class SmeargleSupportColumnTest {
    @Test
    void buildsSupportUpToHigherPlacements() {
        SmeargleSupportColumn supportColumn = SmeargleSupportColumn.forPlacement(
            CanvasDirection.NORTH,
            new BlockPos(10, 64, 10),
            new PixelArtTemplate.BlockPlacement(2, 3, 0, "minecraft:white_concrete")
        );

        assertEquals(new BlockPos(12, 67, 9), supportColumn.anchor());
        assertEquals(67, supportColumn.standingY());
        assertEquals(
            java.util.Set.of(
                new BlockPos(12, 64, 9),
                new BlockPos(12, 65, 9),
                new BlockPos(12, 66, 9)
            ),
            supportColumn.supportBlocks()
        );
    }

    @Test
    void usesNoSupportAtCanvasBase() {
        SmeargleSupportColumn supportColumn = SmeargleSupportColumn.forPlacement(
            CanvasDirection.WEST,
            new BlockPos(5, 70, 5),
            new PixelArtTemplate.BlockPlacement(0, 0, 1, "minecraft:black_concrete")
        );

        assertEquals(new BlockPos(3, 70, 5), supportColumn.anchor());
        assertEquals(70, supportColumn.standingY());
        assertTrue(supportColumn.supportBlocks().isEmpty());
    }

    @Test
    void supportShrinksAgainWhenPaintingLower() {
        BlockPos origin = new BlockPos(0, 50, 0);
        SmeargleSupportColumn highColumn = SmeargleSupportColumn.forPlacement(
            CanvasDirection.EAST,
            origin,
            new PixelArtTemplate.BlockPlacement(1, 4, 0, "minecraft:red_concrete")
        );
        SmeargleSupportColumn lowColumn = SmeargleSupportColumn.forPlacement(
            CanvasDirection.EAST,
            origin,
            new PixelArtTemplate.BlockPlacement(1, 1, 0, "minecraft:red_concrete")
        );

        assertEquals(
            java.util.Set.of(
                new BlockPos(1, 50, 1),
                new BlockPos(1, 51, 1),
                new BlockPos(1, 52, 1),
                new BlockPos(1, 53, 1)
            ),
            highColumn.supportBlocks()
        );
        assertEquals(java.util.Set.of(new BlockPos(1, 50, 1)), lowColumn.supportBlocks());
    }
}
