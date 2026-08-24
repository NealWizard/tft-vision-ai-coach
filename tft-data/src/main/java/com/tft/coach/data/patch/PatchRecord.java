package com.tft.coach.data.patch;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/** Managed Set/Patch metadata (`P1-DATA-Patch-001`). */
public record PatchRecord(
        String id,
        String setId,
        Instant effectiveAt,
        Instant retiredAt,
        PatchStatus status,
        Duration ttl
) {
    public PatchRecord {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(setId, "setId");
        Objects.requireNonNull(effectiveAt, "effectiveAt");
        Objects.requireNonNull(status, "status");
        ttl = ttl == null ? Duration.ofDays(7) : ttl;
    }

    public boolean isExpired(Instant now) {
        return retiredAt != null && now.isAfter(retiredAt);
    }
}
