package com.tft.coach.data.opgg;

import com.tft.coach.data.evidence.FetchEvidence;
import com.tft.coach.data.fetch.FetchResult;
import com.tft.coach.data.fetch.SourceFetchService;
import com.tft.coach.data.meta.MetaSnapshot;
import com.tft.coach.data.meta.MetaSnapshotParser;
import com.tft.coach.data.registry.SourceAdapterRegistry;
import com.tft.coach.data.snapshot.FileSystemRawSnapshotStore;
import com.tft.coach.data.snapshot.RawSnapshotStore;
import com.tft.coach.data.spi.AdapterFetchException;
import com.tft.coach.data.spi.FetchRequest;
import com.tft.coach.data.spi.SourceType;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

/**
 * High-level OP.GG meta fetch: snapshot persistence, degradation, and {@link MetaSnapshot} parsing.
 */
public class OpGgStatsFetchService {

    private final SourceFetchService fetchService;
    private final MetaSnapshotParser parser;

    public OpGgStatsFetchService(SourceFetchService fetchService, MetaSnapshotParser parser) {
        this.fetchService = fetchService;
        this.parser = parser;
    }

    public OpGgStatsOutcome fetchMetaBundle(String patch, String region, String timeWindow)
            throws IOException, AdapterFetchException {
        String url = OpGgStatsUrls.metaBundle(OpGgStatsUrls.DEFAULT_BASE, region, patch, timeWindow);
        FetchRequest request = new FetchRequest(
                SourceType.STATS,
                OpGgStatsAdapter.ADAPTER_ID,
                OpGgStatsResource.META_BUNDLE.resourceKey(),
                url,
                patch,
                Map.of(
                        OpGgStatsAdapter.PARAM_REGION, region,
                        OpGgStatsAdapter.PARAM_TIME_WINDOW, timeWindow
                )
        );
        FetchResult result = fetchService.fetch(request);
        MetaSnapshot snapshot = parser.parse(result.body());
        FetchEvidence evidence = FetchEvidence.fromFetchResult(result);
        return new OpGgStatsOutcome(result, snapshot, evidence);
    }

    public record OpGgStatsOutcome(FetchResult result, MetaSnapshot snapshot, FetchEvidence evidence) {}

    public static OpGgStatsFetchService createDefault(Path snapshotRoot, OpGgStatsHttpClient httpClient) {
        RawSnapshotStore store = new FileSystemRawSnapshotStore(snapshotRoot);
        SourceAdapterRegistry registry = new SourceAdapterRegistry(
                java.util.List.of(new OpGgStatsAdapter(httpClient)));
        return new OpGgStatsFetchService(
                new SourceFetchService(registry, store),
                new OpGgMetaSnapshotParser());
    }
}
