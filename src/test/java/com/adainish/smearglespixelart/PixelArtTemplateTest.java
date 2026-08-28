package com.adainish.smearglespixelart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Comparator;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class PixelArtTemplateTest {
    private static final Comparator<PixelArtTemplate.BlockPlacement> BY_COORDINATE = Comparator
        .comparingInt(PixelArtTemplate.BlockPlacement::y)
        .thenComparingInt(PixelArtTemplate.BlockPlacement::x)
        .thenComparingInt(PixelArtTemplate.BlockPlacement::z);

    @Test
    void fillPatternProgressesByRoundDifficulty() {
        assertEquals(PixelArtTemplate.FillPattern.ROW_SWEEP, PixelArtTemplate.fillPatternForRound(1, 5));
        assertEquals(PixelArtTemplate.FillPattern.ZIG_ZAG_ROWS, PixelArtTemplate.fillPatternForRound(2, 5));
        assertEquals(PixelArtTemplate.FillPattern.COLUMN_SWEEP, PixelArtTemplate.fillPatternForRound(3, 5));
        assertEquals(PixelArtTemplate.FillPattern.CENTER_OUT, PixelArtTemplate.fillPatternForRound(4, 5));
        assertEquals(PixelArtTemplate.FillPattern.RANDOMIZED, PixelArtTemplate.fillPatternForRound(5, 5));
    }

    @Test
    void randomizedPatternShufflesOrderButKeepsAllBlocks() {
        PixelArtTemplate template = template();
        List<PixelArtTemplate.BlockPlacement> base = template.revealOrder();
        List<PixelArtTemplate.BlockPlacement> randomized = template.revealOrder(PixelArtTemplate.FillPattern.RANDOMIZED, new Random(2L));

        assertEquals(base.size(), randomized.size());
        assertEquals(base.stream().sorted(BY_COORDINATE).toList(), randomized.stream().sorted(BY_COORDINATE).toList());
        assertNotEquals(base, randomized);
    }

    private static PixelArtTemplate template() {
        return new PixelArtTemplate(
            "Bulbasaur",
            "bulbasaur",
            3,
            2,
            List.of(
                new PixelArtTemplate.BlockPlacement(0, 0, 0, "minecraft:white_wool"),
                new PixelArtTemplate.BlockPlacement(1, 0, 0, "minecraft:white_wool"),
                new PixelArtTemplate.BlockPlacement(2, 0, 0, "minecraft:white_wool"),
                new PixelArtTemplate.BlockPlacement(0, 1, 0, "minecraft:white_wool"),
                new PixelArtTemplate.BlockPlacement(1, 1, 0, "minecraft:white_wool"),
                new PixelArtTemplate.BlockPlacement(2, 1, 0, "minecraft:white_wool")
            )
        );
    }
}
