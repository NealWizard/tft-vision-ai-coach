package com.tft.coach.data.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** In-memory queue for entities that cannot be mapped to a canonical ID. */
public final class PendingEntityQueue {

    private final List<PendingEntity> entries = new ArrayList<>();

    public synchronized void enqueue(PendingEntity entity) {
        boolean exists = entries.stream().anyMatch(existing ->
                existing.sourceType().equals(entity.sourceType())
                        && existing.kind() == entity.kind()
                        && existing.sourceId().equals(entity.sourceId()));
        if (!exists) {
            entries.add(entity);
        }
    }

    public synchronized List<PendingEntity> snapshot() {
        return Collections.unmodifiableList(List.copyOf(entries));
    }

    public synchronized int size() {
        return entries.size();
    }
}
