package com.adainish.smearglespixelart;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SmearglesPixelArtManagerTest {
    @Test
    void usesFiveSecondCleanupDelay() {
        assertEquals(20 * 5, SmearglesPixelArtManager.CLEANUP_DELAY_TICKS);
    }
}
