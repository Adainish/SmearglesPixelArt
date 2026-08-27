package com.adainish.smearglespixelart;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

public final class MiniMessageText {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final GsonComponentSerializer GSON_SERIALIZER = GsonComponentSerializer.gson();

    private MiniMessageText() {
    }

    public static Text deserialize(MinecraftServer server, String input) {
        String json = GSON_SERIALIZER.serialize(MINI_MESSAGE.deserialize(input));
        MutableText text = Text.Serialization.fromJson(json, server.getRegistryManager());
        return text != null ? text : Text.literal(MINI_MESSAGE.stripTags(input));
    }

    public static String escape(String input) {
        return MINI_MESSAGE.escapeTags(input == null ? "" : input);
    }
}
