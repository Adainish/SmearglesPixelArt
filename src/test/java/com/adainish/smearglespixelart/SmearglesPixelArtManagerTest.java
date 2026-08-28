package com.adainish.smearglespixelart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
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
    void buildPacingCapsLargeTemplatesAtFiveMinutes() {
        int blocksPerStep = SmeargleRoundPacing.buildBlocksPerStep(900, 20);
        int steps = Math.ceilDiv(900, blocksPerStep);

        assertEquals(3, blocksPerStep);
        assertTrue(steps * 20 <= SmeargleRoundPacing.MAX_BUILD_DURATION_TICKS);
    }

    @Test
    void cleanupPacingCapsLargeTemplatesAtFiveSecondsAfterDelay() {
        int cleanupTicksPerPlacement = SmeargleCleanupPacing.ticksPerPlacement(20);
        int blocksPerStep = SmeargleRoundPacing.cleanupBlocksPerStep(600, cleanupTicksPerPlacement);
        int steps = Math.ceilDiv(600, blocksPerStep);

        assertEquals(24, blocksPerStep);
        assertTrue(steps * cleanupTicksPerPlacement <= SmeargleRoundPacing.MAX_CLEANUP_DURATION_TICKS);
    }

    @Test
    void cleanupStillUsesFastPerTickCadence() {
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

        assertEquals(
            "<green><bold>Alice</bold></green> <gray>got the answer for</gray> <gold>7</gold> <gray>points (total: 19).</gray>",
            message
        );
    }

    @Test
    void correctGuessAnnouncementUsesSingularPointWhenNeeded() {
        String message = GuessAnnouncementFormatter.correctGuessAnnouncement("Alice", 1, 19);

        assertEquals(
            "<green><bold>Alice</bold></green> <gray>got the answer for</gray> <gold>1</gold> <gray>point (total: 19).</gray>",
            message
        );
    }

    @Test
    void blocksChatForRegisteredPlayersDuringActiveRounds() {
        UUID playerId = UUID.randomUUID();

        assertTrue(SmeargleChatGate.shouldBlock(true, false, Set.of(playerId), playerId));
    }

    @Test
    void doesNotBlockChatBeforeRoundStarts() {
        UUID playerId = UUID.randomUUID();

        assertFalse(SmeargleChatGate.shouldBlock(false, false, Set.of(playerId), playerId));
    }

    @Test
    void doesNotBlockChatForUnregisteredPlayers() {
        UUID playerId = UUID.randomUUID();

        assertFalse(SmeargleChatGate.shouldBlock(true, false, Set.of(UUID.randomUUID()), playerId));
    }

    @Test
    void doesNotBlockChatForPlayersWithBypassPermission() {
        UUID playerId = UUID.randomUUID();

        assertFalse(SmeargleChatGate.shouldBlock(true, true, Set.of(playerId), playerId));
    }
}
