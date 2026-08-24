package com.tft.coach.data.normalize;

import com.tft.coach.data.entity.EntityKind;

import java.util.Map;
import java.util.Objects;

/** Canonical DTO plus preserved raw payload (`P1-DATA-Normalize-001`). */
public record NormalizedEntity(
        String canonicalId,
        EntityKind kind,
        String patch,
        Map<String, Object> canonical,
        byte[] rawPayload,
        String rawSourceType,
        String rawSourceId
) {
    public NormalizedEntity {
        Objects.requireNonNull(canonicalId, "canonicalId");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(patch, "patch");
        Objects.requireNonNull(canonical, "canonical");
        rawPayload = rawPayload == null ? new byte[0] : rawPayload.clone();
    }
}
