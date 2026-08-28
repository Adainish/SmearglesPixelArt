package com.adainish.smearglespixelart;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
