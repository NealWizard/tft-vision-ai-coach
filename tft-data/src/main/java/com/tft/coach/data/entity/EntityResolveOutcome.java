package com.tft.coach.data.entity;

import java.util.List;
import java.util.Optional;

public record EntityResolveOutcome(
        String sourceId,
        EntityKind kind,
        Optional<String> canonicalId,
        boolean pending
) {
    public static EntityResolveOutcome resolved(String sourceId, EntityKind kind, String canonicalId) {
        return new EntityResolveOutcome(sourceId, kind, Optional.of(canonicalId), false);
    }

    public static EntityResolveOutcome pending(String sourceId, EntityKind kind) {
        return new EntityResolveOutcome(sourceId, kind, Optional.empty(), true);
    }
}
