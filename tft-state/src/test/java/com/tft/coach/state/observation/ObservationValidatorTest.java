package com.tft.coach.state.observation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.ValidationMessage;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservationValidatorTest {

    private final ObservationValidator validator = new ObservationValidator();
    private final ObjectMapper mapper = Observation.defaultMapper();

    @Test
    void fixtureObservationIsValid() {
        Observation obs = ObservationFactory.fromFixture("player.gold", 50);
        assertTrue(validator.isValid(obs));
    }

    @Test
    void ocrObservationIsValid() {
        Observation obs = ObservationFactory.fromOcr(
                "player.gold", 41, "4l", 0.96, "paddle", "paddleocr", "frame-1",
                new Observation.Rect(1, 2, 3, 4)
        );
        assertTrue(validator.isValid(obs));
    }

    @Test
    void missingFieldRejected() {
        Observation bad = new Observation(
                "1.0.0",
                "obs-1",
                "",
                1,
                null,
                new Observation.Confidence(0.9, "high"),
                "fixture",
                null,
                null,
                null,
                null,
                Instant.parse("2026-08-27T00:00:00Z"),
                null,
                null,
                null
        );
        Set<ValidationMessage> errors = validator.validate(bad);
        assertFalse(errors.isEmpty());
        assertThrows(IllegalArgumentException.class, () -> validator.requireValid(bad));
    }

    @Test
    void invalidSourceRejected() {
        ObjectNode node = baseNode();
        node.put("source", "telepathy");
        assertFalse(validator.validate(node).isEmpty());
    }

    @Test
    void wrongSchemaVersionRejected() {
        ObjectNode node = baseNode();
        node.put("schema_version", "1.1.0");
        assertFalse(validator.validate(node).isEmpty());
    }

    @Test
    void missingConfidenceRejected() {
        ObjectNode node = baseNode();
        node.remove("confidence");
        assertFalse(validator.validate(node).isEmpty());
    }

    @Test
    void invalidBboxShapeRejected() {
        ObjectNode node = baseNode();
        ObjectNode bbox = node.putObject("bbox");
        bbox.put("x", 1);
        bbox.put("extra", true);
        assertFalse(validator.validate(node).isEmpty());
    }

    private ObjectNode baseNode() {
        ObjectNode node = mapper.createObjectNode();
        node.put("schema_version", "1.0.0");
        node.put("field", "player.gold");
        node.put("value", 50);
        ObjectNode confidence = node.putObject("confidence");
        confidence.put("score", 0.9);
        confidence.put("level", "high");
        node.put("source", "fixture");
        node.put("timestamp", "2026-08-27T00:00:00Z");
        return node;
    }
}
