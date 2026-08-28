package com.tft.coach.state.observation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.ValidationMessage;
import com.tft.coach.contracts.SchemaValidator;

import java.util.Objects;
import java.util.Set;

/**
 * Validates Observation payloads against canonical schema.
 */
public final class ObservationValidator {

    public static final String SCHEMA = "canonical/observation.schema.json";

    private final SchemaValidator schemaValidator;
    private final ObjectMapper mapper;

    public ObservationValidator() {
        this(new SchemaValidator(), Observation.defaultMapper());
    }

    public ObservationValidator(SchemaValidator schemaValidator, ObjectMapper mapper) {
        this.schemaValidator = Objects.requireNonNull(schemaValidator);
        this.mapper = Objects.requireNonNull(mapper);
    }

    public Set<ValidationMessage> validate(Observation observation) {
        return schemaValidator.validate(SCHEMA, observation.toJsonNode(mapper));
    }

    public Set<ValidationMessage> validate(JsonNode node) {
        return schemaValidator.validate(SCHEMA, node);
    }

    public boolean isValid(Observation observation) {
        return validate(observation).isEmpty();
    }

    public void requireValid(Observation observation) {
        Set<ValidationMessage> errors = validate(observation);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("Invalid observation: " + errors);
        }
    }
}
