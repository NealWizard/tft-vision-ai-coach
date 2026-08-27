package com.tft.coach.data.normalize;

import com.tft.coach.data.entity.EntityKind;

import java.util.List;
import java.util.Optional;

/** Canonical entity index keyed by patch + canonical id (`P1-DATA-Normalize-001`). */
public interface CanonicalKnowledgeStore {

    void put(NormalizedEntity entity);

    Optional<NormalizedEntity> get(String patch, String canonicalId);

    List<NormalizedEntity> findByKind(String patch, EntityKind kind);

    List<NormalizedEntity> searchByName(String patch, EntityKind kind, String query);

    int size();
}
