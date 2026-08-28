package com.adainish.smearglespixelart;

import java.util.ArrayList;
import java.util.List;

final class SmeargleFrustrationSequence {
    private static final int PAUSE_TICKS = 8;

    private SmeargleFrustrationSequence() {
    }

    static List<Step> stepsForStage(int stage) {
        int clampedStage = Math.max(1, Math.min(SmeargleAngerMeter.MAX_STAGE, stage));
        List<Step> steps = new ArrayList<>();

        hold(steps, step(0.0D, FacingMode.ART), PAUSE_TICKS);
        hold(steps, step(0.0D, FacingMode.AUDIENCE), PAUSE_TICKS);
        steps.add(step(0.25D, FacingMode.AUDIENCE));
        steps.add(step(0.5D, FacingMode.AUDIENCE));
        steps.add(step(0.75D, FacingMode.AUDIENCE));
        steps.add(step(1.0D, FacingMode.AUDIENCE));
        steps.add(step(1.25D, FacingMode.AUDIENCE));
        hold(steps, step(1.25D, FacingMode.AUDIENCE, clampedStage >= 2, clampedStage >= 2), PAUSE_TICKS);

        if (clampedStage >= 2) {
            hold(steps, step(1.45D, FacingMode.AUDIENCE, true, true), PAUSE_TICKS);
        }
        if (clampedStage >= 3) {
            hold(steps, step(1.7D, FacingMode.AUDIENCE, true, true), PAUSE_TICKS);
        }

        hold(steps, step(1.25D, FacingMode.ART, false, clampedStage >= 2), PAUSE_TICKS);
        steps.add(step(1.0D, FacingMode.ART));
        steps.add(step(0.75D, FacingMode.ART));
        steps.add(step(0.5D, FacingMode.ART));
        steps.add(step(0.35D, FacingMode.ART));
        hold(steps, step(0.35D, FacingMode.ART, clampedStage >= 2, clampedStage >= 2), PAUSE_TICKS);

        if (clampedStage >= 3) {
            steps.add(step(0.35D, FacingMode.ART, true, true));
        }

        steps.add(step(0.65D, FacingMode.AUDIENCE, false, clampedStage >= 2));
        steps.add(step(1.05D, FacingMode.AUDIENCE, clampedStage >= 2, clampedStage >= 2));
        steps.add(step(1.45D, FacingMode.AUDIENCE, clampedStage >= 2, clampedStage >= 2));
        steps.add(step(clampedStage >= 3 ? 1.95D : 1.7D, FacingMode.AUDIENCE, clampedStage >= 2, true));
        steps.add(step(clampedStage >= 3 ? 2.1D : 1.85D, FacingMode.AUDIENCE, clampedStage >= 2, true));
        hold(steps, step(1.25D, FacingMode.AUDIENCE, false, clampedStage >= 2), PAUSE_TICKS);
        hold(steps, step(0.0D, FacingMode.ART), PAUSE_TICKS);

        return List.copyOf(steps);
    }

    static int pauseTicks() {
        return PAUSE_TICKS;
    }

    private static void hold(List<Step> steps, Step step, int ticks) {
        for (int count = 0; count < ticks; count++) {
            steps.add(step);
        }
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
