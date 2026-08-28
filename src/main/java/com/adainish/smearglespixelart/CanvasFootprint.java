package com.adainish.smearglespixelart;

public record CanvasFootprint(int maxX, int maxY, int maxZ) {
    public CanvasFootprint {
        if (maxX < 0 || maxY < 0 || maxZ < 0) {
            throw new IllegalArgumentException("Canvas bounds must be non-negative.");
        }
    }

    public static CanvasFootprint of(PixelArtTemplate template) {
        return new CanvasFootprint(template.maxX(), template.maxY(), template.maxZ());
    }

    public CanvasFootprint covering(CanvasFootprint other) {
        return new CanvasFootprint(
            Math.max(this.maxX, other.maxX),
            Math.max(this.maxY, other.maxY),
            Math.max(this.maxZ, other.maxZ)
        );
    }
}
