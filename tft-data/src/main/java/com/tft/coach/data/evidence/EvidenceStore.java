package com.tft.coach.data.evidence;

import java.util.Optional;

/** Evidence persistence port (`P1-DATA-Evidence-001`). */
public interface EvidenceStore {

    void save(EvidenceRecord record);

    Optional<EvidenceRecord> findById(String id);

    int size();
}
