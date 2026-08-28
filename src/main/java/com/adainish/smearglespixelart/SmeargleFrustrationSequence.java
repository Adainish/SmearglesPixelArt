package com.adainish.smearglespixelart;

import java.util.ArrayList;
import java.util.List;

final class SmeargleFrustrationSequence {
    private SmeargleFrustrationSequence() {
    }

    static List<Step> stepsForStage(int stage) {
        int clampedStage = Math.max(1, Math.min(SmeargleAngerMeter.MAX_STAGE, stage));
        List<Step> steps = new ArrayList<>();

        steps.add(step(0.0D, FacingMode.ART));
        steps.add(step(0.0D, FacingMode.ART));
        steps.add(step(0.25D, FacingMode.AUDIENCE));
        steps.add(step(0.55D, FacingMode.AUDIENCE));
        steps.add(step(0.85D, FacingMode.AUDIENCE));
        steps.add(step(1.15D, FacingMode.AUDIENCE));
        steps.add(step(1.15D, FacingMode.AUDIENCE));

        if (clampedStage >= 2) {
            steps.add(step(1.15D, FacingMode.AUDIENCE, true, true));
        }
        if (clampedStage >= 3) {
            steps.add(step(1.35D, FacingMode.AUDIENCE, true, true));
        }

        steps.add(step(1.15D, FacingMode.ART, false, clampedStage >= 2));
        steps.add(step(0.85D, FacingMode.ART));
        steps.add(step(0.55D, FacingMode.ART));
        steps.add(step(0.25D, FacingMode.ART));

        if (clampedStage >= 3) {
            steps.add(step(0.25D, FacingMode.ART, true, true));
        }

        steps.add(step(0.95D, FacingMode.AUDIENCE, false, clampedStage >= 2));
        steps.add(step(1.35D, FacingMode.AUDIENCE, clampedStage >= 2, clampedStage >= 2));
        steps.add(step(clampedStage >= 3 ? 1.75D : 1.55D, FacingMode.AUDIENCE, clampedStage >= 2, true));
        steps.add(step(0.0D, FacingMode.ART));

        return List.copyOf(steps);
    }

    private static Step step(double forwardOffset, FacingMode facingMode) {
        return new Step(forwardOffset, facingMode, false, false);
    }

    private static Step step(double forwardOffset, FacingMode facingMode, boolean jump, boolean particles) {
        return new Step(forwardOffset, facingMode, jump, particles);
    }

    enum FacingMode {
        ART,
        AUDIENCE
    }

    record Step(double forwardOffset, FacingMode facingMode, boolean jump, boolean particles) {
    }
}
