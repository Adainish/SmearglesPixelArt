package com.adainish.smearglespixelart;

import java.util.Comparator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public record PixelArtTemplate(
    String pokemon,
    String normalizedPokemon,
    int width,
    int height,
    List<BlockPlacement> blocks
) {
    public enum FillPattern {
        ROW_SWEEP,
        ZIG_ZAG_ROWS,
        COLUMN_SWEEP,
        CENTER_OUT,
        RANDOMIZED
    }

    public static FillPattern fillPatternForRound(int roundNumber, int totalRounds) {
        int safeRound = Math.max(1, roundNumber);
        int safeTotal = Math.max(1, totalRounds);
        int difficultyStep = safeTotal == 1
            ? 0
            : Math.min(4, ((safeRound - 1) * 4) / (safeTotal - 1));
        return switch (difficultyStep) {
            case 0 -> FillPattern.ROW_SWEEP;
            case 1 -> FillPattern.ZIG_ZAG_ROWS;
            case 2 -> FillPattern.COLUMN_SWEEP;
            case 3 -> FillPattern.CENTER_OUT;
            default -> FillPattern.RANDOMIZED;
        };
    }

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

    public List<BlockPlacement> revealOrder(FillPattern fillPattern, Random random) {
        Objects.requireNonNull(fillPattern, "fillPattern");
        Objects.requireNonNull(random, "random");
        return switch (fillPattern) {
            case ROW_SWEEP -> revealOrder();
            case ZIG_ZAG_ROWS -> blocks.stream()
                .sorted(
                    Comparator.comparingInt(BlockPlacement::y)
                        .thenComparingInt(block -> block.y() % 2 == 0 ? block.x() : (maxX() - block.x()))
                )
                .toList();
            case COLUMN_SWEEP -> blocks.stream()
                .sorted(Comparator.comparingInt(BlockPlacement::x).thenComparingInt(BlockPlacement::y))
                .toList();
            case CENTER_OUT -> {
                double centerX = (width - 1) / 2.0D;
                double centerY = (height - 1) / 2.0D;
                yield blocks.stream()
                    .sorted(
                        Comparator.comparingDouble(
                            (BlockPlacement block) -> Math.pow(block.x() - centerX, 2) + Math.pow(block.y() - centerY, 2)
                        )
                            .thenComparingInt(BlockPlacement::y)
                            .thenComparingInt(BlockPlacement::x)
                    )
                    .toList();
            }
            case RANDOMIZED -> {
                List<BlockPlacement> randomized = new ArrayList<>(revealOrder());
                Collections.shuffle(randomized, random);
                yield List.copyOf(randomized);
            }
        };
    }

    public record BlockPlacement(int x, int y, int z, String blockId) {
        public BlockPlacement {
            Objects.requireNonNull(blockId, "blockId");
        }
    }
}
