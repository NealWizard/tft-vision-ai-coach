package com.tft.coach.data.evidence;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** In-memory evidence store; production can swap to MySQL (`P1-DATA-Evidence-001`). */
public final class EvidenceStore {

    private final Map<String, EvidenceRecord> byId = new HashMap<>();

    public void save(EvidenceRecord record) {
        byId.put(record.id(), record);
    }

    public Optional<EvidenceRecord> findById(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    public int size() {
        return byId.size();
    }
}
