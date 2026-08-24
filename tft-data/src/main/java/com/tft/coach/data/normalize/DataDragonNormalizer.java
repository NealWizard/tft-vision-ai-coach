package com.tft.coach.data.normalize;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tft.coach.data.datadragon.DataDragonAdapter;
import com.tft.coach.data.entity.CanonicalEntityResolver;
import com.tft.coach.data.entity.EntityKind;
import com.tft.coach.data.entity.EntityResolveOutcome;
import com.tft.coach.data.spi.AdapterFetchException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Normalizes Data Dragon static JSON into canonical DTO maps. */
public final class DataDragonNormalizer {

    private static final String SCHEMA_VERSION = "1.0.0";
    private final ObjectMapper mapper = new ObjectMapper();

    public List<NormalizedEntity> normalizeChampions(
            byte[] payload,
            String patch,
            CanonicalEntityResolver resolver
    ) throws AdapterFetchException {
        return normalizeNamedEntities(payload, patch, resolver, EntityKind.CHAMP, "cost");
    }

    public List<NormalizedEntity> normalizeTraits(
            byte[] payload,
            String patch,
            CanonicalEntityResolver resolver
    ) throws AdapterFetchException {
        return normalizeNamedEntities(payload, patch, resolver, EntityKind.TRAIT, null);
    }

    public List<NormalizedEntity> normalizeItems(
            byte[] payload,
            String patch,
            CanonicalEntityResolver resolver
    ) throws AdapterFetchException {
        return normalizeNamedEntities(payload, patch, resolver, EntityKind.ITEM, "kind");
    }

    public List<NormalizedEntity> normalizeAugments(
            byte[] payload,
            String patch,
            CanonicalEntityResolver resolver
    ) throws AdapterFetchException {
        return normalizeNamedEntities(payload, patch, resolver, EntityKind.AUGMENT, "tier");
    }

    private List<NormalizedEntity> normalizeNamedEntities(
            byte[] payload,
            String patch,
            CanonicalEntityResolver resolver,
            EntityKind kind,
            String extraNumericField
    ) throws AdapterFetchException {
        try {
            JsonNode data = mapper.readTree(payload).path("data");
            if (!data.isObject()) {
                throw new AdapterFetchException("Data Dragon payload missing data object");
            }
            List<NormalizedEntity> entities = new ArrayList<>();
            data.fields().forEachRemaining(entry -> {
                String sourceId = entry.getKey();
                JsonNode node = entry.getValue();
                EntityResolveOutcome outcome = resolver.resolve(
                        DataDragonAdapter.ADAPTER_ID, kind, sourceId);
                if (outcome.pending()) {
                    return;
                }
                Map<String, Object> canonical = new HashMap<>();
                canonical.put("schema_version", SCHEMA_VERSION);
                canonical.put("id", outcome.canonicalId().orElseThrow());
                canonical.put("patch", patch);
                canonical.put("name", node.path("name").asText(sourceId));
                if (extraNumericField != null && "cost".equals(extraNumericField)) {
                    canonical.put("cost", node.path("cost").asInt(1));
                }
                if (extraNumericField != null && "tier".equals(extraNumericField)) {
                    canonical.put("tier", node.path("tier").asText("silver"));
                }
                if (extraNumericField != null && "kind".equals(extraNumericField)) {
                    canonical.put("kind", node.path("kind").asText("other"));
                }
                entities.add(new NormalizedEntity(
                        outcome.canonicalId().orElseThrow(),
                        kind,
                        patch,
                        Map.copyOf(canonical),
                        node.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8),
                        DataDragonAdapter.ADAPTER_ID,
                        sourceId));
            });
            return List.copyOf(entities);
        } catch (AdapterFetchException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AdapterFetchException("Failed to normalize Data Dragon payload", ex);
        }
    }
}
