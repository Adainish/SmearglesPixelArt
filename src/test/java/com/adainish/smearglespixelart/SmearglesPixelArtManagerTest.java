package com.adainish.smearglespixelart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SmearglesPixelArtManagerTest {
    @Test
    void usesFiveSecondCleanupDelay() {
        assertEquals(20 * 5, SmearglesPixelArtManager.CLEANUP_DELAY_TICKS);
    }

    @Test
    void usesTenMinuteRegistrationWindow() {
        assertEquals(20 * 60 * 10, SmearglesPixelArtManager.REGISTRATION_DURATION_TICKS);
    }

    @Test
    void cleanupRunsFasterThanPainting() {
        assertEquals(4, SmeargleCleanupPacing.ticksPerPlacement(20));
        assertEquals(1, SmeargleCleanupPacing.ticksPerPlacement(5));
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

    @Test
    void correctGuessAnnouncementDoesNotRevealAnswer() {
        String message = GuessAnnouncementFormatter.correctGuessAnnouncement("Alice", 7, 19);

        assertTrue(message.contains("Alice"));
        assertTrue(message.contains("got the answer"));
        assertTrue(message.contains("7"));
        assertTrue(message.contains("19"));
        assertFalse(message.contains("guessed</gray> <gold>"));
    }
}
