package com.adainish.smearglespixelart;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
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
    private static final String SMEARGLE_PROPERTIES = "species=smeargle level=50";

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
                    + "</yellow> <gray>at</gray> <gold>" + configuredCanvas.ticksPerPlacement() + "</gold> <gray>ticks per block.</gray>"
            );
        }

        int placed = activeRound.nextPlacementIndex;
        int total = activeRound.template.blocks().size();
        return MiniMessageText.deserialize(
            server,
            "<gold>Smeargle is painting right now.</gold> <gray>Revealed <yellow>" + placed + "</yellow>/<yellow>" + total + "</yellow> blocks.</gray>"
                + " <gray>Canvas:</gray> <aqua>" + MiniMessageText.escape(activeRound.dimensionId) + "</aqua> <yellow>"
                + activeRound.origin.getX() + " " + activeRound.origin.getY() + " " + activeRound.origin.getZ() + "</yellow>"
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
        finishRound(player.getServer(), false);
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

        if (activeRound.cooldownTicks > 0) {
            activeRound.cooldownTicks--;
            return;
        }

        activeRound.cooldownTicks = activeRound.ticksPerPlacement;

        if (activeRound.nextPlacementIndex >= activeRound.revealOrder.size()) {
            broadcast(
                server,
                "<yellow>Smeargle finished the entire painting.</yellow> <gray>The Pokémon was</gray> <gold>" + MiniMessageText.escape(activeRound.template.pokemon()) + "</gold><gray>.</gray>"
            );
            finishRound(server, false);
            return;
        }

        PixelArtTemplate.BlockPlacement placement = activeRound.revealOrder.get(activeRound.nextPlacementIndex);
        placeBlock(world, activeRound.origin.add(placement.x(), placement.y(), placement.z()), placement.blockId());
        activeRound.nextPlacementIndex++;
        moveSmeargle(world, activeRound.origin, placement);

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
        CanvasFootprint nextFootprint = CanvasFootprint.of(template);
        CanvasFootprint clearFootprint = Optional.ofNullable(canvasFootprints.get(canvasKey))
            .map(existing -> existing.covering(nextFootprint))
            .orElse(nextFootprint);

        clearCanvas(world, canvasOrigin, clearFootprint);
        canvasFootprints.put(canvasKey, nextFootprint);
        activeRound = new ActiveRound(world, configuredCanvas.dimensionId(), canvasOrigin, template, configuredCanvas.ticksPerPlacement());
        activeRound.artistEntityId = spawnSmeargle(world, canvasOrigin);

        broadcast(
            world.getServer(),
            "<aqua><bold>Smeargle has started a new painting!</bold></aqua> "
                + "<gray>Use</gray> <yellow>/guess &lt;pokemon&gt;</yellow> <gray>to answer first.</gray>"
        );
        broadcast(world.getServer(), PokemonHintFormatter.lengthHint(template));
        return StartResult.STARTED;
    }

    private void finishRound(MinecraftServer server, boolean silent) {
        despawnSmeargle(server);
        activeRound = null;
        if (!silent) {
            broadcast(server, "<gray>Use an admin start command to begin the next round.</gray>");
        }
    }

    private void clearCanvas(ServerWorld world, BlockPos origin, CanvasFootprint footprint) {
        for (int x = 0; x <= footprint.maxX(); x++) {
            for (int y = 0; y <= footprint.maxY(); y++) {
                for (int z = 0; z <= footprint.maxZ(); z++) {
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
    private UUID spawnSmeargle(ServerWorld world, BlockPos origin) {
        try {
            Entity entity = PokemonProperties.Companion.parse(SMEARGLE_PROPERTIES).createEntity(world);
            entity.setCustomName(Text.literal("Smeargle"));
            entity.setCustomNameVisible(true);
            entity.setInvulnerable(true);
            entity.refreshPositionAndAngles(origin.getX() + 0.5D, origin.getY(), origin.getZ() + 1.5D, 180.0F, 0.0F);
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

        entity.refreshPositionAndAngles(
            origin.getX() + placement.x() + 0.5D,
            origin.getY() + Math.max(placement.y() - 1, 0),
            origin.getZ() + 1.5D,
            180.0F,
            0.0F
        );
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

    @Nullable
    private ConfiguredCanvas configuredCanvas(MinecraftServer server) {
        SmearglesPixelArtConfig config = configSupplier.get();
        ServerWorld world = server.getWorld(config.dimensionKey());
        if (world == null) {
            return null;
        }
        return new ConfiguredCanvas(world, config.dimension(), config.canvasOrigin(), config.ticksPerPlacement());
    }

    private static final class ActiveRound {
        private final RegistryKey<World> worldKey;
        private final String dimensionId;
        private final BlockPos origin;
        private final PixelArtTemplate template;
        private final java.util.List<PixelArtTemplate.BlockPlacement> revealOrder;
        private final int ticksPerPlacement;
        private int nextPlacementIndex;
        private int cooldownTicks;
        private boolean firstLetterHintSent;
        private boolean silhouetteHintSent;
        @Nullable
        private UUID artistEntityId;

        private ActiveRound(ServerWorld world, String dimensionId, BlockPos origin, PixelArtTemplate template, int ticksPerPlacement) {
            this.worldKey = world.getRegistryKey();
            this.dimensionId = dimensionId;
            this.origin = origin;
            this.template = template;
            this.revealOrder = template.revealOrder();
            this.ticksPerPlacement = ticksPerPlacement;
            this.cooldownTicks = ticksPerPlacement;
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

    private record ConfiguredCanvas(ServerWorld world, String dimensionId, BlockPos origin, int ticksPerPlacement) {
    }

    private record CanvasKey(RegistryKey<World> worldKey, BlockPos origin) {
    }
}
