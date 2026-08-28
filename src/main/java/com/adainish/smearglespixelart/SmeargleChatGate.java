package com.adainish.smearglespixelart;

import java.util.Set;
import java.util.UUID;

final class SmeargleChatGate {
    private SmeargleChatGate() {
    }

    static boolean shouldBlock(boolean roundActive, Set<UUID> registeredPlayerIds, UUID playerId) {
        return roundActive && registeredPlayerIds.contains(playerId);
    }
}
