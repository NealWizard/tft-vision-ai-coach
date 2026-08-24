package com.tft.coach.data.normalize;

import com.tft.coach.data.entity.CanonicalEntityResolver;
import com.tft.coach.data.entity.DataDragonAliasRegistry;
import com.tft.coach.data.spi.AdapterFetchException;

import java.util.List;
import java.util.Objects;

/** Facade for multi-source normalization into {@link CanonicalKnowledgeStore}. */
public final class KnowledgeNormalizer {

    private final CanonicalKnowledgeStore store;
    private final DataDragonNormalizer dataDragonNormalizer;

    public KnowledgeNormalizer() {
        this(new CanonicalKnowledgeStore(), new DataDragonNormalizer());
    }

    public KnowledgeNormalizer(CanonicalKnowledgeStore store, DataDragonNormalizer dataDragonNormalizer) {
        this.store = Objects.requireNonNull(store, "store");
        this.dataDragonNormalizer = Objects.requireNonNull(dataDragonNormalizer, "dataDragonNormalizer");
    }

    public CanonicalKnowledgeStore store() {
        return store;
    }

    public List<NormalizedEntity> ingestChampions(
            byte[] payload,
            String patch,
            CanonicalEntityResolver resolver
    ) throws AdapterFetchException {
        new DataDragonAliasRegistry().registerChampions(resolver, payload);
        List<NormalizedEntity> entities = dataDragonNormalizer.normalizeChampions(payload, patch, resolver);
        entities.forEach(store::put);
        return entities;
    }

    public NormalizedEntity requireEntity(String patch, String canonicalId) {
        return store.get(patch, canonicalId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown canonical entity: " + canonicalId + " @ " + patch));
    }
}
