package com.adainish.smearglespixelart;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SmearglesPixelArtManagerTest {
    @Test
    void usesFiveSecondCleanupDelay() {
        assertEquals(100, SmearglesPixelArtManager.CLEANUP_DELAY_TICKS);
    }
}
