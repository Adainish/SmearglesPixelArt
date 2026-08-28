package com.adainish.smearglespixelart;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.registry.Registries;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public final class SmearglesPixelArtManager {
    private static final int TICKS_PER_PLACEMENT = 10;
    private static final String SMEARGLE_PROPERTIES = "species=smeargle level=50";

    private final PixelArtTemplateRegistry templates;
    private final Path templateDirectory;
    private final Random random = new Random();
    private final Map<CanvasKey, CanvasFootprint> canvasFootprints = new HashMap<>();
    @Nullable
    private ActiveRound activeRound;

    public SmearglesPixelArtManager(PixelArtTemplateRegistry templates, Path templateDirectory) {
        this.templates = templates;
        this.templateDirectory = templateDirectory;
    }

    public Iterable<String> templateNames() {
        return templates.templateNames();
    }

    public boolean hasActiveRound() {
        return activeRound != null;
    }

    public Text describeStatus(MinecraftServer server) {
        if (activeRound == null) {
            return MiniMessageText.deserialize(server, "<gray>No active Smeargle round.</gray>");
        }

        int placed = activeRound.nextPlacementIndex;
        int total = activeRound.template.blocks().size();
        return MiniMessageText.deserialize(
            server,
            "<gold>Smeargle is painting right now.</gold> <gray>Revealed <yellow>" + placed + "</yellow>/<yellow>" + total + "</yellow> blocks.</gray>"
        );
    }

    public boolean startRandom(ServerWorld world, BlockPos origin) {
        return start(world, origin, templates.randomTemplate(random));
    }

    public boolean startTemplate(ServerWorld world, BlockPos origin, String templateName) {
        Optional<PixelArtTemplate> template = templates.find(templateName);
        return template.filter(value -> start(world, origin, value)).isPresent();
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

        activeRound.cooldownTicks = TICKS_PER_PLACEMENT;

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

        if (!activeRound.firstLetterHintSent && activeRound.nextPlacementIndex * 3 >= total) {
            activeRound.firstLetterHintSent = true;
            broadcast(server, PokemonHintFormatter.firstLetterHint(activeRound.template));
        }
        if (!activeRound.silhouetteHintSent && activeRound.nextPlacementIndex * 3 >= total * 2) {
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

    private boolean start(ServerWorld world, BlockPos origin, PixelArtTemplate template) {
        if (activeRound != null) {
            return false;
        }

        BlockPos canvasOrigin = origin.toImmutable();
        CanvasKey canvasKey = new CanvasKey(world.getRegistryKey(), canvasOrigin);
        CanvasFootprint nextFootprint = CanvasFootprint.of(template);
        CanvasFootprint clearFootprint = Optional.ofNullable(canvasFootprints.get(canvasKey))
            .map(existing -> existing.covering(nextFootprint))
            .orElse(nextFootprint);

        clearCanvas(world, canvasOrigin, clearFootprint);
        canvasFootprints.put(canvasKey, nextFootprint);
        activeRound = new ActiveRound(world, canvasOrigin, template);
        activeRound.artistEntityId = spawnSmeargle(world, origin);

        broadcast(
            world.getServer(),
            "<aqua><bold>Smeargle has started a new painting!</bold></aqua> "
                + "<gray>Use</gray> <yellow>/guess &lt;pokemon&gt;</yellow> <gray>to answer first.</gray>"
        );
        broadcast(world.getServer(), PokemonHintFormatter.lengthHint(template));
        return true;
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

    private static final class ActiveRound {
        private final RegistryKey<World> worldKey;
        private final BlockPos origin;
        private final PixelArtTemplate template;
        private final java.util.List<PixelArtTemplate.BlockPlacement> revealOrder;
        private int nextPlacementIndex;
        private int cooldownTicks;
        private boolean firstLetterHintSent;
        private boolean silhouetteHintSent;
        @Nullable
        private UUID artistEntityId;

        private ActiveRound(ServerWorld world, BlockPos origin, PixelArtTemplate template) {
            this.worldKey = world.getRegistryKey();
            this.origin = origin;
            this.template = template;
            this.revealOrder = template.revealOrder();
            this.cooldownTicks = TICKS_PER_PLACEMENT;
        }
    }

    public record RecordedTemplate(String templateName, String pokemon, int width, int height, Path path) {
    }

    private record CanvasKey(RegistryKey<World> worldKey, BlockPos origin) {
    }
}
