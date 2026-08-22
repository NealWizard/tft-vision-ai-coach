package com.tft.coach.contracts;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.ValidationMessage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void canonicalSchemasAcceptMinimalPayloads() {
        Map<String, String> payloads = Map.ofEntries(
                Map.entry("canonical/champion.schema.json",
                        """
                        {"schema_version":"1.0.0","id":"champ.ahri","patch":"set17-16.16","name":"Ahri","cost":4}
                        """),
                Map.entry("canonical/trait.schema.json",
                        """
                        {"schema_version":"1.0.0","id":"trait.stargazer","patch":"set17-16.16","name":"Stargazer","breakpoints":[{"units":2,"style":"bronze"}]}
                        """),
                Map.entry("canonical/item.schema.json",
                        """
                        {"schema_version":"1.0.0","id":"item.infinity_edge","patch":"set17-16.16","name":"Infinity Edge","kind":"completed"}
                        """),
                Map.entry("canonical/augment.schema.json",
                        """
                        {"schema_version":"1.0.0","id":"augment.space_groove","patch":"set17-16.16","name":"Space Groove","tier":"gold"}
                        """),
                Map.entry("canonical/mechanic.schema.json",
                        """
                        {"schema_version":"1.0.0","id":"mechanic.portal","patch":"set17-16.16","name":"Portal","kind":"portal"}
                        """),
                Map.entry("canonical/rule.schema.json",
                        """
                        {"schema_version":"1.0.0","id":"rule.interest_cap","patch":"set17-16.16","category":"economy","key":"interest_cap","value":5}
                        """),
                Map.entry("canonical/patch.schema.json",
                        """
                        {"schema_version":"1.0.0","id":"set17-16.16","set_id":"set17","effective_at":"2026-08-22T00:00:00Z"}
                        """),
                Map.entry("canonical/evidence.schema.json",
                        """
                        {"schema_version":"1.0.0","id":"evidence:riot:set17","source_type":"riot","captured_at":"2026-08-22T00:00:00Z","patch":"set17-16.16"}
                        """),
                Map.entry("canonical/observation.schema.json",
                        """
                        {"schema_version":"1.0.0","field":"gold","value":50,"confidence":{"score":0.99,"level":"certain"},"source":"fixture","timestamp":"2026-08-22T00:00:00Z"}
                        """),
                Map.entry("canonical/gamestate.schema.json",
                        """
                        {"schema_version":"1.0.0","match_id":"match-1","patch":"set17-16.16","stage":"3-2","observed_at":"2026-08-22T00:00:00Z","player":{"level":6,"gold":50,"hp":80}}
                        """)
        );

        payloads.forEach((schema, payload) ->
                assertTrue(validator.isValid(schema, payload), () -> "Invalid payload for " + schema));
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

    @Test
    void packagedSchemasMatchRepositorySources() throws Exception {
        Path packagedRoot = Path.of("src", "main", "resources", "schemas").toAbsolutePath();
        try (Stream<Path> stream = Files.walk(repoSchemas)) {
            for (Path source : stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .toList()) {
                Path relative = repoSchemas.relativize(source);
                Path packaged = packagedRoot.resolve(relative);
                assertTrue(Files.isRegularFile(packaged), "packaged schema missing: " + relative);
                assertEquals(Files.readString(source), Files.readString(packaged),
                        "packaged schema differs: " + relative);
            }
        }
    }
}
