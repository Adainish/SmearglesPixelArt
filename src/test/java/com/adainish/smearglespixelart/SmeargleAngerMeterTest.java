package com.adainish.smearglespixelart;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SmeargleAngerMeterTest {
    @Test
    void increasesInQuarterSteps() {
        assertEquals(0, SmeargleAngerMeter.stageForProgress(0, 8));
        assertEquals(0, SmeargleAngerMeter.stageForProgress(1, 8));
        assertEquals(1, SmeargleAngerMeter.stageForProgress(2, 8));
        assertEquals(2, SmeargleAngerMeter.stageForProgress(4, 8));
        assertEquals(3, SmeargleAngerMeter.stageForProgress(6, 8));
        assertEquals(3, SmeargleAngerMeter.stageForProgress(8, 8));
    }

    @Test
    void handlesSmallAndEmptyTemplates() {
        assertEquals(0, SmeargleAngerMeter.stageForProgress(1, 0));
        assertEquals(1, SmeargleAngerMeter.stageForProgress(1, 3));
        assertEquals(2, SmeargleAngerMeter.stageForProgress(2, 3));
        assertEquals(3, SmeargleAngerMeter.stageForProgress(3, 3));
    }

    @Test
    void describesStages() {
        assertEquals("calm", SmeargleAngerMeter.describe(0));
        assertEquals("annoyed", SmeargleAngerMeter.describe(1));
        assertEquals("frustrated", SmeargleAngerMeter.describe(2));
        assertEquals("furious", SmeargleAngerMeter.describe(3));
        assertEquals("furious", SmeargleAngerMeter.describe(99));
    }
}
