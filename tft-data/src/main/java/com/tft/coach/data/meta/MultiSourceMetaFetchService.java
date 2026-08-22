package com.tft.coach.data.meta;

import com.tft.coach.data.evidence.FetchEvidence;
import com.tft.coach.data.fetch.FetchResult;
import com.tft.coach.data.fetch.SourceFetchService;
import com.tft.coach.data.lolchess.LoLChessStatsAdapter;
import com.tft.coach.data.lolchess.LoLChessStatsHttpClient;
import com.tft.coach.data.lolchess.LoLChessStatsUrls;
import com.tft.coach.data.lolchess.JdkLoLChessStatsHttpClient;
import com.tft.coach.data.opgg.JdkOpGgStatsHttpClient;
import com.tft.coach.data.opgg.OpGgMetaSnapshotParser;
import com.tft.coach.data.opgg.OpGgStatsAdapter;
import com.tft.coach.data.opgg.OpGgStatsHttpClient;
import com.tft.coach.data.opgg.OpGgStatsResource;
import com.tft.coach.data.opgg.OpGgStatsUrls;
import com.tft.coach.data.registry.SourceAdapterRegistry;
import com.tft.coach.data.snapshot.FileSystemRawSnapshotStore;
import com.tft.coach.data.snapshot.RawSnapshotStore;
import com.tft.coach.data.spi.AdapterFetchException;
import com.tft.coach.data.spi.FetchRequest;
import com.tft.coach.data.spi.SourceType;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Fetches {@link MetaSnapshot} from multiple stats sources for the same query (P1-005).
 */
public class MultiSourceMetaFetchService {

    private final SourceFetchService fetchService;
    private final MetaSnapshotParser parser;

    public MultiSourceMetaFetchService(SourceFetchService fetchService, MetaSnapshotParser parser) {
        this.fetchService = Objects.requireNonNull(fetchService, "fetchService");
        this.parser = Objects.requireNonNull(parser, "parser");
    }

    public MultiSourceMetaResult fetch(MetaSnapshotQuery query) throws IOException, AdapterFetchException {
        List<MetaSnapshotOutcome> outcomes = new ArrayList<>();
        outcomes.add(fetchOne(OpGgStatsAdapter.ADAPTER_ID, OpGgStatsUrls.metaBundle(
                OpGgStatsUrls.DEFAULT_BASE, query.region(), query.patch(), query.timeWindow()), query));
        outcomes.add(fetchOne(LoLChessStatsAdapter.ADAPTER_ID, LoLChessStatsUrls.metaBundle(
                LoLChessStatsUrls.DEFAULT_BASE, query.region(), query.patch(), query.timeWindow()), query));
        return new MultiSourceMetaResult(query, outcomes);
    }

    private MetaSnapshotOutcome fetchOne(String sourceId, String url, MetaSnapshotQuery query)
            throws IOException, AdapterFetchException {
        FetchRequest request = new FetchRequest(
                SourceType.STATS,
                sourceId,
                OpGgStatsResource.META_BUNDLE.resourceKey(),
                url,
                query.patch(),
                Map.of(
                        OpGgStatsAdapter.PARAM_REGION, query.region(),
                        OpGgStatsAdapter.PARAM_TIME_WINDOW, query.timeWindow()
                )
        );
        FetchResult result = fetchService.fetch(request);
        MetaSnapshot snapshot = parser.parse(result.body());
        FetchEvidence evidence = FetchEvidence.fromFetchResult(result);
        return new MetaSnapshotOutcome(sourceId, result, snapshot, evidence);
    }

    public static MultiSourceMetaFetchService createDefault(
            Path snapshotRoot,
            OpGgStatsHttpClient opggClient,
            LoLChessStatsHttpClient lolchessClient
    ) {
        RawSnapshotStore store = new FileSystemRawSnapshotStore(snapshotRoot);
        SourceAdapterRegistry registry = new SourceAdapterRegistry(List.of(
                new OpGgStatsAdapter(opggClient),
                new LoLChessStatsAdapter(lolchessClient)
        ));
        return new MultiSourceMetaFetchService(
                new SourceFetchService(registry, store),
                new NormalizedMetaSnapshotParser());
    }

    /**
     * Parses normalized meta bundle JSON; {@code source_id} in payload identifies vendor.
     */
    public static class NormalizedMetaSnapshotParser implements MetaSnapshotParser {
        private final OpGgMetaSnapshotParser delegate = new OpGgMetaSnapshotParser();

        @Override
        public MetaSnapshot parse(byte[] rawJson) throws AdapterFetchException {
            return delegate.parse(rawJson);
        }
    }
}
