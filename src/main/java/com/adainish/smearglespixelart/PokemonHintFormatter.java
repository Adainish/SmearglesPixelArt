package com.adainish.smearglespixelart;

public final class PokemonHintFormatter {
    private PokemonHintFormatter() {
    }

    public static String lengthHint(PixelArtTemplate template) {
        int length = template.normalizedPokemon().length();
        return "<gray>Hint:</gray> <yellow>The Pokémon's name has " + length + " letters.</yellow>";
    }

    public static String firstLetterHint(PixelArtTemplate template) {
        String normalized = template.normalizedPokemon();
        String letter = normalized.isEmpty() ? "?" : String.valueOf(Character.toUpperCase(normalized.charAt(0)));
        return "<gray>Hint:</gray> <yellow>The Pokémon's name starts with " + letter + ".</yellow>";
    }

    public static String silhouetteHint(PixelArtTemplate template) {
        String normalized = template.normalizedPokemon();
        if (normalized.isEmpty()) {
            return "<gray>Hint:</gray> <yellow>No additional hint is available.</yellow>";
        }

        StringBuilder pattern = new StringBuilder();
        for (int index = 0; index < normalized.length(); index++) {
            if (index > 0) {
                pattern.append(' ');
            }

            if (index == 0 || index == normalized.length() - 1) {
                pattern.append(Character.toUpperCase(normalized.charAt(index)));
            } else {
                pattern.append('_');
            }
        }

        return "<gray>Hint:</gray> <yellow>Name pattern:</yellow> <gold>" + pattern + "</gold>";
    }
}
