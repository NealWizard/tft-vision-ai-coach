package com.tft.coach.data.normalize;

import com.tft.coach.data.entity.EntityKind;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** In-memory canonical entity index keyed by patch + canonical id. */
public final class CanonicalKnowledgeStore {

    private final Map<String, NormalizedEntity> entities = new HashMap<>();

    public void put(NormalizedEntity entity) {
        entities.put(key(entity.patch(), entity.canonicalId()), entity);
    }

    public Optional<NormalizedEntity> get(String patch, String canonicalId) {
        return Optional.ofNullable(entities.get(key(patch, canonicalId)));
    }

    public List<NormalizedEntity> findByKind(String patch, EntityKind kind) {
        List<NormalizedEntity> matches = new ArrayList<>();
        for (NormalizedEntity entity : entities.values()) {
            if (entity.patch().equals(patch) && entity.kind() == kind) {
                matches.add(entity);
            }
        }
        return List.copyOf(matches);
    }

    public List<NormalizedEntity> searchByName(String patch, EntityKind kind, String query) {
        String needle = query.toLowerCase(Locale.ROOT);
        return findByKind(patch, kind).stream()
                .filter(entity -> entity.canonical().getOrDefault("name", "")
                        .toString()
                        .toLowerCase(Locale.ROOT)
                        .contains(needle))
                .toList();
    }

    public int size() {
        return entities.size();
    }

    private static String key(String patch, String canonicalId) {
        return patch + "|" + canonicalId;
    }
}
