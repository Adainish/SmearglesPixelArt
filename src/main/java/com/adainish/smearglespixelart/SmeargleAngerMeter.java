package com.adainish.smearglespixelart;

final class SmeargleAngerMeter {
    static final int MAX_STAGE = 3;

    private SmeargleAngerMeter() {
    }

    static int stageForProgress(int revealedBlocks, int totalBlocks) {
        if (revealedBlocks <= 0 || totalBlocks <= 0) {
            return 0;
        }

        long scaledProgress = revealedBlocks * 4L;
        return Math.min(MAX_STAGE, (int) (scaledProgress / totalBlocks));
    }

    static String describe(int stage) {
        return switch (Math.max(0, Math.min(MAX_STAGE, stage))) {
            case 0 -> "calm";
            case 1 -> "annoyed";
            case 2 -> "frustrated";
            default -> "furious";
        };
    }
}
