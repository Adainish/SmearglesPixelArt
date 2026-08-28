package com.adainish.smearglespixelart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Test;

class PixelArtTemplateRegistryTest {
    @Test
    void loadsBuiltInTemplates() {
        PixelArtTemplateRegistry registry = PixelArtTemplateRegistry.loadBuiltins();

        assertTrue(registry.find("pikachu").isPresent());
        assertTrue(registry.find("voltorb").isPresent());
        assertEquals(2, registry.templateNames().size());
    }

    @Test
    void savesAndLoadsCustomTemplates(@TempDir Path tempDir) throws IOException {
        PixelArtTemplateRegistry registry = PixelArtTemplateRegistry.loadBuiltins();
        SpriteTemplateRecorder.RecordedTemplate recorded = new SpriteTemplateRecorder.RecordedTemplate(
            "Smeargle",
            java.util.Map.of("1", "minecraft:white_concrete"),
            java.util.List.of("11", "1.")
        );

        registry.saveCustomTemplate(tempDir, "Smeargle-Test", recorded);

        PixelArtTemplateRegistry reloaded = PixelArtTemplateRegistry.loadBuiltins();
        reloaded.loadCustomTemplates(tempDir);

        assertTrue(reloaded.find("smeargle-test").isPresent());
        assertEquals("smeargle", reloaded.find("smeargle-test").orElseThrow().normalizedPokemon());
    }
}
