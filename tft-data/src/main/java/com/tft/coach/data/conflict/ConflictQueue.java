package com.tft.coach.data.conflict;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Queue for unresolved multi-source conflicts (`P1-DATA-Conflict-001`). */
public final class ConflictQueue {

    private final List<ConflictRecord> records = new ArrayList<>();

    public synchronized void enqueue(ConflictRecord record) {
        records.add(record);
    }

    public synchronized List<ConflictRecord> snapshot() {
        return Collections.unmodifiableList(List.copyOf(records));
    }

    public synchronized int size() {
        return records.size();
    }
}
