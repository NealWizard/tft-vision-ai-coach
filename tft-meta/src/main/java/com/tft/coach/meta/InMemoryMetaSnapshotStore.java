package com.tft.coach.meta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Append-only in-memory store. Existing ids are never updated. */
public final class InMemoryMetaSnapshotStore implements MetaSnapshotStore {

    private final Map<String, StoredMetaSnapshot> byId = new ConcurrentHashMap<>();

    @Override
    public StoredMetaSnapshot save(StoredMetaSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        String id = snapshot.id();
        if (id == null || id.isBlank()) {
            id = "meta-" + UUID.randomUUID();
            snapshot = new StoredMetaSnapshot(id, snapshot.snapshot());
        }
        if (byId.putIfAbsent(id, snapshot) != null) {
            throw new IllegalStateException("Refusing to overwrite meta snapshot: " + id);
        }
        return snapshot;
    }

    @Override
    public Optional<StoredMetaSnapshot> findById(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public Optional<StoredMetaSnapshot> findLatest(MetaQuery query) {
        return byId.values().stream()
                .filter(s -> matches(s, query))
                .max(Comparator.comparing(s -> s.snapshot().capturedAt()));
    }

    @Override
    public List<StoredMetaSnapshot> findAll(String patch, String region) {
        List<StoredMetaSnapshot> out = new ArrayList<>();
        for (StoredMetaSnapshot snapshot : byId.values()) {
            if (patch.equals(snapshot.snapshot().patch())
                    && region.equals(snapshot.snapshot().region())) {
                out.add(snapshot);
            }
        }
        out.sort(Comparator.comparing(s -> s.snapshot().capturedAt()));
        return List.copyOf(out);
    }

    private static boolean matches(StoredMetaSnapshot stored, MetaQuery query) {
        var snap = stored.snapshot();
        return query.patch().equals(snap.patch())
                && query.region().equals(snap.region())
                && query.timeWindow().equals(snap.timeWindow());
    }
}
