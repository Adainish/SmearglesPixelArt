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
        steps.add(step(0.0D, FacingMode.AUDIENCE));
        steps.add(step(0.0D, FacingMode.AUDIENCE));
        steps.add(step(0.25D, FacingMode.AUDIENCE));
        steps.add(step(0.5D, FacingMode.AUDIENCE));
        steps.add(step(0.75D, FacingMode.AUDIENCE));
        steps.add(step(1.0D, FacingMode.AUDIENCE));
        steps.add(step(1.25D, FacingMode.AUDIENCE));
        steps.add(step(1.25D, FacingMode.AUDIENCE, clampedStage >= 2, clampedStage >= 2));

        if (clampedStage >= 2) {
            steps.add(step(1.45D, FacingMode.AUDIENCE, true, true));
            steps.add(step(1.45D, FacingMode.AUDIENCE, true, true));
        }
        if (clampedStage >= 3) {
            steps.add(step(1.7D, FacingMode.AUDIENCE, true, true));
            steps.add(step(1.7D, FacingMode.AUDIENCE, true, true));
        }

        steps.add(step(1.25D, FacingMode.ART, false, clampedStage >= 2));
        steps.add(step(1.0D, FacingMode.ART));
        steps.add(step(0.75D, FacingMode.ART));
        steps.add(step(0.5D, FacingMode.ART));
        steps.add(step(0.35D, FacingMode.ART));
        steps.add(step(0.35D, FacingMode.ART, clampedStage >= 2, clampedStage >= 2));

        if (clampedStage >= 3) {
            steps.add(step(0.35D, FacingMode.ART, true, true));
        }

        steps.add(step(0.65D, FacingMode.AUDIENCE, false, clampedStage >= 2));
        steps.add(step(1.05D, FacingMode.AUDIENCE, clampedStage >= 2, clampedStage >= 2));
        steps.add(step(1.45D, FacingMode.AUDIENCE, clampedStage >= 2, clampedStage >= 2));
        steps.add(step(clampedStage >= 3 ? 1.95D : 1.7D, FacingMode.AUDIENCE, clampedStage >= 2, true));
        steps.add(step(clampedStage >= 3 ? 2.1D : 1.85D, FacingMode.AUDIENCE, clampedStage >= 2, true));
        steps.add(step(1.25D, FacingMode.AUDIENCE, false, clampedStage >= 2));
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
