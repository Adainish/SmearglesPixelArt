package com.adainish.smearglespixelart;

import net.minecraft.util.math.BlockPos;

public enum CanvasDirection {
    NORTH("north", 0, -1, 180.0F),
    NORTHEAST("northeast", 1, -1, -135.0F),
    EAST("east", 1, 0, -90.0F),
    SOUTHEAST("southeast", 1, 1, -45.0F),
    SOUTH("south", 0, 1, 0.0F),
    SOUTHWEST("southwest", -1, 1, 45.0F),
    WEST("west", -1, 0, 90.0F),
    NORTHWEST("northwest", -1, -1, 135.0F);

    private final String id;
    private final int facingX;
    private final int facingZ;
    private final int rightX;
    private final int rightZ;
    private final float yaw;

    CanvasDirection(String id, int facingX, int facingZ, float yaw) {
        this.id = id;
        this.facingX = facingX;
        this.facingZ = facingZ;
        this.rightX = -facingZ;
        this.rightZ = facingX;
        this.yaw = yaw;
    }

    public static CanvasDirection parse(String input) {
        if (input == null || input.isBlank()) {
            return NORTH;
        }

        String normalized = GuessNormalizer.normalize(input);
        return switch (normalized) {
            case "north" -> NORTH;
            case "northeast", "ne" -> NORTHEAST;
            case "east" -> EAST;
            case "southeast", "se" -> SOUTHEAST;
            case "south" -> SOUTH;
            case "southwest", "sw" -> SOUTHWEST;
            case "west" -> WEST;
            case "northwest", "nw" -> NORTHWEST;
            default -> NORTH;
        };
    }

    public String id() {
        return this.id;
    }

    public float yaw() {
        return this.yaw;
    }

    public float audienceYaw() {
        return this.yaw >= 0.0F ? this.yaw - 180.0F : this.yaw + 180.0F;
    }

    public BlockPos worldOffset(PixelArtTemplate.BlockPlacement placement) {
        return new BlockPos(
            this.rightX * placement.x() + this.facingX * placement.z(),
            placement.y(),
            this.rightZ * placement.x() + this.facingZ * placement.z()
        );
    }

    public BlockPos transform(BlockPos origin, PixelArtTemplate.BlockPlacement placement) {
        return origin.add(worldOffset(placement));
    }

    public BlockPos supportAnchor(BlockPos blockPos) {
        return blockPos.add(this.facingX, 0, this.facingZ);
    }

    public double artistX(BlockPos pos) {
        return pos.getX() + 0.5D;
    }

    public double artistY(int standingY) {
        return standingY;
    }

    public double artistZ(BlockPos pos) {
        return pos.getZ() + 0.5D;
    }

    public double audienceX(BlockPos pos, double distance) {
        return artistX(pos) + normalizedFacingX() * distance;
    }

    public double audienceZ(BlockPos pos, double distance) {
        return artistZ(pos) + normalizedFacingZ() * distance;
    }

    private double normalizedFacingX() {
        return this.facingX / facingMagnitude();
    }

    private double normalizedFacingZ() {
        return this.facingZ / facingMagnitude();
    }

    private double facingMagnitude() {
        return Math.sqrt((this.facingX * this.facingX) + (this.facingZ * this.facingZ));
    }
}
