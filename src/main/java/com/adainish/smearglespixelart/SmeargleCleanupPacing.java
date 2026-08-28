package com.adainish.smearglespixelart;

final class SmeargleCleanupPacing {
    static final int CLEANUP_SPEED_MULTIPLIER = 2;

    private SmeargleCleanupPacing() {
    }

    static int ticksPerPlacement(int paintingTicksPerPlacement) {
        return Math.max(1, paintingTicksPerPlacement / CLEANUP_SPEED_MULTIPLIER);
    }
}
