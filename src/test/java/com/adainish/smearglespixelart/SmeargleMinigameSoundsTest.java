package com.adainish.smearglespixelart;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.sound.SoundCategory;
import org.junit.jupiter.api.Test;

class SmeargleMinigameSoundsTest {
    @Test
    void roundStartUsesChimeCue() {
        SmeargleMinigameSounds.SoundCue cue = SmeargleMinigameSounds.roundStart();

        assertEquals("minecraft:block.note_block.chime", cue.soundId());
        assertEquals(SoundCategory.PLAYERS, cue.category());
        assertEquals(0.8F, cue.volume());
        assertEquals(1.15F, cue.pitch());
    }

    @Test
    void correctGuessUsesCelebrationCue() {
        SmeargleMinigameSounds.SoundCue cue = SmeargleMinigameSounds.correctGuess();

        assertEquals("minecraft:entity.player.levelup", cue.soundId());
        assertEquals(SoundCategory.PLAYERS, cue.category());
        assertEquals(0.9F, cue.volume());
        assertEquals(1.1F, cue.pitch());
    }

    @Test
    void angerReactionGetsLowerPitchedAtHigherStages() {
        SmeargleMinigameSounds.SoundCue firstStage = SmeargleMinigameSounds.angerReaction(1);
        SmeargleMinigameSounds.SoundCue thirdStage = SmeargleMinigameSounds.angerReaction(3);
        SmeargleMinigameSounds.SoundCue clampedStage = SmeargleMinigameSounds.angerReaction(99);

        assertEquals("minecraft:entity.villager.no", firstStage.soundId());
        assertEquals(SoundCategory.NEUTRAL, firstStage.category());
        assertEquals(0.85F, firstStage.volume());
        assertEquals(0.95F, firstStage.pitch());
        assertEquals(0.65F, thirdStage.pitch());
        assertEquals(thirdStage.pitch(), clampedStage.pitch());
    }
}
