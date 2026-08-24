package com.tft.coach.data.entity;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Maps source-native IDs to canonical `{type}.{slug}` identifiers.
 * Unknown entities are queued instead of inventing unstable IDs.
 */
public final class CanonicalEntityResolver {

    private final Map<String, String> aliases = new HashMap<>();
    private final PendingEntityQueue pendingQueue;

    public CanonicalEntityResolver() {
        this(new PendingEntityQueue());
    }

    public CanonicalEntityResolver(PendingEntityQueue pendingQueue) {
        this.pendingQueue = Objects.requireNonNull(pendingQueue, "pendingQueue");
    }

    public EntityResolveOutcome resolve(String sourceType, EntityKind kind, String sourceId) {
        Objects.requireNonNull(sourceType, "sourceType");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(sourceId, "sourceId");

        String aliasKey = aliasKey(sourceType, kind, sourceId);
        String known = aliases.get(aliasKey);
        if (known != null) {
            return EntityResolveOutcome.resolved(sourceId, kind, known);
        }

        String derived = CanonicalIdSlugs.fromSourceId(kind, sourceId);
        if (derived != null) {
            registerAlias(sourceType, kind, sourceId, derived);
            return EntityResolveOutcome.resolved(sourceId, kind, derived);
        }

        pendingQueue.enqueue(new PendingEntity(sourceId, sourceType, kind, null, "unresolved"));
        return EntityResolveOutcome.pending(sourceId, kind);
    }

    public void registerAlias(String sourceType, EntityKind kind, String sourceId, String canonicalId) {
        aliases.put(aliasKey(sourceType, kind, sourceId), canonicalId);
    }

    public Optional<String> lookupAlias(String sourceType, EntityKind kind, String sourceId) {
        return Optional.ofNullable(aliases.get(aliasKey(sourceType, kind, sourceId)));
    }

    public PendingEntityQueue pendingQueue() {
        return pendingQueue;
    }

    public static String aliasKey(String sourceType, EntityKind kind, String sourceId) {
        return sourceType + "|" + kind.name() + "|" + sourceId;
    }
}
