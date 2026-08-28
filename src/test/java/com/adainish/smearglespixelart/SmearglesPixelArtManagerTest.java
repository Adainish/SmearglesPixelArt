package com.adainish.smearglespixelart;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SmearglesPixelArtManagerTest {
    @Test
    void usesFiveSecondCleanupDelay() {
        assertEquals(20 * 5, SmearglesPixelArtManager.CLEANUP_DELAY_TICKS);
    }

    @Test
    void cleanupRunsFasterThanPainting() {
        assertEquals(10, SmeargleCleanupPacing.ticksPerPlacement(20));
        assertEquals(2, SmeargleCleanupPacing.ticksPerPlacement(5));
        assertEquals(1, SmeargleCleanupPacing.ticksPerPlacement(1));
        assertEquals(1, SmeargleCleanupPacing.ticksPerPlacement(3));
    }

    @Test
    void pointsDecreaseByGuessOrderWithMinimumOfOne() {
        assertEquals(10, RoundScoring.pointsForCorrectGuessOrder(0));
        assertEquals(9, RoundScoring.pointsForCorrectGuessOrder(1));
        assertEquals(1, RoundScoring.pointsForCorrectGuessOrder(9));
        assertEquals(1, RoundScoring.pointsForCorrectGuessOrder(40));
    }

    @Test
    void winnerNamesIncludesTiesAtTopScore() {
        Set<String> winners = Set.copyOf(RoundScoring.winnerNames(Map.of(
            "Alice", 18,
            "Bob", 21,
            "Caro", 21,
            "Dan", 4
        )));
        assertEquals(Set.of("Bob", "Caro"), winners);
    }
}
