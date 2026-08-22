package com.tft.coach.contracts;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.ValidationMessage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SchemaContractTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static SchemaValidator validator;
    private static Path repoSchemas;

    @BeforeAll
    static void init() {
        Path cwd = Path.of("").toAbsolutePath();
        Path candidate = cwd.resolve("schemas");
        if (!Files.isDirectory(candidate)) {
            candidate = cwd.getParent().resolve("schemas");
        }
        repoSchemas = candidate;
        validator = new SchemaValidator(repoSchemas);
    }

    @Test
    void championSchemaAcceptsValidPayload() throws Exception {
        String json = """
                {
                  "schema_version": "1.0.0",
                  "id": "champ.jinx",
                  "patch": "set17-14.23",
                  "name": "Jinx",
                  "cost": 4,
                  "traits": ["trait.stargazer"]
                }
                """;
        Set<ValidationMessage> errors = validator.validate("canonical/champion.schema.json", json);
        assertTrue(errors.isEmpty(), errors::toString);
    }

    @Test
    void championSchemaRejectsInvalidCost() {
        String json = """
                {
                  "schema_version": "1.0.0",
                  "id": "champ.jinx",
                  "patch": "set17-14.23",
                  "name": "Jinx",
                  "cost": 9
                }
                """;
        assertFalse(validator.isValid("canonical/champion.schema.json", json));
    }

    @Test
    void agentSamplesValidateAgainstContract() throws Exception {
        Path samplesDir = repoSchemas.resolve("agent").resolve("samples");
        assertTrue(Files.isDirectory(samplesDir), "samples dir missing: " + samplesDir);
        try (Stream<Path> stream = Files.list(samplesDir)) {
            stream.filter(p -> p.getFileName().toString().endsWith(".json")).forEach(path -> {
                try {
                    JsonNode node = MAPPER.readTree(path.toFile());
                    Set<ValidationMessage> errors = validator.validate("agent/agent-contract.schema.json", node);
                    assertTrue(errors.isEmpty(), () -> path + " => " + errors);
                } catch (Exception e) {
                    throw new AssertionError("Failed validating " + path, e);
                }
            });
        }
    }

    @Test
    void requiredCanonicalSchemasPresentOnClasspath() {
        String[] required = {
                "/schemas/canonical/champion.schema.json",
                "/schemas/canonical/trait.schema.json",
                "/schemas/canonical/item.schema.json",
                "/schemas/canonical/augment.schema.json",
                "/schemas/canonical/mechanic.schema.json",
                "/schemas/canonical/rule.schema.json",
                "/schemas/canonical/patch.schema.json",
                "/schemas/canonical/evidence.schema.json",
                "/schemas/canonical/observation.schema.json",
                "/schemas/canonical/gamestate.schema.json",
                "/schemas/agent/agent-contract.schema.json"
        };
        for (String path : required) {
            InputStream in = SchemaValidator.class.getResourceAsStream(path);
            assertTrue(in != null, "missing classpath schema: " + path);
            try {
                in.close();
            } catch (Exception ignored) {
                // ignore
            }
        }
    }
}
