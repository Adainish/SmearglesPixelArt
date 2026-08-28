package com.adainish.smearglespixelart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.junit.jupiter.api.Test;

class SpriteTemplateRecorderTest {
    @Test
    void recordsTwoDimensionalSpriteRowsAndPalette() {
        SpriteTemplateRecorder.SpriteSelection selection = SpriteTemplateRecorder.SpriteSelection.between(
            new BlockPos(2, 5, 9),
            new BlockPos(4, 6, 9)
        );

        SpriteTemplateRecorder.RecordedTemplate recorded = SpriteTemplateRecorder.record("Pikachu", selection, pos -> {
            if (pos.equals(new BlockPos(2, 6, 9)) || pos.equals(new BlockPos(2, 5, 9))) {
                return "minecraft:yellow_concrete";
            }
            if (pos.equals(new BlockPos(3, 6, 9))) {
                return "minecraft:black_concrete";
            }
            return null;
        });

        assertEquals(Direction.Axis.X, selection.horizontalAxis());
        assertEquals(3, selection.width());
        assertEquals(2, selection.height());
        assertEquals(java.util.Map.of("1", "minecraft:yellow_concrete", "2", "minecraft:black_concrete"), recorded.palette());
        assertEquals(java.util.List.of("12.", "1.."), recorded.rows());
    }

    @Test
    void rejectsThreeDimensionalSelections() {
        assertThrows(
            IllegalArgumentException.class,
            () -> SpriteTemplateRecorder.SpriteSelection.between(new BlockPos(0, 0, 0), new BlockPos(1, 1, 1))
        );
    }

    @Test
    void rejectsDegenerateVerticalLineSelections() {
        assertThrows(
            IllegalArgumentException.class,
            () -> SpriteTemplateRecorder.SpriteSelection.between(new BlockPos(0, 0, 0), new BlockPos(0, 5, 0))
        );
    }
}
