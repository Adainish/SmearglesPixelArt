package com.adainish.smearglespixelart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PixelArtTemplateRegistryTest {
    @Test
    void loadsBuiltInTemplates() {
        PixelArtTemplateRegistry registry = PixelArtTemplateRegistry.loadBuiltins();

        assertTrue(registry.find("pikachu").isPresent());
        assertTrue(registry.find("voltorb").isPresent());
        assertEquals(2, registry.templateNames().size());
    }
}
