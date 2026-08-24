package com.tft.coach.data.fetch;

import com.tft.coach.data.spi.SourceType;

import java.time.Instant;
import java.util.Objects;

/** Last observed health of one configured source adapter. */
public record SourceHealth(
        SourceType sourceType,
        String sourceId,
        SourceHealthStatus status,
        Instant checkedAt,
        String message
) {
    public SourceHealth {
        Objects.requireNonNull(sourceType, "sourceType");
        Objects.requireNonNull(sourceId, "sourceId");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(checkedAt, "checkedAt");
        Objects.requireNonNull(message, "message");
    }
}
