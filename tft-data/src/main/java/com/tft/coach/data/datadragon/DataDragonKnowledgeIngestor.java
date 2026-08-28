package com.tft.coach.data.datadragon;

import com.tft.coach.data.entity.CanonicalEntityResolver;
import com.tft.coach.data.normalize.KnowledgeNormalizer;
import com.tft.coach.data.normalize.NormalizedEntity;
import com.tft.coach.data.spi.AdapterFetchException;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Fetches Data Dragon static JSON and normalizes into CanonicalKnowledgeStore. */
public final class DataDragonKnowledgeIngestor {

    private final DataDragonFetchService fetchService;
    private final DataDragonVersionResolver versionResolver;
    private final KnowledgeNormalizer normalizer;
    private final CanonicalEntityResolver resolver;

    public DataDragonKnowledgeIngestor(
            DataDragonFetchService fetchService,
            DataDragonVersionResolver versionResolver,
            KnowledgeNormalizer normalizer,
            CanonicalEntityResolver resolver
    ) {
        this.fetchService = Objects.requireNonNull(fetchService);
        this.versionResolver = Objects.requireNonNull(versionResolver);
        this.normalizer = Objects.requireNonNull(normalizer);
        this.resolver = Objects.requireNonNull(resolver);
    }

    public IngestReport ingest(String patchId, String locale) throws AdapterFetchException, java.io.IOException {
        String hint = PatchVersionHints.toDragonHint(patchId);
        String version = versionResolver.resolve(hint);
        List<NormalizedEntity> all = new ArrayList<>();
        for (DataDragonResource resource : DataDragonResource.values()) {
            var outcome = fetchService.fetch(resource, version, locale);
            List<NormalizedEntity> entities = switch (resource) {
                case CHAMPION -> normalizer.ingestChampions(outcome.result().body(), patchId, resolver);
                case TRAIT -> normalizer.ingestTraits(outcome.result().body(), patchId, resolver);
                case ITEM -> normalizer.ingestItems(outcome.result().body(), patchId, resolver);
                case AUGMENT -> normalizer.ingestAugments(outcome.result().body(), patchId, resolver);
            };
            all.addAll(entities);
        }
        return new IngestReport(patchId, version, all.size());
    }

    public static DataDragonKnowledgeIngestor createDefault(
            Path snapshotRoot,
            KnowledgeNormalizer normalizer,
            CanonicalEntityResolver resolver
    ) {
        DataDragonHttpClient http = new JdkDataDragonHttpClient();
        return new DataDragonKnowledgeIngestor(
                DataDragonFetchService.createDefault(snapshotRoot, http),
                new DataDragonVersionResolver(http),
                normalizer,
                resolver);
    }

    public record IngestReport(String patchId, String dragonVersion, int entityCount) {}
}
