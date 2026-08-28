package com.tft.coach.state.observation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Observation DTO aligned with canonical observation.schema.json (1.0.x).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Observation(
        @JsonProperty("schema_version") String schemaVersion,
        @JsonProperty("observation_id") String observationId,
        String field,
        Object value,
        @JsonProperty("raw_value") String rawValue,
        Confidence confidence,
        String source,
        String detector,
        String model,
        @JsonProperty("model_version") String modelVersion,
        @JsonProperty("preprocess_version") String preprocessVersion,
        Instant timestamp,
        Rect roi,
        Rect bbox,
        @JsonProperty("frame_id") String frameId
) {
    public Observation {
        Objects.requireNonNull(schemaVersion, "schemaVersion");
        Objects.requireNonNull(field, "field");
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(confidence, "confidence");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(timestamp, "timestamp");
    }

    public record Confidence(double score, String level) {
    }

    public record Rect(double x, double y, double w, double h) {
    }

    public JsonNode toJsonNode(ObjectMapper mapper) {
        return mapper.valueToTree(this);
    }

    public static ObjectMapper defaultMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * Convenience builder for snake_case map payloads in tests.
     */
    public static Observation fromMap(Map<String, Object> map, ObjectMapper mapper) {
        return mapper.convertValue(map, Observation.class);
    }

    public ObjectNode toObjectNode(ObjectMapper mapper) {
        return (ObjectNode) toJsonNode(mapper);
    }
}
