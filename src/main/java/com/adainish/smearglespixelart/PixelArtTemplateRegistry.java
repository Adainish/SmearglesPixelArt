package com.adainish.smearglespixelart;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

public final class PixelArtTemplateRegistry {
    private static final Gson GSON = new Gson();
    private static final Gson PRETTY_GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, String> BUILTIN_TEMPLATE_PATHS = Map.of(
        "pikachu", "assets/smearglespixelart/templates/pikachu.json",
        "voltorb", "assets/smearglespixelart/templates/voltorb.json"
    );

    private final Map<String, PixelArtTemplate> templates;

    private PixelArtTemplateRegistry(Map<String, PixelArtTemplate> templates) {
        this.templates = new LinkedHashMap<>(templates);
    }

    public static PixelArtTemplateRegistry loadBuiltins() {
        Map<String, PixelArtTemplate> loaded = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : BUILTIN_TEMPLATE_PATHS.entrySet()) {
            loaded.put(entry.getKey(), load(entry.getValue()));
        }
        return new PixelArtTemplateRegistry(loaded);
    }

    public Collection<String> templateNames() {
        return List.copyOf(templates.keySet());
    }

    public Optional<PixelArtTemplate> find(String templateName) {
        return Optional.ofNullable(templates.get(GuessNormalizer.normalize(templateName)));
    }

    public PixelArtTemplate randomTemplate(Random random) {
        List<PixelArtTemplate> values = List.copyOf(templates.values());
        return values.get(random.nextInt(values.size()));
    }

    public void loadCustomTemplates(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) {
            return;
        }

        try (var paths = Files.list(directory)) {
            for (Path path : paths.filter(file -> file.getFileName().toString().endsWith(".json")).sorted().toList()) {
                String fileName = path.getFileName().toString();
                String templateName = fileName.substring(0, fileName.length() - ".json".length());
                try {
                    put(templateName, load(path));
                } catch (IllegalStateException exception) {
                    SmearglesPixelArtMod.LOGGER.warn("Skipping invalid custom template file {}", path, exception);
                }
            }
        }
    }

    public Path saveCustomTemplate(Path directory, String templateName, SpriteTemplateRecorder.RecordedTemplate template) throws IOException {
        String normalizedTemplateName = GuessNormalizer.normalize(templateName);
        if (normalizedTemplateName.isEmpty()) {
            throw new IllegalArgumentException("Template names must contain letters or numbers.");
        }

        Files.createDirectories(directory);

        TemplateJson json = new TemplateJson();
        json.pokemon = template.pokemon();
        json.palette = template.palette();
        json.rows = template.rows();

        Path target = directory.resolve(normalizedTemplateName + ".json");
        try (Writer writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
            PRETTY_GSON.toJson(json, writer);
        }

        put(normalizedTemplateName, expand(json, target.toString()));
        return target;
    }

    private void put(String templateName, PixelArtTemplate template) {
        templates.put(GuessNormalizer.normalize(templateName), template);
    }

    private static PixelArtTemplate load(String resourcePath) {
        InputStream stream = PixelArtTemplateRegistry.class.getClassLoader().getResourceAsStream(resourcePath);
        if (stream == null) {
            throw new IllegalStateException("Missing built-in template resource: " + resourcePath);
        }

        try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            TemplateJson json = GSON.fromJson(reader, TemplateJson.class);
            return expand(json, resourcePath);
        } catch (IOException | JsonParseException exception) {
            throw new IllegalStateException("Unable to load template resource: " + resourcePath, exception);
        }
    }

    private static PixelArtTemplate load(Path path) {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            TemplateJson json = GSON.fromJson(reader, TemplateJson.class);
            return expand(json, path.toString());
        } catch (IOException | JsonParseException exception) {
            throw new IllegalStateException("Unable to load template file: " + path, exception);
        }
    }

    private static PixelArtTemplate expand(TemplateJson json, String resourcePath) {
        if (json == null || json.pokemon == null || json.pokemon.isBlank()) {
            throw new IllegalStateException("Template is missing a pokemon name: " + resourcePath);
        }
        if (json.rows == null || json.rows.isEmpty()) {
            throw new IllegalStateException("Template is missing rows: " + resourcePath);
        }
        if (json.palette == null || json.palette.isEmpty()) {
            throw new IllegalStateException("Template is missing a palette: " + resourcePath);
        }

        int width = json.rows.getFirst().length();
        List<PixelArtTemplate.BlockPlacement> blocks = new ArrayList<>();

        for (int rowIndex = 0; rowIndex < json.rows.size(); rowIndex++) {
            String row = Objects.requireNonNull(json.rows.get(rowIndex), "row");
            if (row.length() != width) {
                throw new IllegalStateException("Template rows must all have the same width: " + resourcePath);
            }

            for (int column = 0; column < row.length(); column++) {
                char key = row.charAt(column);
                if (key == '.') {
                    continue;
                }

                String blockId = json.palette.get(String.valueOf(key));
                if (blockId == null) {
                    throw new IllegalStateException("Template palette is missing a block mapping for '" + key + "': " + resourcePath);
                }

                int y = (json.rows.size() - 1) - rowIndex;
                blocks.add(new PixelArtTemplate.BlockPlacement(column, y, 0, blockId));
            }
        }

        return new PixelArtTemplate(
            json.pokemon,
            GuessNormalizer.normalize(json.pokemon),
            width,
            json.rows.size(),
            blocks
        );
    }

    private static final class TemplateJson {
        private String pokemon;
        private Map<String, String> palette = Map.of();
        private List<String> rows = List.of();
    }
}
