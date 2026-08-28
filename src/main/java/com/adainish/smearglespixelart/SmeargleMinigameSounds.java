package com.adainish.smearglespixelart;

import net.minecraft.sound.SoundCategory;

final class SmeargleMinigameSounds {
    private static final SoundCue ROUND_START = new SoundCue("minecraft:block.note_block.chime", SoundCategory.PLAYERS, 0.8F, 1.15F);
    private static final SoundCue CORRECT_GUESS = new SoundCue("minecraft:entity.player.levelup", SoundCategory.PLAYERS, 0.9F, 1.1F);
    private static final String ANGER_REACTION_SOUND_ID = "minecraft:entity.villager.no";

    private SmeargleMinigameSounds() {
    }

    static SoundCue roundStart() {
        return ROUND_START;
    }

    static SoundCue correctGuess() {
        return CORRECT_GUESS;
    }

    static SoundCue angerReaction(int stage) {
        int clampedStage = Math.max(1, Math.min(SmeargleAngerMeter.MAX_STAGE, stage));
        float pitch = 0.95F - ((clampedStage - 1) * 0.15F);
        return new SoundCue(ANGER_REACTION_SOUND_ID, SoundCategory.NEUTRAL, 0.85F, pitch);
    }

    record SoundCue(String soundId, SoundCategory category, float volume, float pitch) {
    }
}
