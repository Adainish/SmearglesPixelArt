package com.adainish.smearglespixelart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class SmeargleFrustrationSequenceTest {
    @Test
    void startsStillThenWalksTowardAudience() {
        List<SmeargleFrustrationSequence.Step> steps = SmeargleFrustrationSequence.stepsForStage(1);
        int pause = SmeargleFrustrationSequence.PAUSE_TICKS;

        assertTrue(steps.size() >= 60);
        assertEquals(0.0D, steps.getFirst().forwardOffset());
        assertEquals(SmeargleFrustrationSequence.FacingMode.ART, steps.getFirst().facingMode());
        assertEquals(SmeargleFrustrationSequence.FacingMode.ART, steps.get(pause - 1).facingMode());
        assertEquals(SmeargleFrustrationSequence.FacingMode.AUDIENCE, steps.get(pause).facingMode());
        assertEquals(0.0D, steps.get(pause).forwardOffset());
        assertEquals(SmeargleFrustrationSequence.FacingMode.AUDIENCE, steps.get((pause * 2) - 1).facingMode());
        assertEquals(0.0D, steps.get((pause * 2) - 1).forwardOffset());
        assertTrue(steps.get(pause * 2).forwardOffset() > steps.get((pause * 2) - 1).forwardOffset());
        assertTrue(steps.get((pause * 2) + 1).forwardOffset() > steps.get(pause * 2).forwardOffset());
        assertTrue(steps.get((pause * 2) + 2).forwardOffset() > steps.get((pause * 2) + 1).forwardOffset());
        assertEquals(steps.get((pause * 2) + 4), steps.get((pause * 2) + 5));
    }

    @Test
    void looksBackAndEndsReadyToPaintAgain() {
        List<SmeargleFrustrationSequence.Step> steps = SmeargleFrustrationSequence.stepsForStage(1);
        int pause = SmeargleFrustrationSequence.PAUSE_TICKS;

        int lookBackIndex = -1;
        for (int index = 0; index < steps.size(); index++) {
            SmeargleFrustrationSequence.Step step = steps.get(index);
            if (step.facingMode() == SmeargleFrustrationSequence.FacingMode.ART && step.forwardOffset() > 0.0D) {
                lookBackIndex = index;
                break;
            }
        }

        assertTrue(lookBackIndex > 0);
        assertTrue(steps.size() > lookBackIndex + pause);
        double lookBackOffset = steps.get(lookBackIndex).forwardOffset();
        assertEquals(steps.get(lookBackIndex), steps.get(lookBackIndex + 1));
        assertEquals(steps.get(lookBackIndex + 1), steps.get(lookBackIndex + 2));
        assertTrue(steps.stream().skip(lookBackIndex + 1).anyMatch(step -> step.facingMode() == SmeargleFrustrationSequence.FacingMode.ART && step.forwardOffset() < lookBackOffset));
        double retreatFloor = steps.subList(lookBackIndex, steps.size()).stream()
            .filter(step -> step.facingMode() == SmeargleFrustrationSequence.FacingMode.ART)
            .mapToDouble(SmeargleFrustrationSequence.Step::forwardOffset)
            .min()
            .orElse(0.0D);
        double surgePeak = steps.subList(lookBackIndex + pause, steps.size()).stream()
            .mapToDouble(SmeargleFrustrationSequence.Step::forwardOffset)
            .max()
            .orElse(0.0D);
        assertTrue(surgePeak > retreatFloor);
        assertEquals(steps.get(steps.size() - 2), steps.getLast());
        assertEquals(0.0D, steps.getLast().forwardOffset());
        assertEquals(SmeargleFrustrationSequence.FacingMode.ART, steps.getLast().facingMode());
    }

    @Test
    void higherStagesGetLongerAndAngrier() {
        List<SmeargleFrustrationSequence.Step> annoyed = SmeargleFrustrationSequence.stepsForStage(1);
        List<SmeargleFrustrationSequence.Step> frustrated = SmeargleFrustrationSequence.stepsForStage(2);
        List<SmeargleFrustrationSequence.Step> furious = SmeargleFrustrationSequence.stepsForStage(3);

        assertTrue(frustrated.size() > annoyed.size());
        assertTrue(furious.size() > frustrated.size());
        assertFalse(annoyed.stream().anyMatch(SmeargleFrustrationSequence.Step::jump));
        assertTrue(frustrated.stream().anyMatch(SmeargleFrustrationSequence.Step::jump));
        assertTrue(furious.stream().filter(SmeargleFrustrationSequence.Step::jump).count() > frustrated.stream().filter(SmeargleFrustrationSequence.Step::jump).count());
        assertTrue(furious.stream().filter(SmeargleFrustrationSequence.Step::particles).count() >= frustrated.stream().filter(SmeargleFrustrationSequence.Step::particles).count());
    }
}
