package com.adainish.smearglespixelart;

final class SmeargleRoundPacing {
    static final int MAX_BUILD_DURATION_TICKS = 20 * 60 * 5;
    static final int MAX_CLEANUP_DURATION_TICKS = 20 * 5;

    private SmeargleRoundPacing() {
    }

    static int buildBlocksPerStep(int totalBlocks, int ticksPerStep) {
        return blocksPerStep(totalBlocks, ticksPerStep, MAX_BUILD_DURATION_TICKS);
    }

    static int cleanupBlocksPerStep(int totalBlocks, int ticksPerStep) {
        return blocksPerStep(totalBlocks, ticksPerStep, MAX_CLEANUP_DURATION_TICKS);
    }

    private static int blocksPerStep(int totalBlocks, int ticksPerStep, int maxDurationTicks) {
        int normalizedBlocks = Math.max(1, totalBlocks);
        int normalizedTicks = Math.max(1, ticksPerStep);
        int maxSteps = Math.max(1, maxDurationTicks / normalizedTicks);
        return Math.max(1, Math.ceilDiv(normalizedBlocks, maxSteps));
    }
}
