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

        assertEquals(0.0D, steps.getFirst().forwardOffset());
        assertEquals(SmeargleFrustrationSequence.FacingMode.ART, steps.getFirst().facingMode());
        assertEquals(SmeargleFrustrationSequence.FacingMode.ART, steps.get(1).facingMode());
        assertEquals(SmeargleFrustrationSequence.FacingMode.AUDIENCE, steps.get(2).facingMode());
        assertTrue(steps.get(3).forwardOffset() > steps.get(2).forwardOffset());
        assertTrue(steps.get(4).forwardOffset() > steps.get(3).forwardOffset());
        assertTrue(steps.get(5).forwardOffset() > steps.get(4).forwardOffset());
    }

    @Test
    void looksBackAndEndsReadyToPaintAgain() {
        List<SmeargleFrustrationSequence.Step> steps = SmeargleFrustrationSequence.stepsForStage(1);

        assertTrue(steps.stream().anyMatch(step -> step.facingMode() == SmeargleFrustrationSequence.FacingMode.ART && step.forwardOffset() > 0.0D));
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
