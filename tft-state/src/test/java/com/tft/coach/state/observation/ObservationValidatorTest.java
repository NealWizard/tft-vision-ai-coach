package com.tft.coach.state.observation;

import com.networknt.schema.ValidationMessage;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservationValidatorTest {

    private final ObservationValidator validator = new ObservationValidator();

    @Test
    void fixtureObservationIsValid() {
        Observation obs = ObservationFactory.fromFixture("player.gold", 50);
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
}
