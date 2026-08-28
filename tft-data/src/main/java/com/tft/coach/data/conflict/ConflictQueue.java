package com.tft.coach.data.conflict;

import java.util.List;

/** Queue for unresolved multi-source conflicts (`P1-DATA-Conflict-001`). */
public interface ConflictQueue {

    void enqueue(ConflictRecord record);

    List<ConflictRecord> snapshot();

    int size();
}
