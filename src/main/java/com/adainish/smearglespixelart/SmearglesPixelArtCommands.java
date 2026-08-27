package com.adainish.smearglespixelart;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public final class SmearglesPixelArtCommands {
    private SmearglesPixelArtCommands() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, net.minecraft.command.CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("smearglespixelart")
            .requires(source -> source.hasPermissionLevel(2))
            .then(CommandManager.literal("start")
                .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                    .executes(context -> {
                        boolean started = SmearglesPixelArtMod.getManager().startRandom(
                            context.getSource().getWorld(),
                            BlockPosArgumentType.getBlockPos(context, "pos")
                        );
                        if (!started) {
                            context.getSource().sendFeedback(() -> MiniMessageText.deserialize(context.getSource().getServer(), "<red>A round is already active.</red>"), false);
                            return 0;
                        }
                        return 1;
                    })
                )
                .then(CommandManager.argument("template", StringArgumentType.word())
                    .suggests((context, builder) -> CommandSource.suggestMatching(SmearglesPixelArtMod.getManager().templateNames(), builder))
                    .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                        .executes(context -> {
                            boolean started = SmearglesPixelArtMod.getManager().startTemplate(
                                context.getSource().getWorld(),
                                BlockPosArgumentType.getBlockPos(context, "pos"),
                                StringArgumentType.getString(context, "template")
                            );
                            if (!started) {
                                context.getSource().sendFeedback(() -> MiniMessageText.deserialize(context.getSource().getServer(), "<red>That template was not found, or a round is already active.</red>"), false);
                                return 0;
                            }
                            return 1;
                        })
                    )
                )
            )
            .then(CommandManager.literal("stop")
                .executes(context -> {
                    if (!SmearglesPixelArtMod.getManager().hasActiveRound()) {
                        context.getSource().sendFeedback(() -> MiniMessageText.deserialize(context.getSource().getServer(), "<gray>No active round to stop.</gray>"), false);
                        return 0;
                    }
                    SmearglesPixelArtMod.getManager().stop(context.getSource().getServer(), "<yellow>An admin stopped the current Smeargle round.</yellow>");
                    return 1;
                })
            )
            .then(CommandManager.literal("list")
                .executes(context -> {
                    String available = String.join(", ", SmearglesPixelArtMod.getManager().templateNames());
                    context.getSource().sendFeedback(() -> MiniMessageText.deserialize(context.getSource().getServer(), "<gold>Available templates:</gold> <gray>" + MiniMessageText.escape(available) + "</gray>"), false);
                    return 1;
                })
            )
            .then(CommandManager.literal("status")
                .executes(context -> {
                    context.getSource().sendFeedback(() -> SmearglesPixelArtMod.getManager().describeStatus(context.getSource().getServer()), false);
                    return 1;
                })
            )
        );

        dispatcher.register(CommandManager.literal("guess")
            .requires(source -> source.getEntity() instanceof net.minecraft.server.network.ServerPlayerEntity)
            .then(CommandManager.argument("pokemon", StringArgumentType.greedyString())
                .executes(context -> SmearglesPixelArtMod.getManager().guess(
                    context.getSource().getPlayerOrThrow(),
                    StringArgumentType.getString(context, "pokemon")
                ) ? 1 : 0)
            )
        );
    }
}
