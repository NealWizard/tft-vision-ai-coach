package com.tft.coach.data.conflict;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** In-memory conflict queue (`P1-DATA-Conflict-001`). */
public final class InMemoryConflictQueue implements ConflictQueue {

    private final List<ConflictRecord> records = new ArrayList<>();

    @Override
    public synchronized void enqueue(ConflictRecord record) {
        records.add(record);
    }

    @Override
    public synchronized List<ConflictRecord> snapshot() {
        return Collections.unmodifiableList(List.copyOf(records));
    }

    @Override
    public synchronized int size() {
        return records.size();
    }
}
