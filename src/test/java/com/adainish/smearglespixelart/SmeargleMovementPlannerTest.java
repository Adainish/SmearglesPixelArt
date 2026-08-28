package com.adainish.smearglespixelart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.Set;

class SmeargleMovementPlannerTest {
    @Test
    void repositionsToNewAnchorBeforeBuildingUp() {
        SmeargleSupportColumn targetColumn = SmeargleSupportColumn.forPlacement(
            CanvasDirection.NORTH,
            new BlockPos(10, 64, 10),
            new PixelArtTemplate.BlockPlacement(2, 3, 0, "minecraft:white_concrete")
        );

        SmeargleMovementPlanner.MovementFrame frame = SmeargleMovementPlanner.nextFrame(
            new BlockPos(9, 64, 9),
            67,
            Set.of(new BlockPos(9, 64, 9), new BlockPos(9, 65, 9), new BlockPos(9, 66, 9)),
            targetColumn,
            64
        );

        assertEquals(targetColumn.anchor(), frame.anchor());
        assertEquals(64, frame.standingY());
        assertEquals(Set.of(), frame.supportToAdd());
        assertEquals(Set.of(new BlockPos(9, 64, 9), new BlockPos(9, 65, 9), new BlockPos(9, 66, 9)), frame.supportToRemove());
        assertFalse(frame.readyToPaint());
    }

    @Test
    void buildsSupportOneLevelAtATime() {
        SmeargleSupportColumn targetColumn = SmeargleSupportColumn.forPlacement(
            CanvasDirection.NORTH,
            new BlockPos(10, 64, 10),
            new PixelArtTemplate.BlockPlacement(2, 3, 0, "minecraft:white_concrete")
        );

        SmeargleMovementPlanner.MovementFrame frame = SmeargleMovementPlanner.nextFrame(
            targetColumn.anchor(),
            64,
            Set.of(),
            targetColumn,
            64
        );

        assertEquals(targetColumn.anchor(), frame.anchor());
        assertEquals(65, frame.standingY());
        assertEquals(Set.of(new BlockPos(12, 64, 9)), frame.supportToAdd());
        assertEquals(Set.of(), frame.supportToRemove());
        assertFalse(frame.readyToPaint());
    }

    @Test
    void stepsDownBeforeRemovingHigherSupport() {
        SmeargleSupportColumn targetColumn = SmeargleSupportColumn.forPlacement(
            CanvasDirection.EAST,
            new BlockPos(0, 50, 0),
            new PixelArtTemplate.BlockPlacement(1, 1, 0, "minecraft:red_concrete")
        );

        SmeargleMovementPlanner.MovementFrame frame = SmeargleMovementPlanner.nextFrame(
            targetColumn.anchor(),
            54,
            Set.of(
                new BlockPos(1, 50, 1),
                new BlockPos(1, 51, 1),
                new BlockPos(1, 52, 1),
                new BlockPos(1, 53, 1)
            ),
            targetColumn,
            50
        );

        assertEquals(53, frame.standingY());
        assertEquals(Set.of(), frame.supportToAdd());
        assertEquals(Set.of(new BlockPos(1, 53, 1)), frame.supportToRemove());
        assertFalse(frame.readyToPaint());
    }

    @Test
    void reportsReadyWhenAlreadyInPlace() {
        SmeargleSupportColumn targetColumn = SmeargleSupportColumn.forPlacement(
            CanvasDirection.WEST,
            new BlockPos(5, 70, 5),
            new PixelArtTemplate.BlockPlacement(0, 0, 1, "minecraft:black_concrete")
        );

        SmeargleMovementPlanner.MovementFrame frame = SmeargleMovementPlanner.nextFrame(
            targetColumn.anchor(),
            targetColumn.standingY(),
            targetColumn.supportBlocks(),
            targetColumn,
            70
        );

        assertTrue(frame.readyToPaint());
    }
}
