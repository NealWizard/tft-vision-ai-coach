package com.tft.coach.data.entity;

import java.time.Instant;
import java.util.Objects;

/** Unknown entity waiting for manual alias registration. */
public record PendingEntity(
        String sourceId,
        String sourceType,
        EntityKind kind,
        Instant enqueuedAt,
        String reason
) {
    public PendingEntity {
        Objects.requireNonNull(sourceId, "sourceId");
        Objects.requireNonNull(sourceType, "sourceType");
        Objects.requireNonNull(kind, "kind");
        enqueuedAt = enqueuedAt == null ? Instant.now() : enqueuedAt;
        reason = reason == null ? "unresolved" : reason;
    }
}
