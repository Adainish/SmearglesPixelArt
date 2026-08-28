package com.adainish.smearglespixelart;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public record PixelArtTemplate(
    String pokemon,
    String normalizedPokemon,
    int width,
    int height,
    List<BlockPlacement> blocks
) {
    public PixelArtTemplate {
        Objects.requireNonNull(pokemon, "pokemon");
        Objects.requireNonNull(normalizedPokemon, "normalizedPokemon");
        Objects.requireNonNull(blocks, "blocks");
        blocks = List.copyOf(blocks);
    }

    public int minX() {
        return 0;
    }

    public int minY() {
        return 0;
    }

    public int minZ() {
        return 0;
    }

    public int maxX() {
        return Math.max(0, width - 1);
    }

    public int maxY() {
        return Math.max(0, height - 1);
    }

    public int maxZ() {
        return 0;
    }

    public List<BlockPlacement> revealOrder() {
        return blocks.stream()
            .sorted(Comparator.comparingInt(BlockPlacement::y).thenComparingInt(BlockPlacement::x))
            .toList();
    }

    public record BlockPlacement(int x, int y, int z, String blockId) {
        public BlockPlacement {
            Objects.requireNonNull(blockId, "blockId");
        }
    }
}
