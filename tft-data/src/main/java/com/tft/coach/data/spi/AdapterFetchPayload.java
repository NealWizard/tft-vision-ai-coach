package com.tft.coach.data.spi;

import java.time.Instant;
import java.util.Objects;

/**
 * Result of a live fetch before snapshot persistence.
 */
public record AdapterFetchPayload(
        byte[] body,
        String contentType,
        Instant capturedAt,
        String patch
) {
    public AdapterFetchPayload {
        Objects.requireNonNull(body, "body");
        Objects.requireNonNull(capturedAt, "capturedAt");
        if (body.length == 0) {
            throw new IllegalArgumentException("body must not be empty");
        }
    }
}
