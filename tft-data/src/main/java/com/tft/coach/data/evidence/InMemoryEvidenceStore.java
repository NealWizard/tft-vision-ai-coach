package com.tft.coach.data.evidence;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** In-memory evidence store (`P1-DATA-Evidence-001`). */
public final class InMemoryEvidenceStore implements EvidenceStore {

    private final Map<String, EvidenceRecord> byId = new HashMap<>();

    @Override
    public void save(EvidenceRecord record) {
        byId.put(record.id(), record);
    }

    @Override
    public Optional<EvidenceRecord> findById(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public int size() {
        return byId.size();
    }
}
