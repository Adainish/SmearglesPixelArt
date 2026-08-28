package com.adainish.smearglespixelart;

import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Set;

final class SmeargleMovementPlanner {
    private SmeargleMovementPlanner() {
    }

    static MovementFrame nextFrame(
        @Nullable BlockPos currentAnchor,
        int currentStandingY,
        Set<BlockPos> currentSupportBlocks,
        SmeargleSupportColumn targetColumn,
        int baseY
    ) {
        if (currentAnchor == null || !currentAnchor.equals(targetColumn.anchor())) {
            return new MovementFrame(
                targetColumn.anchor(),
                baseY,
                Set.of(),
                Set.copyOf(currentSupportBlocks),
                false
            );
        }

        if (currentStandingY < targetColumn.standingY()) {
            BlockPos nextSupport = new BlockPos(currentAnchor.getX(), currentStandingY, currentAnchor.getZ());
            return new MovementFrame(
                currentAnchor,
                currentStandingY + 1,
                Set.of(nextSupport),
                Set.of(),
                false
            );
        }

        if (currentStandingY > targetColumn.standingY()) {
            BlockPos removedSupport = new BlockPos(currentAnchor.getX(), currentStandingY - 1, currentAnchor.getZ());
            Set<BlockPos> supportToRemove = targetColumn.supportBlocks().contains(removedSupport) ? Set.of() : Set.of(removedSupport);
            return new MovementFrame(
                currentAnchor,
                currentStandingY - 1,
                Set.of(),
                supportToRemove,
                false
            );
        }

        Set<BlockPos> supportToRemove = new LinkedHashSet<>(currentSupportBlocks);
        supportToRemove.removeAll(targetColumn.supportBlocks());
        if (!supportToRemove.isEmpty()) {
            return new MovementFrame(currentAnchor, currentStandingY, Set.of(), Set.copyOf(supportToRemove), false);
        }

        Set<BlockPos> supportToAdd = new LinkedHashSet<>(targetColumn.supportBlocks());
        supportToAdd.removeAll(currentSupportBlocks);
        if (!supportToAdd.isEmpty()) {
            BlockPos nextSupport = supportToAdd.stream()
                .min(java.util.Comparator.comparingInt(BlockPos::getY))
                .orElseThrow();
            return new MovementFrame(currentAnchor, currentStandingY, Set.of(nextSupport), Set.of(), false);
        }

        return new MovementFrame(currentAnchor, currentStandingY, Set.of(), Set.of(), true);
    }

    record MovementFrame(
        BlockPos anchor,
        int standingY,
        Set<BlockPos> supportToAdd,
        Set<BlockPos> supportToRemove,
        boolean readyToPaint
    ) {
    }
}
