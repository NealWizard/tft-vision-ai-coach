package com.tft.coach.contracts;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads versioned JSON Schemas and validates payloads.
 * Schemas are read from classpath {@code /schemas/**} first, then optional filesystem override.
 */
public final class SchemaValidator {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final JsonSchemaFactory FACTORY = JsonSchemaFactory.getInstance(
            SpecVersion.VersionFlag.V202012,
            builder -> builder.schemaMappers(mappers ->
                    mappers.mapPrefix("https://tft.coach/schemas/", "classpath:/schemas/"))
    );

    private final Map<String, JsonSchema> cache = new ConcurrentHashMap<>();
    private final Path filesystemRoot;

    public SchemaValidator() {
        this(null);
    }

    public SchemaValidator(Path filesystemRoot) {
        this.filesystemRoot = filesystemRoot;
    }

    public Set<ValidationMessage> validate(String schemaLogicalName, JsonNode payload) {
        JsonSchema schema = cache.computeIfAbsent(schemaLogicalName, this::loadSchema);
        return schema.validate(payload);
    }

    public Set<ValidationMessage> validate(String schemaLogicalName, String json) {
        try {
            return validate(schemaLogicalName, MAPPER.readTree(json));
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid JSON payload for schema " + schemaLogicalName, e);
        }
    }

    public boolean isValid(String schemaLogicalName, String json) {
        return validate(schemaLogicalName, json).isEmpty();
    }

    private JsonSchema loadSchema(String schemaLogicalName) {
        String normalized = schemaLogicalName.toLowerCase(Locale.ROOT);
        String classpath = "/schemas/" + normalized;
        try (InputStream in = open(classpath, normalized)) {
            if (in == null) {
                throw new IllegalArgumentException("Schema not found: " + schemaLogicalName);
            }
            JsonNode node = MAPPER.readTree(in);
            return FACTORY.getSchema(node);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load schema: " + schemaLogicalName, e);
        }
    }

    private InputStream open(String classpath, String normalized) throws IOException {
        InputStream in = SchemaValidator.class.getResourceAsStream(classpath);
        if (in != null) {
            return in;
        }
        if (filesystemRoot != null) {
            Path path = filesystemRoot.resolve(normalized.replace('/', java.io.File.separatorChar));
            if (Files.isRegularFile(path)) {
                return Files.newInputStream(path);
            }
        }
        return null;
    }
}
