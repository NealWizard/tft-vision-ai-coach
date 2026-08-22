package com.tft.coach.data.opgg;

import com.tft.coach.data.evidence.FetchEvidence;
import com.tft.coach.data.fetch.SourceFetchService;
import com.tft.coach.data.meta.MetaSnapshot;
import com.tft.coach.data.registry.SourceAdapterRegistry;
import com.tft.coach.data.snapshot.FileSystemRawSnapshotStore;
import com.tft.coach.data.spi.AdapterFetchException;
import com.tft.coach.data.spi.FetchRequest;
import com.tft.coach.data.spi.SourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpGgStatsAdapterTest {

    @TempDir
    Path tempDir;

    private byte[] bundleJson;
    private StubHttpClient httpClient;

    @BeforeEach
    void setUp() throws Exception {
        try (var in = OpGgStatsAdapterTest.class.getClassLoader().getResourceAsStream("opgg/meta-bundle-sample.json")) {
            bundleJson = in.readAllBytes();
        }
        httpClient = new StubHttpClient();
    }

    @Test
    void parserCoversCompUnitItemAugment() throws Exception {
        MetaSnapshot snapshot = new OpGgMetaSnapshotParser().parse(bundleJson);

        assertEquals("opgg", snapshot.sourceId());
        assertEquals(578607, snapshot.sampleSize());
        assertEquals(1, snapshot.comps().size());
        assertEquals(1, snapshot.units().size());
        assertEquals(1, snapshot.items().size());
        assertEquals(1, snapshot.augments().size());
        assertEquals("Stargazer Xayah", snapshot.comps().getFirst().name());
    }

    @Test
    void fetchServiceReturnsMetaSnapshotWithEvidence() throws Exception {
        OpGgStatsFetchService service = OpGgStatsFetchService.createDefault(tempDir, httpClient);
        var outcome = service.fetchMetaBundle("set17-16.16", "global", "24h");

        assertTrue(outcome.result().live());
        assertEquals(SourceType.STATS, outcome.evidence().sourceType());
        assertEquals(OpGgStatsAdapter.ADAPTER_ID, outcome.evidence().sourceId());
        assertEquals("24h", outcome.snapshot().timeWindow());
    }

    @Test
    void degradesToCachedSnapshotWhenLiveFails() throws Exception {
        FileSystemRawSnapshotStore store = new FileSystemRawSnapshotStore(tempDir);
        OpGgStatsAdapter adapter = new OpGgStatsAdapter(httpClient);
        SourceFetchService fetchService = new SourceFetchService(
                new SourceAdapterRegistry(List.of(adapter)), store);

        FetchRequest request = new FetchRequest(
                SourceType.STATS,
                OpGgStatsAdapter.ADAPTER_ID,
                OpGgStatsResource.META_BUNDLE.resourceKey(),
                "https://tft.op.gg/api/meta/bundle",
                "set17-16.16",
                Map.of()
        );
        fetchService.fetch(request);

        httpClient.failNext = true;
        var result = fetchService.fetch(request);

        assertFalse(result.live());
        assertTrue(result.degraded());
        MetaSnapshot snapshot = new OpGgMetaSnapshotParser().parse(result.body());
        assertEquals(578607, snapshot.sampleSize());
    }

    private final class StubHttpClient implements OpGgStatsHttpClient {
        private boolean failNext;

        @Override
        public byte[] getBytes(String url) throws AdapterFetchException {
            if (failNext) {
                failNext = false;
                throw new AdapterFetchException("simulated opgg outage");
            }
            return bundleJson;
        }
    }
}
