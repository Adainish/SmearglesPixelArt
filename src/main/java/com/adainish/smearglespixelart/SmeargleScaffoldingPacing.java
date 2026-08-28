package com.adainish.smearglespixelart;

final class SmeargleScaffoldingPacing {
    static final double SCAFFOLDING_SPEED_MULTIPLIER = 2.8;

    private SmeargleScaffoldingPacing() {
    }

    static int ticksPerPlacement(int paintingTicksPerPlacement) {
        return Math.max(1, (int) (paintingTicksPerPlacement / SCAFFOLDING_SPEED_MULTIPLIER));
    }
}
