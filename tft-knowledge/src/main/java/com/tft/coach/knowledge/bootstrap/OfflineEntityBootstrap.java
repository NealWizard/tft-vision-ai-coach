package com.tft.coach.knowledge.bootstrap;

import com.tft.coach.data.entity.CanonicalEntityResolver;
import com.tft.coach.data.normalize.KnowledgeNormalizer;
import com.tft.coach.data.spi.AdapterFetchException;

import java.io.IOException;
import java.io.InputStream;

/** Seeds offline/CI platforms with fixture Data Dragon payloads (no network). */
public final class OfflineEntityBootstrap {

    private OfflineEntityBootstrap() {}

    public static void seedChampions(KnowledgeNormalizer normalizer, String patch)
            throws AdapterFetchException {
        try (InputStream in = OfflineEntityBootstrap.class.getClassLoader()
                .getResourceAsStream("knowledge/fixtures/offline-champions.json")) {
            if (in == null) {
                throw new IllegalStateException("Missing offline-champions.json");
            }
            byte[] payload = in.readAllBytes();
            CanonicalEntityResolver resolver = new CanonicalEntityResolver();
            normalizer.ingestChampions(payload, patch, resolver);
        } catch (IOException ex) {
            throw new AdapterFetchException("Failed to load offline champion fixture", ex);
        }
    }
}
