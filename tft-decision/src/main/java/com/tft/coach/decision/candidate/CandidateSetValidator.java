package com.tft.coach.decision.candidate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.networknt.schema.ValidationMessage;
import com.tft.coach.contracts.SchemaValidator;

import java.util.Objects;
import java.util.Set;

public final class CandidateSetValidator {

    public static final String SCHEMA = "canonical/candidate-set.schema.json";

    private final SchemaValidator schemaValidator;
    private final ObjectMapper mapper;

    public CandidateSetValidator() {
        this(new SchemaValidator());
    }

    public CandidateSetValidator(SchemaValidator schemaValidator) {
        this.schemaValidator = Objects.requireNonNull(schemaValidator);
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public Set<ValidationMessage> validate(CandidateSet set) {
        JsonNode node = mapper.valueToTree(set);
        return schemaValidator.validate(SCHEMA, node);
    }

    public void requireValid(CandidateSet set) {
        Set<ValidationMessage> errors = validate(set);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("Invalid CandidateSet: " + errors);
        }
        for (CandidateSet.CandidateOption option : set.candidates()) {
            if (option.evidence().isEmpty() && !"low".equals(option.confidence().level())) {
                throw new IllegalArgumentException("INV-003: empty evidence requires confidence.level=low");
            }
        }
    }

    public ObjectMapper mapper() {
        return mapper;
    }
}
