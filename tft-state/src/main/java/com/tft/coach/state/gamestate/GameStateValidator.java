package com.tft.coach.state.gamestate;

import com.tft.coach.contracts.SchemaValidator;
import com.networknt.schema.ValidationMessage;

import java.util.Objects;
import java.util.Set;

/**
 * Validates GameState against canonical schema.
 */
public final class GameStateValidator {

    public static final String SCHEMA = "canonical/gamestate.schema.json";

    private final SchemaValidator schemaValidator;

    public GameStateValidator() {
        this(new SchemaValidator());
    }

    public GameStateValidator(SchemaValidator schemaValidator) {
        this.schemaValidator = Objects.requireNonNull(schemaValidator);
    }

    public Set<ValidationMessage> validate(GameState state) {
        return schemaValidator.validate(SCHEMA, state.toJsonNode());
    }

    public void requireValid(GameState state) {
        Set<ValidationMessage> errors = validate(state);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("Invalid GameState: " + errors);
        }
    }
}
