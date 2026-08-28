package com.adainish.smearglespixelart;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public final class SmearglesPixelArtManager {
    static final int CLEANUP_DELAY_TICKS = 20 * 5;
    static final int REGISTRATION_DURATION_TICKS = 20 * 60 * 10;
    private static final String SMEARGLE_PROPERTIES = "species=smeargle level=50";
    private static final BlockState SMEARGLE_SUPPORT_BLOCK = Blocks.SCAFFOLDING.getDefaultState();

    private final PixelArtTemplateRegistry templates;
    private final Path templateDirectory;
    private final Supplier<SmearglesPixelArtConfig> configSupplier;
    private final Random random = new Random();
    private final Map<CanvasKey, CanvasFootprint> canvasFootprints = new HashMap<>();
    @Nullable
    private ActiveRound activeRound;
    @Nullable
    private ActiveSession activeSession;
    private Set<UUID> currentSessionParticipants = new HashSet<>();

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
        return activeRound != null || activeSession != null;
    }

    public Text describeStatus(MinecraftServer server) {
        ConfiguredCanvas configuredCanvas = configuredCanvas(server);
        if (activeRound == null && activeSession != null && activeSession.registrationOpen()) {
            int registrationSeconds = activeSession.registrationTicksRemaining() / 20;
            return MiniMessageText.deserialize(
                server,
                "<gold>Registration is open.</gold> <gray>Time remaining:</gray> <yellow>" + registrationSeconds + "</yellow> <gray>seconds.</gray> "
                    + "<gray>Registered players:</gray> <yellow>" + activeSession.registeredPlayerCount() + "</yellow>. "
                    + "<gray>Use</gray> <click:run_command:'/smearglesjoin'><yellow>/smearglesjoin</yellow></click> <gray>to join.</gray>"
            );
        }

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
            case ANGER_REACTION -> "<red>Smeargle is glaring at the players.</red> <gray>Anger:</gray> <gold>" + SmeargleAngerMeter.describe(activeRound.angerStage) + "</gold>.";
            case WAITING_TO_CLEAR -> "<gold>Smeargle finished the round.</gold> <gray>Cleanup starts in <yellow>" + activeRound.cleanupWaitTicksRemaining + "</yellow> ticks.</gray>";
            case CLEARING -> "<gold>Smeargle is clearing the canvas.</gold> <gray>Cleared <yellow>" + activeRound.nextCleanupIndex + "</yellow>/<yellow>" + total + "</yellow> blocks.</gray>";
        };
        return MiniMessageText.deserialize(
            server,
            phaseDetails
                + " <gray>Round:</gray> <yellow>" + activeRound.roundNumber + "</yellow>/<yellow>" + activeRound.totalRounds + "</yellow>."
                + " <gray>Canvas:</gray> <aqua>" + MiniMessageText.escape(activeRound.dimensionId) + "</aqua> <yellow>"
                + activeRound.origin.getX() + " " + activeRound.origin.getY() + " " + activeRound.origin.getZ() + "</yellow>"
                + " <gray>facing</gray> <gold>" + MiniMessageText.escape(activeRound.direction.id()) + "</gold>"
                + " <gray>at</gray> <gold>" + activeRound.ticksPerPlacement + "</gold> <gray>ticks per block.</gray>"
                + " <gray>Anger:</gray> <gold>" + SmeargleAngerMeter.describe(activeRound.angerStage) + "</gold>."
        );
    }

    public StartResult startRandom(MinecraftServer server) {
        return startRandom(server, 1);
    }

    public StartResult startRandom(MinecraftServer server, int rounds) {
        if (rounds < 1) {
            return StartResult.INVALID_ROUND_COUNT;
        }
        if (activeRound != null || activeSession != null) {
            return StartResult.ROUND_ALREADY_ACTIVE;
        }
        ConfiguredCanvas configuredCanvas = configuredCanvas(server);
        if (configuredCanvas == null) {
            return StartResult.CONFIGURED_DIMENSION_UNAVAILABLE;
        }

        activeSession = ActiveSession.random(rounds);
        broadcast(
            configuredCanvas.world().getServer(),
            "<aqua><bold>Smeargle session queued!</bold></aqua> <gray>Registration is open for</gray> <yellow>10 minutes</yellow><gray>.</gray> "
                + "<gray>Use</gray> <click:run_command:'/smearglesjoin'><yellow>/smearglesjoin</yellow></click> <gray>to participate.</gray>"
        );
        return StartResult.STARTED;
    }

    public StartResult startTemplate(MinecraftServer server, String templateName) {
        return startTemplate(server, templateName, 1);
    }

    public StartResult startTemplate(MinecraftServer server, String templateName, int rounds) {
        if (rounds < 1) {
            return StartResult.INVALID_ROUND_COUNT;
        }
        if (activeRound != null || activeSession != null) {
            return StartResult.ROUND_ALREADY_ACTIVE;
        }
        ConfiguredCanvas configuredCanvas = configuredCanvas(server);
        if (configuredCanvas == null) {
            return StartResult.CONFIGURED_DIMENSION_UNAVAILABLE;
        }

        Optional<PixelArtTemplate> template = templates.find(templateName);
        if (template.isEmpty()) {
            return StartResult.TEMPLATE_NOT_FOUND;
        }

        activeSession = ActiveSession.template(rounds, template.orElseThrow());
        broadcast(
            configuredCanvas.world().getServer(),
            "<aqua><bold>Smeargle session queued!</bold></aqua> <gray>Registration is open for</gray> <yellow>10 minutes</yellow><gray>.</gray> "
                + "<gray>Use</gray> <click:run_command:'/smearglesjoin'><yellow>/smearglesjoin</yellow></click> <gray>to participate.</gray>"
        );
        return StartResult.STARTED;
    }

    public JoinResult joinRegistration(ServerPlayerEntity player) {
        if (activeSession == null) {
            return JoinResult.NO_REGISTRATION_ACTIVE;
        }
        if (!activeSession.registrationOpen()) {
            return JoinResult.REGISTRATION_CLOSED;
        }
        return activeSession.register(player) ? JoinResult.JOINED : JoinResult.ALREADY_JOINED;
    }

    public boolean shouldBlockChat(UUID playerId, boolean hasBypassPermission) {
        return SmeargleChatGate.shouldBlock(activeRound != null, hasBypassPermission, activeSession == null ? Set.of() : activeSession.registeredPlayerIds, playerId);
    }

    public ForceStartResult forceStartRegistration(MinecraftServer server) {
        if (activeSession == null || !activeSession.registrationOpen()) {
            return ForceStartResult.NO_REGISTRATION_ACTIVE;
        }
        if (!activeSession.hasRegisteredPlayers()) {
            return ForceStartResult.NO_REGISTERED_PLAYERS;
        }
        return startRegisteredSession(server, true) ? ForceStartResult.STARTED : ForceStartResult.CONFIGURED_DIMENSION_UNAVAILABLE;
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
        if (activeSession != null && !activeSession.isRegistered(player.getUuid())) {
            player.sendMessage(MiniMessageText.deserialize(player.getServer(), "<red>You must register first with</red> <yellow>/smearglesjoin</yellow><red>.</red>"), false);
            return false;
        }
        if (activeRound.phase == RoundPhase.WAITING_TO_CLEAR || activeRound.phase == RoundPhase.CLEARING) {
            player.sendMessage(MiniMessageText.deserialize(player.getServer(), "<gray>That round has already ended. Wait for the next one to begin.</gray>"), false);
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

        UUID playerId = player.getUuid();
        if (activeRound.correctGuessers.contains(playerId)) {
            player.sendMessage(MiniMessageText.deserialize(player.getServer(), "<gray>You already got this round correct.</gray>"), false);
            return false;
        }

        int points = RoundScoring.pointsForCorrectGuessOrder(activeRound.correctGuessers.size());
        activeRound.correctGuessers.add(playerId);
        if (activeSession != null) {
            activeSession.award(player, points);
        }
        int totalPoints = activeSession == null ? points : activeSession.pointsFor(playerId);
        broadcastToParticipants(player.getServer(), GuessAnnouncementFormatter.correctGuessAnnouncement(player.getName().getString(), points, totalPoints));
        playRoundSound(player.getServer(), activeRound.origin, SmeargleMinigameSounds.correctGuess());
        return true;
    }

    public void tick(MinecraftServer server) {
        tickRegistration(server);
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
            broadcastToParticipants(server, "<gray>Smeargle is starting to clean up the canvas.</gray>");
        }

        if (activeRound.phase == RoundPhase.ANGER_REACTION) {
            tickAngerReaction(world);
            return;
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

            int clearedThisStep = 0;
            while (activeRound.nextCleanupIndex < activeRound.cleanupOrder.size() && clearedThisStep < activeRound.cleanupBlocksPerStep) {
                PixelArtTemplate.BlockPlacement placement = activeRound.cleanupOrder.get(activeRound.nextCleanupIndex);
                prepareSmeargleForPlacement(world, activeRound.origin, placement);
                placeBlock(world, activeRound.direction.transform(activeRound.origin, placement), "minecraft:air");
                activeRound.nextCleanupIndex++;
                clearedThisStep++;
            }
            if (activeRound.nextCleanupIndex >= activeRound.cleanupOrder.size()) {
                finishRound(server, false);
                return;
            }
            activeRound.cooldownTicks = activeRound.cleanupTicksPerPlacement;
            return;
        }

        if (activeRound.nextPlacementIndex >= activeRound.revealOrder.size()) {
            broadcastToParticipants(
                server,
                "<yellow>Smeargle finished the entire painting.</yellow> <gray>The Pokémon was</gray> <gold>" + MiniMessageText.escape(activeRound.template.pokemon()) + "</gold><gray>.</gray>"
            );
            beginCleanup(server);
            return;
        }

        int placedThisStep = 0;
        while (activeRound.nextPlacementIndex < activeRound.revealOrder.size() && placedThisStep < activeRound.paintBlocksPerStep) {
            PixelArtTemplate.BlockPlacement placement = activeRound.revealOrder.get(activeRound.nextPlacementIndex);
            prepareSmeargleForPlacement(world, activeRound.origin, placement);
            placeBlock(world, activeRound.direction.transform(activeRound.origin, placement), placement.blockId());
            activeRound.nextPlacementIndex++;
            placedThisStep++;

            if (activeRound.nextPlacementIndex % 8 == 0 || activeRound.nextPlacementIndex == activeRound.revealOrder.size()) {
                broadcastToParticipants(
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
                broadcastToParticipants(server, PokemonHintFormatter.firstLetterHint(activeRound.template));
            }
            if (!activeRound.silhouetteHintSent && revealed * 3L >= totalBlocks * 2L) {
                activeRound.silhouetteHintSent = true;
                broadcastToParticipants(server, PokemonHintFormatter.silhouetteHint(activeRound.template));
            }
            tryTriggerAngerReaction(world);
            if (activeRound.phase != RoundPhase.PAINTING) {
                return;
            }
        }
        activeRound.cooldownTicks = activeRound.ticksPerPlacement;
    }

    public void stop(MinecraftServer server, String message) {
        if (activeRound == null && activeSession == null) {
            return;
        }

        boolean cancelledSession = activeSession != null;
        if (activeRound != null) {
            clearActiveCanvas(server);
            clearTemporarySupport(server);
            despawnSmeargle(server);
        }
        activeRound = null;
        activeSession = null;
        broadcast(server, message);
        if (cancelledSession) {
            broadcast(server, "<gray>The active score session was cancelled and scores were cleared.</gray>");
        }
    }

    private StartResult startNextRound(ConfiguredCanvas configuredCanvas) {
        if (activeSession == null) {
            return StartResult.NO_ACTIVE_SESSION;
        }
        int roundNumber = activeSession.startRound();
        return start(
            configuredCanvas,
            activeSession.nextTemplate(templates, random),
            roundNumber,
            activeSession.totalRounds()
        );
    }

    private StartResult start(ConfiguredCanvas configuredCanvas, PixelArtTemplate template, int roundNumber, int totalRounds) {
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
        activeRound = new ActiveRound(
            world,
            configuredCanvas.dimensionId(),
            configuredCanvas.direction(),
            canvasOrigin,
            template,
            configuredCanvas.ticksPerPlacement(),
            random,
            roundNumber,
            totalRounds
        );
        activeRound.artistEntityId = spawnSmeargle(world, configuredCanvas.direction(), canvasOrigin);
        activeRound.currentSupportAnchor = configuredCanvas.direction().supportAnchor(canvasOrigin);
        activeRound.currentStandingY = canvasOrigin.getY();

        broadcastToParticipants(
            world.getServer(),
            "<aqua><bold>Round " + roundNumber + "/" + totalRounds + " has started!</bold></aqua> "
                + "<gray>Use</gray> <yellow>/guess <pokemon></yellow> <gray>to score points.</gray>"
        );
        broadcastToParticipants(world.getServer(), "<gray>Scoring:</gray> <gold>10</gold><gray> points for first correct, down to a minimum of </gray><gold>1</gold><gray>.</gray>");
        broadcastToParticipants(world.getServer(), PokemonHintFormatter.lengthHint(template));
        playSound(world, canvasOrigin, SmeargleMinigameSounds.roundStart());
        return StartResult.STARTED;
    }

    private void tickRegistration(MinecraftServer server) {
        if (activeSession == null || !activeSession.registrationOpen()) {
            return;
        }
        activeSession.tickRegistration();
        if (activeSession.registrationTicksRemaining() > 0) {
            return;
        }
        if (!activeSession.hasRegisteredPlayers()) {
            broadcast(server, "<gray>Smeargle registration ended with no participants, so the session was cancelled.</gray>");
            activeSession = null;
            return;
        }
        startRegisteredSession(server, false);
    }

    private boolean startRegisteredSession(MinecraftServer server, boolean forced) {
        if (activeSession == null) {
            return false;
        }
        ConfiguredCanvas configuredCanvas = configuredCanvas(server);
        if (configuredCanvas == null) {
            broadcast(server, "<red>The configured canvas dimension is unavailable. Session cancelled.</red>");
            activeSession = null;
            return false;
        }
        int registeredPlayers = activeSession.registeredPlayerCount();
        currentSessionParticipants = new HashSet<>(activeSession.getRegisteredPlayerIds());
        activeSession.closeRegistration();
        broadcast(
            server,
            forced
                ? "<yellow>An admin force-started the session with</yellow> <gold>" + registeredPlayers + "</gold> <yellow>registered player(s).</yellow>"
                : "<green>Registration closed with</green> <gold>" + registeredPlayers + "</gold> <green>player(s). Starting now!</green>"
        );
        StartResult result = startNextRound(configuredCanvas);
        if (result != StartResult.STARTED) {
            broadcast(server, "<red>Unable to start the round after registration.</red>");
            activeSession = null;
            return false;
        }
        return true;
    }

    private void finishRound(MinecraftServer server, boolean silent) {
        if (activeRound != null && activeRound.phase != RoundPhase.CLEARING) {
            clearActiveCanvas(server);
        }
        clearTemporarySupport(server);
        despawnSmeargle(server);
        activeRound = null;
        if (activeSession != null && activeSession.hasMoreRounds()) {
            ConfiguredCanvas configuredCanvas = configuredCanvas(server);
            if (configuredCanvas == null) {
                broadcastToParticipants(server, "<red>The configured canvas dimension is unavailable. Ending the active game session.</red>");
                activeSession = null;
                return;
            }
            StartResult nextResult = startNextRound(configuredCanvas);
            if (nextResult == StartResult.STARTED) {
                return;
            }
            broadcastToParticipants(server, "<red>Unable to start the next round. Ending the active game session.</red>");
            activeSession = null;
            return;
        }
        if (activeSession != null) {
            announceFinalWinners(server, activeSession);
            activeSession = null;
            return;
        }
        if (!silent) {
            broadcast(server, "<gray>Smeargle cleaned up the canvas. Use an admin start command to begin the next round.</gray>");
        }
    }

    private void announceFinalWinners(MinecraftServer server, ActiveSession session) {
        List<PlayerScore> leaderboard = session.leaderboard();
        if (leaderboard.isEmpty()) {
            broadcastToParticipants(server, "<gold><bold>All rounds are complete!</bold></gold> <gray>No players scored any points this session.</gray>");
            return;
        }

        Map<String, Integer> pointsByPlayer = new HashMap<>();
        for (PlayerScore score : leaderboard) {
            pointsByPlayer.put(score.name(), score.points());
        }
        List<String> winners = RoundScoring.winnerNames(pointsByPlayer);
        int winningScore = leaderboard.getFirst().points();
        StringBuilder standings = new StringBuilder();
        for (int i = 0; i < leaderboard.size(); i++) {
            PlayerScore score = leaderboard.get(i);
            if (i > 0) {
                standings.append("<gray>, </gray>");
            }
            standings
                .append("<gold>")
                .append(MiniMessageText.escape(score.name()))
                .append("</gold>")
                .append("<gray>: </gray><yellow>")
                .append(score.points())
                .append("</yellow>");
        }

        StringBuilder winnerSummary = new StringBuilder();
        for (int i = 0; i < winners.size(); i++) {
            if (i > 0) {
                winnerSummary.append("<gray>, </gray>");
            }
            winnerSummary.append("<gold>").append(MiniMessageText.escape(winners.get(i))).append("</gold>");
        }

        broadcastToParticipants(server, "<gold><bold>All rounds are complete!</bold></gold> <gray>Final scores:</gray> " + standings);
        broadcastToParticipants(
            server,
            winners.size() == 1
                ? "<green>Winner:</green> " + winnerSummary + " <gray>with</gray> <yellow>" + winningScore + "</yellow> <gray>points.</gray>"
                : "<green>Winners (tie):</green> " + winnerSummary + " <gray>with</gray> <yellow>" + winningScore + "</yellow> <gray>points.</gray>"
        );
    }

    private void beginCleanup(MinecraftServer server) {
        if (activeRound == null || activeRound.phase != RoundPhase.PAINTING) {
            return;
        }
        activeRound.phase = RoundPhase.WAITING_TO_CLEAR;
        activeRound.cleanupWaitTicksRemaining = CLEANUP_DELAY_TICKS;
        activeRound.cooldownTicks = 0;
        broadcastToParticipants(server, "<gray>Smeargle will start cleaning the canvas in 5 seconds.</gray>");
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
        BlockState currentState = world.getBlockState(pos);
        if (blockId.equals("minecraft:air")) {
            if (!currentState.isAir()) {
                world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
                BlockSoundGroup soundGroup = currentState.getSoundGroup();
                playSound(world, pos, soundGroup.getBreakSound(), SoundCategory.BLOCKS, soundGroup.getVolume() * 0.8F, soundGroup.getPitch() * 0.8F);
            }
            return;
        }

        BlockState state = resolveBlockState(blockId);
        world.setBlockState(pos, state, Block.NOTIFY_ALL);
        if (!state.isAir()) {
            BlockSoundGroup soundGroup = state.getSoundGroup();
            playSound(world, pos, soundGroup.getPlaceSound(), SoundCategory.BLOCKS, soundGroup.getVolume(), soundGroup.getPitch());
        }
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
            if (entity instanceof LivingEntity livingEntity) {
                var scaleAttribute = livingEntity.getAttributeInstance(EntityAttributes.GENERIC_SCALE);
                if (scaleAttribute != null) {
                    scaleAttribute.setBaseValue(3.2);
                }
            }
            if (entity instanceof MobEntity mobEntity) {
                mobEntity.setAiDisabled(true);
            }
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

    private boolean prepareSmeargleForPlacement(ServerWorld world, BlockPos origin, PixelArtTemplate.BlockPlacement placement) {
        SmeargleSupportColumn supportColumn = SmeargleSupportColumn.forPlacement(activeRound.direction, origin, placement);
        if (activeRound == null || activeRound.artistEntityId == null) {
            return true;
        }

        Entity entity = world.getEntity(activeRound.artistEntityId);
        if (entity == null) {
            return true;
        }

        activeRound.currentSupportAnchor = supportColumn.anchor();
        activeRound.currentStandingY = supportColumn.standingY();
        entity.refreshPositionAndAngles(
            activeRound.direction.artistX(supportColumn.anchor()),
            activeRound.direction.artistY(supportColumn.standingY()),
            activeRound.direction.artistZ(supportColumn.anchor()),
            activeRound.direction.yaw(),
            0.0F
        );
        return true;
    }

    private boolean moveSmeargleTowardColumn(ServerWorld world, SmeargleSupportColumn targetColumn, float yaw) {
        if (activeRound == null || activeRound.artistEntityId == null) {
            return true;
        }

        Entity entity = world.getEntity(activeRound.artistEntityId);
        if (entity == null) {
            return true;
        }

        SmeargleMovementPlanner.MovementFrame frame = SmeargleMovementPlanner.nextFrame(
            activeRound.currentSupportAnchor,
            activeRound.currentStandingY,
            activeRound.temporarySupportBlocks,
            targetColumn,
            activeRound.origin.getY()
        );
        updateTemporarySupport(world, frame.supportToRemove(), false);
        updateTemporarySupport(world, frame.supportToAdd(), true);
        activeRound.currentSupportAnchor = frame.anchor();
        activeRound.currentStandingY = frame.standingY();
        entity.refreshPositionAndAngles(
            activeRound.direction.artistX(frame.anchor()),
            activeRound.direction.artistY(frame.standingY()),
            activeRound.direction.artistZ(frame.anchor()),
            yaw,
            0.0F
        );
        return frame.readyToPaint();
    }

    private void tryTriggerAngerReaction(ServerWorld world) {
        if (activeRound == null || activeRound.phase != RoundPhase.PAINTING) {
            return;
        }

        int nextStage = SmeargleAngerMeter.stageForProgress(activeRound.nextPlacementIndex, activeRound.revealOrder.size());
        if (nextStage <= activeRound.angerStage) {
            return;
        }

        activeRound.angerStage = nextStage;
        activeRound.phase = RoundPhase.ANGER_REACTION;
        activeRound.frustrationSteps = SmeargleFrustrationSequence.stepsForStage(nextStage);
        activeRound.frustrationStepIndex = 0;
        activeRound.audienceYaw = audienceYaw(world);
        activeRound.cooldownTicks = 0;
        broadcastToParticipants(world.getServer(), configSupplier.get().angerMessage(nextStage, random));
        BlockPos soundPos = artistSoundPos();
        if (soundPos != null) {
            playSound(world, soundPos, SmeargleMinigameSounds.angerReaction(nextStage));
        }
    }

    private void tickAngerReaction(ServerWorld world) {
        if (activeRound == null || activeRound.phase != RoundPhase.ANGER_REACTION) {
            return;
        }

        if (activeRound.currentSupportAnchor != null && activeRound.currentStandingY > activeRound.origin.getY()) {
            SmeargleSupportColumn groundedColumn = SmeargleSupportColumn.forAnchor(
                activeRound.currentSupportAnchor,
                activeRound.origin.getY(),
                activeRound.origin.getY()
            );
            moveSmeargleTowardColumn(world, groundedColumn, activeRound.direction.yaw());
            return;
        }

        if (activeRound.frustrationStepIndex < activeRound.frustrationSteps.size()) {
            playFrustrationStep(world, activeRound.frustrationSteps.get(activeRound.frustrationStepIndex));
            activeRound.frustrationStepIndex++;
        }

        if (activeRound.frustrationStepIndex >= activeRound.frustrationSteps.size()) {
            restorePaintingPose(world);
            activeRound.phase = RoundPhase.PAINTING;
        }
    }

    private void playFrustrationStep(ServerWorld world, SmeargleFrustrationSequence.Step step) {
        if (activeRound == null || activeRound.artistEntityId == null || activeRound.currentSupportAnchor == null) {
            return;
        }

        Entity entity = world.getEntity(activeRound.artistEntityId);
        if (entity == null) {
            return;
        }

        double baseX = activeRound.direction.artistX(activeRound.currentSupportAnchor);
        double baseY = activeRound.direction.artistY(activeRound.currentStandingY);
        double baseZ = activeRound.direction.artistZ(activeRound.currentSupportAnchor);
        double targetX = activeRound.direction.audienceX(activeRound.currentSupportAnchor, step.forwardOffset());
        double targetZ = activeRound.direction.audienceZ(activeRound.currentSupportAnchor, step.forwardOffset());
        float yaw = step.facingMode() == SmeargleFrustrationSequence.FacingMode.AUDIENCE ? activeRound.audienceYaw : activeRound.direction.yaw();
        entity.refreshPositionAndAngles(targetX, baseY, targetZ, yaw, 0.0F);
        if (step.jump()) {
            entity.addVelocity(0.0D, 0.28D + (activeRound.angerStage * 0.05D), 0.0D);
        }
        if (step.particles()) {
            showAngerParticles(world, targetX, baseY + 1.1D, targetZ);
        }
    }

    private void restorePaintingPose(ServerWorld world) {
        if (activeRound == null || activeRound.artistEntityId == null || activeRound.currentSupportAnchor == null) {
            return;
        }

        Entity entity = world.getEntity(activeRound.artistEntityId);
        if (entity == null) {
            return;
        }
        entity.refreshPositionAndAngles(
            activeRound.direction.artistX(activeRound.currentSupportAnchor),
            activeRound.direction.artistY(activeRound.currentStandingY),
            activeRound.direction.artistZ(activeRound.currentSupportAnchor),
            activeRound.direction.yaw(),
            0.0F
        );
    }

    @Nullable
    private BlockPos artistSoundPos() {
        if (activeRound == null || activeRound.currentSupportAnchor == null) {
            return null;
        }
        return new BlockPos(activeRound.currentSupportAnchor.getX(), activeRound.currentStandingY, activeRound.currentSupportAnchor.getZ());
    }

    private float audienceYaw(ServerWorld world) {
        if (activeRound == null || activeRound.currentSupportAnchor == null) {
            return 0.0F;
        }

        double baseX = activeRound.direction.artistX(activeRound.currentSupportAnchor);
        double baseY = activeRound.direction.artistY(activeRound.currentStandingY);
        double baseZ = activeRound.direction.artistZ(activeRound.currentSupportAnchor);
        ServerPlayerEntity player = nearestPlayer(world, baseX, baseY, baseZ);
        if (player == null) {
            return activeRound.direction.audienceYaw();
        }
        return (float) (MathHelper.atan2(player.getZ() - baseZ, player.getX() - baseX) * (180.0F / (float) Math.PI)) - 90.0F;
    }

    @Nullable
    private ServerPlayerEntity nearestPlayer(ServerWorld world, double x, double y, double z) {
        ServerPlayerEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (ServerPlayerEntity player : world.getPlayers()) {
            double distance = player.squaredDistanceTo(x, y, z);
            if (distance < nearestDistance) {
                nearest = player;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private void showAngerParticles(ServerWorld world, double x, double y, double z) {
        int angryCount = activeRound.angerStage >= SmeargleAngerMeter.MAX_STAGE ? 8 : 4;
        world.spawnParticles(ParticleTypes.ANGRY_VILLAGER, x, y, z, angryCount, 0.3D, 0.4D, 0.3D, 0.02D);
        if (activeRound.angerStage >= SmeargleAngerMeter.MAX_STAGE) {
            world.spawnParticles(ParticleTypes.SMOKE, x, y, z, 10, 0.25D, 0.3D, 0.25D, 0.01D);
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

        updateTemporarySupport(world, Set.copyOf(activeRound.temporarySupportBlocks), false);
    }

    private void updateTemporarySupport(ServerWorld world, Set<BlockPos> positions, boolean place) {
        if (activeRound == null || positions.isEmpty()) {
            return;
        }

        for (BlockPos pos : positions) {
            if (place) {
                if (activeRound.temporarySupportBlocks.contains(pos)) {
                    continue;
                }
                if (world.getBlockState(pos).isAir()) {
                    world.setBlockState(pos, SMEARGLE_SUPPORT_BLOCK, Block.NOTIFY_ALL);
                    activeRound.temporarySupportBlocks.add(pos.toImmutable());
                }
                continue;
            }

            if (!activeRound.temporarySupportBlocks.remove(pos)) {
                continue;
            }
            if (world.getBlockState(pos).isOf(SMEARGLE_SUPPORT_BLOCK.getBlock())) {
                world.removeBlock(pos, false);
            }
        }
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

    private void broadcastToParticipants(MinecraftServer server, String message) {
        Text text = MiniMessageText.deserialize(server, message);
        for (UUID participantId : currentSessionParticipants) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(participantId);
            if (player != null) {
                player.sendMessage(text, false);
            }
        }
    }

    private void playRoundSound(MinecraftServer server, BlockPos pos, SmeargleMinigameSounds.SoundCue cue) {
        if (activeRound == null) {
            return;
        }

        ServerWorld world = server.getWorld(activeRound.worldKey);
        if (world == null) {
            return;
        }
        playSound(world, pos, cue);
    }

    private void playSound(ServerWorld world, BlockPos pos, SmeargleMinigameSounds.SoundCue cue) {
        Identifier identifier = Identifier.tryParse(cue.soundId());
        if (identifier == null) {
            return;
        }
        Registries.SOUND_EVENT.getOrEmpty(identifier).ifPresentOrElse(
            sound -> playSound(world, pos, sound, cue.category(), cue.volume(), cue.pitch()),
            () -> SmearglesPixelArtMod.LOGGER.warn("Unknown Smeargle minigame sound id {}", cue.soundId())
        );
    }

    private void playSound(ServerWorld world, BlockPos pos, SoundEvent sound, SoundCategory category, float volume, float pitch) {
        world.playSound(null, pos, sound, category, volume, pitch);
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
        private final int cleanupTicksPerPlacement;
        private final int paintBlocksPerStep;
        private final int cleanupBlocksPerStep;
        private final int roundNumber;
        private final int totalRounds;
        private RoundPhase phase;
        private int nextPlacementIndex;
        private int nextCleanupIndex;
        private int cooldownTicks;
        private int cleanupWaitTicksRemaining;
        private int angerStage;
        private List<SmeargleFrustrationSequence.Step> frustrationSteps = List.of();
        private int frustrationStepIndex;
        private float audienceYaw;
        private boolean firstLetterHintSent;
        private boolean silhouetteHintSent;
        @Nullable
        private UUID artistEntityId;
        @Nullable
        private BlockPos currentSupportAnchor;
        private int currentStandingY;
        private final Set<BlockPos> temporarySupportBlocks = new HashSet<>();
        private final Set<UUID> correctGuessers = new HashSet<>();

        private ActiveRound(
            ServerWorld world,
            String dimensionId,
            CanvasDirection direction,
            BlockPos origin,
            PixelArtTemplate template,
            int ticksPerPlacement,
            Random random,
            int roundNumber,
            int totalRounds
        ) {
            this.worldKey = world.getRegistryKey();
            this.dimensionId = dimensionId;
            this.direction = direction;
            this.origin = origin;
            this.template = template;
            this.revealOrder = template.revealOrder(PixelArtTemplate.fillPatternForRound(roundNumber, totalRounds), random);
            this.cleanupOrder = this.revealOrder.reversed();
            this.ticksPerPlacement = ticksPerPlacement;
            this.cleanupTicksPerPlacement = SmeargleCleanupPacing.ticksPerPlacement(ticksPerPlacement);
            this.paintBlocksPerStep = SmeargleRoundPacing.buildBlocksPerStep(this.revealOrder.size(), ticksPerPlacement);
            this.cleanupBlocksPerStep = SmeargleRoundPacing.cleanupBlocksPerStep(this.cleanupOrder.size(), this.cleanupTicksPerPlacement);
            this.cooldownTicks = ticksPerPlacement;
            this.phase = RoundPhase.PAINTING;
            this.roundNumber = roundNumber;
            this.totalRounds = totalRounds;
        }
    }

    private static final class ActiveSession {
        private final int totalRounds;
        @Nullable
        private final PixelArtTemplate firstTemplate;
        private boolean firstTemplatePending;
        private int roundsStarted;
        private boolean registrationOpen = true;
        private int registrationTicksRemaining = REGISTRATION_DURATION_TICKS;
        private final Set<UUID> registeredPlayerIds = new HashSet<>();
        private final Map<UUID, Integer> pointsByPlayerId = new HashMap<>();
        private final Map<UUID, String> namesByPlayerId = new HashMap<>();

        private ActiveSession(int totalRounds, @Nullable PixelArtTemplate firstTemplate) {
            this.totalRounds = totalRounds;
            this.firstTemplate = firstTemplate;
            this.firstTemplatePending = firstTemplate != null;
        }

        private static ActiveSession random(int totalRounds) {
            return new ActiveSession(totalRounds, null);
        }

        private static ActiveSession template(int totalRounds, PixelArtTemplate template) {
            return new ActiveSession(totalRounds, template);
        }

        private int startRound() {
            roundsStarted++;
            return roundsStarted;
        }

        private boolean register(ServerPlayerEntity player) {
            UUID playerId = player.getUuid();
            if (!registeredPlayerIds.add(playerId)) {
                return false;
            }
            namesByPlayerId.put(playerId, player.getName().getString());
            return true;
        }

        private PixelArtTemplate nextTemplate(PixelArtTemplateRegistry templates, Random random) {
            if (firstTemplatePending && firstTemplate != null) {
                firstTemplatePending = false;
                return firstTemplate;
            }
            return templates.randomTemplate(random);
        }

        private int totalRounds() {
            return totalRounds;
        }

        private boolean hasMoreRounds() {
            return roundsStarted < totalRounds;
        }

        private boolean registrationOpen() {
            return registrationOpen;
        }

        private int registrationTicksRemaining() {
            return registrationTicksRemaining;
        }

        private void tickRegistration() {
            if (registrationTicksRemaining > 0) {
                registrationTicksRemaining--;
            }
        }

        private boolean hasRegisteredPlayers() {
            return !registeredPlayerIds.isEmpty();
        }

        private int registeredPlayerCount() {
            return registeredPlayerIds.size();
        }

        private boolean isRegistered(UUID playerId) {
            return registeredPlayerIds.contains(playerId);
        }

        private Set<UUID> getRegisteredPlayerIds() {
            return Set.copyOf(registeredPlayerIds);
        }

        private void closeRegistration() {
            registrationOpen = false;
            registrationTicksRemaining = 0;
        }

        private void award(ServerPlayerEntity player, int points) {
            UUID playerId = player.getUuid();
            pointsByPlayerId.put(playerId, pointsByPlayerId.getOrDefault(playerId, 0) + points);
            namesByPlayerId.put(playerId, player.getName().getString());
        }

        private int pointsFor(UUID playerId) {
            return pointsByPlayerId.getOrDefault(playerId, 0);
        }

        private List<PlayerScore> leaderboard() {
            List<PlayerScore> leaderboard = new ArrayList<>();
            for (Map.Entry<UUID, Integer> entry : pointsByPlayerId.entrySet()) {
                leaderboard.add(new PlayerScore(
                    namesByPlayerId.getOrDefault(entry.getKey(), entry.getKey().toString()),
                    entry.getValue()
                ));
            }
            leaderboard.sort((left, right) -> {
                int byPoints = Integer.compare(right.points(), left.points());
                if (byPoints != 0) {
                    return byPoints;
                }
                return left.name().compareToIgnoreCase(right.name());
            });
            return leaderboard;
        }
    }

    private record PlayerScore(String name, int points) {
    }

    public record RecordedTemplate(String templateName, String pokemon, int width, int height, Path path) {
    }

    public enum StartResult {
        STARTED,
        ROUND_ALREADY_ACTIVE,
        TEMPLATE_NOT_FOUND,
        CONFIGURED_DIMENSION_UNAVAILABLE,
        INVALID_ROUND_COUNT,
        NO_ACTIVE_SESSION
    }

    public enum JoinResult {
        JOINED,
        ALREADY_JOINED,
        NO_REGISTRATION_ACTIVE,
        REGISTRATION_CLOSED
    }

    public enum ForceStartResult {
        STARTED,
        NO_REGISTRATION_ACTIVE,
        NO_REGISTERED_PLAYERS,
        CONFIGURED_DIMENSION_UNAVAILABLE
    }

    private record ConfiguredCanvas(ServerWorld world, String dimensionId, CanvasDirection direction, BlockPos origin, int ticksPerPlacement) {
    }

    private record CanvasKey(RegistryKey<World> worldKey, BlockPos origin) {
    }

    private enum RoundPhase {
        PAINTING,
        ANGER_REACTION,
        WAITING_TO_CLEAR,
        CLEARING
    }
}
