package com.adainish.smearglespixelart;

import net.minecraft.util.math.BlockPos;

import java.util.LinkedHashSet;
import java.util.Set;

final class SmeargleSupportColumn {
    private final BlockPos anchor;
    private final int standingY;
    private final Set<BlockPos> supportBlocks;

    private SmeargleSupportColumn(BlockPos anchor, int standingY, Set<BlockPos> supportBlocks) {
        this.anchor = anchor;
        this.standingY = standingY;
        this.supportBlocks = Set.copyOf(supportBlocks);
    }

    static SmeargleSupportColumn forPlacement(CanvasDirection direction, BlockPos origin, PixelArtTemplate.BlockPlacement placement) {
        BlockPos targetBlock = direction.transform(origin, placement);
        BlockPos anchor = direction.supportAnchor(targetBlock);
        return forAnchor(anchor, targetBlock.getY(), origin.getY());
    }

    static SmeargleSupportColumn forAnchor(BlockPos anchor, int standingY, int baseY) {
        Set<BlockPos> supportBlocks = new LinkedHashSet<>();
        for (int y = baseY; y < standingY; y++) {
            supportBlocks.add(new BlockPos(anchor.getX(), y, anchor.getZ()));
        }
        return new SmeargleSupportColumn(anchor, standingY, supportBlocks);
    }

    BlockPos anchor() {
        return anchor;
    }

    int standingY() {
        return standingY;
    }

    Set<BlockPos> supportBlocks() {
        return supportBlocks;
    }
}
