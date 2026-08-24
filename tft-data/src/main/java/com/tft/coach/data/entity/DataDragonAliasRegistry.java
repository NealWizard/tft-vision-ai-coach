package com.tft.coach.data.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tft.coach.data.datadragon.DataDragonAdapter;
import com.tft.coach.data.datadragon.DataDragonResource;
import com.tft.coach.data.spi.AdapterFetchException;

/**
 * Seeds {@link CanonicalEntityResolver} aliases from Data Dragon static JSON payloads.
 */
public final class DataDragonAliasRegistry {

    private static final String SOURCE_TYPE = DataDragonAdapter.ADAPTER_ID;
    private final ObjectMapper mapper = new ObjectMapper();

    public void registerChampions(CanonicalEntityResolver resolver, byte[] championJson)
            throws AdapterFetchException {
        registerNamedEntities(resolver, championJson, EntityKind.CHAMP);
    }

    public void registerTraits(CanonicalEntityResolver resolver, byte[] traitJson)
            throws AdapterFetchException {
        registerNamedEntities(resolver, traitJson, EntityKind.TRAIT);
    }

    public void registerItems(CanonicalEntityResolver resolver, byte[] itemJson)
            throws AdapterFetchException {
        registerNamedEntities(resolver, itemJson, EntityKind.ITEM);
    }

    public void registerAugments(CanonicalEntityResolver resolver, byte[] augmentJson)
            throws AdapterFetchException {
        registerNamedEntities(resolver, augmentJson, EntityKind.AUGMENT);
    }

    public void registerBundle(
            CanonicalEntityResolver resolver,
            DataDragonResource resource,
            byte[] payload
    ) throws AdapterFetchException {
        switch (resource) {
            case CHAMPION -> registerChampions(resolver, payload);
            case TRAIT -> registerTraits(resolver, payload);
            case ITEM -> registerItems(resolver, payload);
            case AUGMENT -> registerAugments(resolver, payload);
        }
    }

    private void registerNamedEntities(
            CanonicalEntityResolver resolver,
            byte[] payload,
            EntityKind kind
    ) throws AdapterFetchException {
        try {
            JsonNode data = mapper.readTree(payload).path("data");
            if (!data.isObject()) {
                throw new AdapterFetchException("Data Dragon payload missing data object for " + kind);
            }
            data.fields().forEachRemaining(entry -> {
                String sourceId = entry.getKey();
                String name = entry.getValue().path("name").asText(null);
                String canonical = name == null || name.isBlank()
                        ? CanonicalIdSlugs.fromSourceId(kind, sourceId)
                        : CanonicalIdSlugs.fromDisplayName(kind, name);
                if (canonical != null) {
                    resolver.registerAlias(SOURCE_TYPE, kind, sourceId, canonical);
                }
            });
        } catch (AdapterFetchException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AdapterFetchException("Failed to register Data Dragon aliases for " + kind, ex);
        }
    }
}
