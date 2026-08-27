package com.adainish.smearglespixelart;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class GuessNormalizerTest {
    @Test
    void normalizesCommonPokemonPunctuation() {
        assertEquals("pikachu", GuessNormalizer.normalize("Pika-chu"));
        assertEquals("mrmime", GuessNormalizer.normalize("Mr. Mime"));
        assertEquals("farfetchd", GuessNormalizer.normalize("Farfetch'd"));
    }
}
