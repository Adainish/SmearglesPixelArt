package com.adainish.smearglespixelart;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import java.io.IOException;
import java.util.HashSet;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public final class SmearglesPixelArtManager {
    static final int CLEANUP_DELAY_TICKS = 20 * 5;
    private static final String SMEARGLE_PROPERTIES = "species=smeargle level=50";
    private static final BlockState SMEARGLE_SUPPORT_BLOCK = Blocks.SCAFFOLDING.getDefaultState();

    private final PixelArtTemplateRegistry templates;
    private final Path templateDirectory;
    private final Supplier<SmearglesPixelArtConfig> configSupplier;
    private final Random random = new Random();
    private final Map<CanvasKey, CanvasFootprint> canvasFootprints = new HashMap<>();
    @Nullable
    private ActiveRound activeRound;

    public SmearglesPixelArtManager(
        PixelArtTemplateRegistry templates,
        Path templateDirectory,
        Supplier<SmearglesPixelArtConfig> configSupplier
    ) {
        this.templates = templates;
        this.templateDirectory = templateDirectory;
        this.configSupplier = configSupplier;
    }

    public Iterable<String> templateNames() {
        return templates.templateNames();
    }

    public boolean hasActiveRound() {
        return activeRound != null;
    }

    public Text describeStatus(MinecraftServer server) {
        ConfiguredCanvas configuredCanvas = configuredCanvas(server);
        if (activeRound == null) {
            if (configuredCanvas == null) {
                return MiniMessageText.deserialize(
                    server,
                    "<gray>No active Smeargle round.</gray> <red>The configured canvas dimension is unavailable.</red>"
                );
            }
            return MiniMessageText.deserialize(
                server,
                "<gray>No active Smeargle round.</gray> <gray>Configured canvas:</gray> <aqua>"
                    + MiniMessageText.escape(configuredCanvas.dimensionId()) + "</aqua> <yellow>"
                    + configuredCanvas.origin().getX() + " " + configuredCanvas.origin().getY() + " " + configuredCanvas.origin().getZ()
                    + "</yellow> <gray>facing</gray> <gold>" + MiniMessageText.escape(configuredCanvas.direction().id()) + "</gold>"
                    + " <gray>at</gray> <gold>" + configuredCanvas.ticksPerPlacement() + "</gold> <gray>ticks per block.</gray>"
            );
        }

        int placed = activeRound.nextPlacementIndex;
        int total = activeRound.template.blocks().size();
        String phaseDetails = switch (activeRound.phase) {
            case PAINTING -> "<gold>Smeargle is painting right now.</gold> <gray>Revealed <yellow>" + placed + "</yellow>/<yellow>" + total + "</yellow> blocks.</gray>";
            case WAITING_TO_CLEAR -> "<gold>Smeargle finished the round.</gold> <gray>Cleanup starts in <yellow>" + activeRound.cleanupWaitTicksRemaining + "</yellow> ticks.</gray>";
            case CLEARING -> "<gold>Smeargle is clearing the canvas.</gold> <gray>Cleared <yellow>" + activeRound.nextCleanupIndex + "</yellow>/<yellow>" + total + "</yellow> blocks.</gray>";
        };
        return MiniMessageText.deserialize(
            server,
            phaseDetails
                + " <gray>Canvas:</gray> <aqua>" + MiniMessageText.escape(activeRound.dimensionId) + "</aqua> <yellow>"
                + activeRound.origin.getX() + " " + activeRound.origin.getY() + " " + activeRound.origin.getZ() + "</yellow>"
                + " <gray>facing</gray> <gold>" + MiniMessageText.escape(activeRound.direction.id()) + "</gold>"
                + " <gray>at</gray> <gold>" + activeRound.ticksPerPlacement + "</gold> <gray>ticks per block.</gray>"
        );
    }

    public StartResult startRandom(MinecraftServer server) {
        ConfiguredCanvas configuredCanvas = configuredCanvas(server);
        if (configuredCanvas == null) {
            return StartResult.CONFIGURED_DIMENSION_UNAVAILABLE;
        }
        return start(configuredCanvas, templates.randomTemplate(random));
    }

    public StartResult startTemplate(MinecraftServer server, String templateName) {
        ConfiguredCanvas configuredCanvas = configuredCanvas(server);
        if (configuredCanvas == null) {
            return StartResult.CONFIGURED_DIMENSION_UNAVAILABLE;
        }

        Optional<PixelArtTemplate> template = templates.find(templateName);
        if (template.isEmpty()) {
            return StartResult.TEMPLATE_NOT_FOUND;
        }
        return start(configuredCanvas, template.orElseThrow());
    }

    public RecordedTemplate recordTemplate(ServerWorld world, String templateName, String pokemon, BlockPos first, BlockPos second) throws IOException {
        SpriteTemplateRecorder.CapturedSprite captured = SpriteTemplateRecorder.capture(world, pokemon, first, second);
        Path outputPath = templates.saveCustomTemplate(templateDirectory, templateName, captured.template());
        return new RecordedTemplate(
            GuessNormalizer.normalize(templateName),
            captured.template().pokemon(),
            captured.selection().width(),
            captured.selection().height(),
            outputPath
        );
    }

    public boolean guess(ServerPlayerEntity player, String guess) {
        if (activeRound == null) {
            player.sendMessage(MiniMessageText.deserialize(player.getServer(), "<gray>There is no active pixel-art round to guess right now.</gray>"), false);
            return false;
        }

        String normalizedGuess = GuessNormalizer.normalize(guess);
        if (normalizedGuess.isEmpty()) {
            player.sendMessage(MiniMessageText.deserialize(player.getServer(), "<red>Your guess needs a Pokémon name.</red>"), false);
            return false;
        }

        if (!normalizedGuess.equals(activeRound.template.normalizedPokemon())) {
            player.sendMessage(
                MiniMessageText.deserialize(
                    player.getServer(),
                    "<red>\"" + MiniMessageText.escape(guess) + "\"</red> <gray>is not the right Pokémon. Keep guessing!</gray>"
                ),
                false
            );
            return false;
        }

        broadcast(
            player.getServer(),
            "<green><bold>" + MiniMessageText.escape(player.getName().getString()) + "</bold></green> "
                + "<gray>guessed</gray> <gold>" + MiniMessageText.escape(activeRound.template.pokemon()) + "</gold> <gray>correctly and wins the round!</gray>"
        );
        beginCleanup(player.getServer());
        return true;
    }

    public void tick(MinecraftServer server) {
        if (activeRound == null) {
            return;
        }

        ServerWorld world = server.getWorld(activeRound.worldKey);
        if (world == null) {
            stop(server, "<red>The active painting world is no longer available. Ending the round.</red>");
            return;
        }

        if (activeRound.phase == RoundPhase.WAITING_TO_CLEAR) {
            if (activeRound.cleanupWaitTicksRemaining > 0) {
                activeRound.cleanupWaitTicksRemaining--;
                return;
            }
            activeRound.phase = RoundPhase.CLEARING;
            activeRound.cooldownTicks = 0;
            broadcast(server, "<gray>Smeargle is starting to clean up the canvas.</gray>");
        }

        if (activeRound.cooldownTicks > 0) {
            activeRound.cooldownTicks--;
            return;
        }

        if (activeRound.phase == RoundPhase.CLEARING) {
            if (activeRound.nextCleanupIndex >= activeRound.cleanupOrder.size()) {
                finishRound(server, false);
                return;
            }

            PixelArtTemplate.BlockPlacement placement = activeRound.cleanupOrder.get(activeRound.nextCleanupIndex);
            placeBlock(world, activeRound.direction.transform(activeRound.origin, placement), "minecraft:air");
            activeRound.nextCleanupIndex++;
            moveSmeargle(world, activeRound.origin, placement);
            activeRound.cooldownTicks = activeRound.ticksPerPlacement;
            return;
        }

        if (activeRound.nextPlacementIndex >= activeRound.revealOrder.size()) {
            broadcast(
                server,
                "<yellow>Smeargle finished the entire painting.</yellow> <gray>The Pokémon was</gray> <gold>" + MiniMessageText.escape(activeRound.template.pokemon()) + "</gold><gray>.</gray>"
            );
            beginCleanup(server);
            return;
        }

        PixelArtTemplate.BlockPlacement placement = activeRound.revealOrder.get(activeRound.nextPlacementIndex);
        placeBlock(world, activeRound.direction.transform(activeRound.origin, placement), placement.blockId());
        activeRound.nextPlacementIndex++;
        moveSmeargle(world, activeRound.origin, placement);
        activeRound.cooldownTicks = activeRound.ticksPerPlacement;

        if (activeRound.nextPlacementIndex % 8 == 0 || activeRound.nextPlacementIndex == activeRound.revealOrder.size()) {
            broadcast(
                server,
                "<gray>Smeargle has revealed <yellow>" + activeRound.nextPlacementIndex + "</yellow>/<yellow>"
                    + activeRound.revealOrder.size() + "</yellow> blocks.</gray>"
            );
        }

        int total = activeRound.revealOrder.size();
        if (total == 0) {
            return;
        }

        long revealed = activeRound.nextPlacementIndex;
        long totalBlocks = total;
        if (!activeRound.firstLetterHintSent && revealed * 3L >= totalBlocks) {
            activeRound.firstLetterHintSent = true;
            broadcast(server, PokemonHintFormatter.firstLetterHint(activeRound.template));
        }
        if (!activeRound.silhouetteHintSent && revealed * 3L >= totalBlocks * 2L) {
            activeRound.silhouetteHintSent = true;
            broadcast(server, PokemonHintFormatter.silhouetteHint(activeRound.template));
        }
    }

    public void stop(MinecraftServer server, String message) {
        if (activeRound == null) {
            return;
        }

        clearActiveCanvas(server);
        clearTemporarySupport(server);
        despawnSmeargle(server);
        activeRound = null;
        broadcast(server, message);
    }

    private StartResult start(ConfiguredCanvas configuredCanvas, PixelArtTemplate template) {
        if (activeRound != null) {
            return StartResult.ROUND_ALREADY_ACTIVE;
        }

        ServerWorld world = configuredCanvas.world();
        BlockPos canvasOrigin = configuredCanvas.origin().toImmutable();
        CanvasKey canvasKey = new CanvasKey(world.getRegistryKey(), canvasOrigin);
        CanvasFootprint nextFootprint = CanvasFootprint.of(template, configuredCanvas.direction());
        CanvasFootprint clearFootprint = Optional.ofNullable(canvasFootprints.get(canvasKey))
            .map(existing -> existing.covering(nextFootprint))
            .orElse(nextFootprint);

        clearCanvas(world, canvasOrigin, clearFootprint);
        canvasFootprints.put(canvasKey, nextFootprint);
        activeRound = new ActiveRound(world, configuredCanvas.dimensionId(), configuredCanvas.direction(), canvasOrigin, template, configuredCanvas.ticksPerPlacement());
        activeRound.artistEntityId = spawnSmeargle(world, configuredCanvas.direction(), canvasOrigin);

        broadcast(
            world.getServer(),
            "<aqua><bold>Smeargle has started a new painting!</bold></aqua> "
                + "<gray>Use</gray> <yellow>/guess &lt;pokemon&gt;</yellow> <gray>to answer first.</gray>"
        );
        broadcast(world.getServer(), PokemonHintFormatter.lengthHint(template));
        return StartResult.STARTED;
    }

    private void finishRound(MinecraftServer server, boolean silent) {
        if (activeRound != null && activeRound.phase != RoundPhase.CLEARING) {
            clearActiveCanvas(server);
        }
        clearTemporarySupport(server);
        despawnSmeargle(server);
        activeRound = null;
        if (!silent) {
            broadcast(server, "<gray>Smeargle cleaned up the canvas. Use an admin start command to begin the next round.</gray>");
        }
    }

    private void beginCleanup(MinecraftServer server) {
        if (activeRound == null || activeRound.phase != RoundPhase.PAINTING) {
            return;
        }
        activeRound.phase = RoundPhase.WAITING_TO_CLEAR;
        activeRound.cleanupWaitTicksRemaining = CLEANUP_DELAY_TICKS;
        activeRound.cooldownTicks = 0;
        broadcast(server, "<gray>Smeargle will start cleaning the canvas in 5 seconds.</gray>");
    }

    private void clearCanvas(ServerWorld world, BlockPos origin, CanvasFootprint footprint) {
        for (int x = footprint.minX(); x <= footprint.maxX(); x++) {
            for (int y = footprint.minY(); y <= footprint.maxY(); y++) {
                for (int z = footprint.minZ(); z <= footprint.maxZ(); z++) {
                    world.setBlockState(origin.add(x, y, z), Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
                }
            }
        }
    }

    private void placeBlock(ServerWorld world, BlockPos pos, String blockId) {
        BlockState state = resolveBlockState(blockId);
        world.setBlockState(pos, state, Block.NOTIFY_ALL);
    }

    private BlockState resolveBlockState(String blockId) {
        Identifier identifier = Identifier.tryParse(blockId);
        if (identifier == null) {
            return Blocks.MAGENTA_GLAZED_TERRACOTTA.getDefaultState();
        }

        Block block = Registries.BLOCK.get(identifier);
        if (block == Blocks.AIR && !blockId.equals("minecraft:air")) {
            return Blocks.MAGENTA_GLAZED_TERRACOTTA.getDefaultState();
        }

        return block.getDefaultState();
    }

    @Nullable
    private UUID spawnSmeargle(ServerWorld world, CanvasDirection direction, BlockPos origin) {
        try {
            Entity entity = PokemonProperties.Companion.parse(SMEARGLE_PROPERTIES).createEntity(world);
            entity.setCustomName(Text.literal("Smeargle"));
            entity.setCustomNameVisible(true);
            entity.setInvulnerable(true);
            BlockPos supportAnchor = direction.supportAnchor(origin);
            entity.refreshPositionAndAngles(
                direction.artistX(supportAnchor),
                direction.artistY(origin.getY()),
                direction.artistZ(supportAnchor),
                direction.yaw(),
                0.0F
            );
            if (world.spawnEntity(entity)) {
                return entity.getUuid();
            }
        } catch (Exception exception) {
            SmearglesPixelArtMod.LOGGER.warn("Unable to spawn Smeargle artist", exception);
        }
        return null;
    }

    private void moveSmeargle(ServerWorld world, BlockPos origin, PixelArtTemplate.BlockPlacement placement) {
        if (activeRound == null || activeRound.artistEntityId == null) {
            return;
        }

        Entity entity = world.getEntity(activeRound.artistEntityId);
        if (entity == null) {
            return;
        }

        SmeargleSupportColumn supportColumn = SmeargleSupportColumn.forPlacement(activeRound.direction, origin, placement);
        syncTemporarySupport(world, supportColumn);
        entity.refreshPositionAndAngles(
            activeRound.direction.artistX(supportColumn.anchor()),
            activeRound.direction.artistY(supportColumn.standingY()),
            activeRound.direction.artistZ(supportColumn.anchor()),
            activeRound.direction.yaw(),
            0.0F
        );
    }

    private void syncTemporarySupport(ServerWorld world, SmeargleSupportColumn supportColumn) {
        if (activeRound == null) {
            return;
        }

        Set<BlockPos> desiredSupport = supportColumn.supportBlocks();
        Set<BlockPos> staleSupport = new HashSet<>(activeRound.temporarySupportBlocks);
        staleSupport.removeAll(desiredSupport);
        for (BlockPos pos : staleSupport) {
            world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
            activeRound.temporarySupportBlocks.remove(pos);
        }

        for (BlockPos pos : desiredSupport) {
            if (activeRound.temporarySupportBlocks.contains(pos)) {
                continue;
            }
            if (world.getBlockState(pos).isAir()) {
                world.setBlockState(pos, SMEARGLE_SUPPORT_BLOCK, Block.NOTIFY_ALL);
                activeRound.temporarySupportBlocks.add(pos.toImmutable());
            }
        }
    }

    private void clearTemporarySupport(MinecraftServer server) {
        if (activeRound == null || activeRound.temporarySupportBlocks.isEmpty()) {
            return;
        }

        ServerWorld world = server.getWorld(activeRound.worldKey);
        if (world == null) {
            return;
        }

        for (BlockPos pos : activeRound.temporarySupportBlocks) {
            world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
        }
        activeRound.temporarySupportBlocks.clear();
    }

    private void despawnSmeargle(MinecraftServer server) {
        if (activeRound == null || activeRound.artistEntityId == null) {
            return;
        }

        ServerWorld world = server.getWorld(activeRound.worldKey);
        if (world == null) {
            return;
        }

        Entity entity = world.getEntity(activeRound.artistEntityId);
        if (entity != null) {
            entity.discard();
        }
    }

    private void broadcast(MinecraftServer server, String message) {
        server.getPlayerManager().broadcast(MiniMessageText.deserialize(server, message), false);
    }

    private void clearActiveCanvas(MinecraftServer server) {
        if (activeRound == null) {
            return;
        }

        ServerWorld world = server.getWorld(activeRound.worldKey);
        if (world == null) {
            return;
        }

        clearCanvas(world, activeRound.origin, CanvasFootprint.of(activeRound.template, activeRound.direction));
    }

    @Nullable
    private ConfiguredCanvas configuredCanvas(MinecraftServer server) {
        SmearglesPixelArtConfig config = configSupplier.get();
        ServerWorld world = server.getWorld(config.dimensionKey());
        if (world == null) {
            return null;
        }
        return new ConfiguredCanvas(world, config.dimension(), config.direction(), config.canvasOrigin(), config.ticksPerPlacement());
    }

    private static final class ActiveRound {
        private final RegistryKey<World> worldKey;
        private final String dimensionId;
        private final CanvasDirection direction;
        private final BlockPos origin;
        private final PixelArtTemplate template;
        private final java.util.List<PixelArtTemplate.BlockPlacement> revealOrder;
        private final java.util.List<PixelArtTemplate.BlockPlacement> cleanupOrder;
        private final int ticksPerPlacement;
        private RoundPhase phase;
        private int nextPlacementIndex;
        private int nextCleanupIndex;
        private int cooldownTicks;
        private int cleanupWaitTicksRemaining;
        private boolean firstLetterHintSent;
        private boolean silhouetteHintSent;
        @Nullable
        private UUID artistEntityId;
        private final Set<BlockPos> temporarySupportBlocks = new HashSet<>();

        private ActiveRound(ServerWorld world, String dimensionId, CanvasDirection direction, BlockPos origin, PixelArtTemplate template, int ticksPerPlacement) {
            this.worldKey = world.getRegistryKey();
            this.dimensionId = dimensionId;
            this.direction = direction;
            this.origin = origin;
            this.template = template;
            this.revealOrder = template.revealOrder();
            this.cleanupOrder = this.revealOrder.reversed();
            this.ticksPerPlacement = ticksPerPlacement;
            this.cooldownTicks = ticksPerPlacement;
            this.phase = RoundPhase.PAINTING;
        }
    }

    public record RecordedTemplate(String templateName, String pokemon, int width, int height, Path path) {
    }

    public enum StartResult {
        STARTED,
        ROUND_ALREADY_ACTIVE,
        TEMPLATE_NOT_FOUND,
        CONFIGURED_DIMENSION_UNAVAILABLE
    }

    private record ConfiguredCanvas(ServerWorld world, String dimensionId, CanvasDirection direction, BlockPos origin, int ticksPerPlacement) {
    }

    private record CanvasKey(RegistryKey<World> worldKey, BlockPos origin) {
    }

    private enum RoundPhase {
        PAINTING,
        WAITING_TO_CLEAR,
        CLEARING
    }
}
