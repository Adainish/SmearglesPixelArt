package com.adainish.smearglespixelart;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

public final class SpriteTemplateRecorder {
    private static final char[] PALETTE_KEYS = "123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

    private SpriteTemplateRecorder() {
    }

    public static CapturedSprite capture(ServerWorld world, String pokemon, BlockPos first, BlockPos second) {
        SpriteSelection selection = SpriteSelection.between(first, second);
        return new CapturedSprite(selection, record(pokemon, selection, pos -> blockId(world, pos)));
    }

    static RecordedTemplate record(String pokemon, SpriteSelection selection, BlockLookup lookup) {
        Map<String, String> palette = new LinkedHashMap<>();
        Map<String, Character> blockKeys = new LinkedHashMap<>();
        List<String> rows = new ArrayList<>();

        for (int y = selection.maxY(); y >= selection.minY(); y--) {
            StringBuilder row = new StringBuilder(selection.width());
            for (int horizontal = selection.minHorizontal(); horizontal <= selection.maxHorizontal(); horizontal++) {
                String blockId = lookup.blockId(selection.toWorldPos(horizontal, y));
                if (blockId == null || "minecraft:air".equals(blockId)) {
                    row.append('.');
                    continue;
                }

                Character key = blockKeys.get(blockId);
                if (key == null) {
                    int paletteIndex = blockKeys.size();
                    if (paletteIndex >= PALETTE_KEYS.length) {
                        throw new IllegalStateException("Sprite template recording supports up to " + PALETTE_KEYS.length + " unique block types.");
                    }

                    key = PALETTE_KEYS[paletteIndex];
                    blockKeys.put(blockId, key);
                    palette.put(String.valueOf(key), blockId);
                }

                row.append(key);
            }
            rows.add(row.toString());
        }

        return new RecordedTemplate(pokemon, Collections.unmodifiableMap(new LinkedHashMap<>(palette)), List.copyOf(rows));
    }

    private static @Nullable String blockId(ServerWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (state.isAir()) {
            return null;
        }
        return Registries.BLOCK.getId(state.getBlock()).toString();
    }

    @FunctionalInterface
    interface BlockLookup {
        @Nullable
        String blockId(BlockPos pos);
    }

    public record RecordedTemplate(String pokemon, Map<String, String> palette, List<String> rows) {
    }

    public record CapturedSprite(SpriteSelection selection, RecordedTemplate template) {
    }

    public record SpriteSelection(
        Direction.Axis horizontalAxis,
        int fixedAxisValue,
        int minHorizontal,
        int maxHorizontal,
        int minY,
        int maxY
    ) {
        public SpriteSelection {
            if (horizontalAxis != Direction.Axis.X && horizontalAxis != Direction.Axis.Z) {
                throw new IllegalArgumentException("2D sprite templates must use X or Z as the horizontal axis.");
            }
            if (maxHorizontal < minHorizontal || maxY < minY) {
                throw new IllegalArgumentException("Sprite selection bounds are invalid.");
            }
        }

        public static SpriteSelection between(BlockPos first, BlockPos second) {
            boolean sameX = first.getX() == second.getX();
            boolean sameZ = first.getZ() == second.getZ();

            if (sameX && sameZ) {
                throw new IllegalArgumentException("2D sprite selections must span more than one block horizontally.");
            }

            if (!sameX && !sameZ) {
                throw new IllegalArgumentException("2D sprite selections must keep either X or Z constant.");
            }

            if (sameZ) {
                return new SpriteSelection(
                    Direction.Axis.X,
                    first.getZ(),
                    Math.min(first.getX(), second.getX()),
                    Math.max(first.getX(), second.getX()),
                    Math.min(first.getY(), second.getY()),
                    Math.max(first.getY(), second.getY())
                );
            }

            return new SpriteSelection(
                Direction.Axis.Z,
                first.getX(),
                Math.min(first.getZ(), second.getZ()),
                Math.max(first.getZ(), second.getZ()),
                Math.min(first.getY(), second.getY()),
                Math.max(first.getY(), second.getY())
            );
        }

        public int width() {
            return maxHorizontal - minHorizontal + 1;
        }

        public int height() {
            return maxY - minY + 1;
        }

        public BlockPos toWorldPos(int horizontal, int y) {
            if (horizontalAxis == Direction.Axis.X) {
                return new BlockPos(horizontal, y, fixedAxisValue);
            }
            return new BlockPos(fixedAxisValue, y, horizontal);
        }
    }
}
