package com.adainish.smearglespixelart;

public record CanvasFootprint(int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
    public CanvasFootprint {
        if (minX > maxX || minY > maxY || minZ > maxZ) {
            throw new IllegalArgumentException("Canvas bounds are invalid.");
        }
    }

    public static CanvasFootprint of(PixelArtTemplate template, CanvasDirection direction) {
        int minX = 0;
        int maxX = 0;
        int minY = 0;
        int maxY = 0;
        int minZ = 0;
        int maxZ = 0;

        for (int x = template.minX(); x <= template.maxX(); x++) {
            for (int y = template.minY(); y <= template.maxY(); y++) {
                for (int z = template.minZ(); z <= template.maxZ(); z++) {
                    PixelArtTemplate.BlockPlacement placement = new PixelArtTemplate.BlockPlacement(x, y, z, "minecraft:air");
                    var offset = direction.worldOffset(placement);
                    minX = Math.min(minX, offset.getX());
                    maxX = Math.max(maxX, offset.getX());
                    minY = Math.min(minY, offset.getY());
                    maxY = Math.max(maxY, offset.getY());
                    minZ = Math.min(minZ, offset.getZ());
                    maxZ = Math.max(maxZ, offset.getZ());
                }
            }
        }

        return new CanvasFootprint(minX, maxX, minY, maxY, minZ, maxZ);
    }

    public CanvasFootprint covering(CanvasFootprint other) {
        return new CanvasFootprint(
            Math.min(this.minX, other.minX),
            Math.max(this.maxX, other.maxX),
            Math.min(this.minY, other.minY),
            Math.max(this.maxY, other.maxY),
            Math.min(this.minZ, other.minZ),
            Math.max(this.maxZ, other.maxZ)
        );
    }
}
