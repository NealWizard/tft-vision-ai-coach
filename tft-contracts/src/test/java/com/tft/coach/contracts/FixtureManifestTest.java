package com.tft.coach.contracts;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FixtureManifestTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void fixtureManifestsCoverRequiredDatasetTypes() throws Exception {
        Path fixtures = repositoryRoot().resolve("fixtures");
        List<Path> manifests;
        try (Stream<Path> stream = Files.walk(fixtures)) {
            manifests = stream
                    .filter(path -> path.getFileName().toString().equals("manifest.json"))
                    .toList();
        }

        assertTrue(manifests.size() >= 6, "at least six fixture manifests are required");
        for (Path manifest : manifests) {
            JsonNode node = MAPPER.readTree(manifest.toFile());
            assertEquals("1.0.0", node.path("schema_version").asText(), manifest.toString());
            assertTrue(node.path("dataset").isTextual(), manifest.toString());
            assertTrue(node.path("version").isTextual(), manifest.toString());
            assertTrue(node.path("patch_scope").isArray(), manifest.toString());
            assertTrue(node.path("label_fields").isArray(), manifest.toString());
            assertTrue(node.path("sample_count").canConvertToInt(), manifest.toString());
        }

        assertTrue(Files.isRegularFile(fixtures.resolve("knowledge_qa_set/manifest.json")));
        assertTrue(Files.isRegularFile(fixtures.resolve("video_replay_set/manifest.json")));
    }

    private static Path repositoryRoot() {
        Path cwd = Path.of("").toAbsolutePath();
        return Files.isDirectory(cwd.resolve("fixtures")) ? cwd : cwd.getParent();
    }
}
