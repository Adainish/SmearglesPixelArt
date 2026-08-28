package com.adainish.smearglespixelart;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class PokemonHintFormatterTest {
    private static PixelArtTemplate template(String pokemon, String normalizedPokemon) {
        return new PixelArtTemplate(
            pokemon,
            normalizedPokemon,
            1,
            1,
            List.of(new PixelArtTemplate.BlockPlacement(0, 0, 0, "minecraft:white_concrete"))
        );
    }

    @Test
    void formatsLengthHintFromNormalizedName() {
        assertEquals(
            "<gray>Hint:</gray> <yellow>The Pokémon's name has 6 letters.</yellow>",
            PokemonHintFormatter.lengthHint(template("Mr. Mime", "mrmime"))
        );
    }

    @Test
    void formatsFirstLetterHint() {
        assertEquals(
            "<gray>Hint:</gray> <yellow>The Pokémon's name starts with P.</yellow>",
            PokemonHintFormatter.firstLetterHint(template("Pikachu", "pikachu"))
        );
    }

    @Test
    void formatsSilhouetteHint() {
        assertEquals(
            "<gray>Hint:</gray> <yellow>Name pattern:</yellow> <gold>P _ _ _ _ _ U</gold>",
            PokemonHintFormatter.silhouetteHint(template("Pikachu", "pikachu"))
        );
    }
}
