package com.tft.coach.data.fetch;

import com.tft.coach.data.registry.SourceAdapterRegistry;
import com.tft.coach.data.snapshot.FileSystemRawSnapshotStore;
import com.tft.coach.data.snapshot.RawSnapshot;
import com.tft.coach.data.snapshot.RawSnapshotQuery;
import com.tft.coach.data.spi.AdapterFetchException;
import com.tft.coach.data.spi.AdapterFetchPayload;
import com.tft.coach.data.spi.FetchRequest;
import com.tft.coach.data.spi.SourceAdapter;
import com.tft.coach.data.spi.SourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SourceFetchServiceTest {

    @TempDir
    Path tempDir;

    private FileSystemRawSnapshotStore store;
    private FetchRequest request;

    @BeforeEach
    void setUp() {
        store = new FileSystemRawSnapshotStore(tempDir);
        request = new FetchRequest(
                SourceType.RIOT,
                "riot-datadragon",
                "tft-champion",
                "https://ddragon.example/tft-champion.json",
                "14.23",
                Map.of()
        );
    }

    @Test
    void liveFetchPersistsSnapshot() throws Exception {
        SourceAdapter adapter = new StubAdapter(false, "{\"ok\":true}".getBytes());
        SourceFetchService service = new SourceFetchService(
                new SourceAdapterRegistry(List.of(adapter)), store);

        FetchResult result = service.fetch(request);

        assertTrue(result.live());
        assertFalse(result.degraded());
        assertEquals(1, store.findByQuery(new RawSnapshotQuery(
                SourceType.RIOT, "riot-datadragon", "tft-champion")).size());
    }

    @Test
    void failedFetchFallsBackToLatestSnapshot() throws Exception {
        SourceAdapter ok = new StubAdapter(false, "v1".getBytes());
        SourceFetchService seed = new SourceFetchService(new SourceAdapterRegistry(List.of(ok)), store);
        seed.fetch(request);

        SourceAdapter failing = new StubAdapter(true, new byte[0]);
        SourceFetchService service = new SourceFetchService(
                new SourceAdapterRegistry(List.of(failing)), store);

        FetchResult result = service.fetch(request);

        assertFalse(result.live());
        assertTrue(result.degraded());
        assertEquals("forced-failure", result.message());
        assertEquals("v1", new String(result.body()));
        assertEquals(1, store.findByQuery(new RawSnapshotQuery(
                SourceType.RIOT, "riot-datadragon", "tft-champion")).size());
    }

    private static final class StubAdapter implements SourceAdapter {
        private final boolean fail;
        private final byte[] body;

        StubAdapter(boolean fail, byte[] body) {
            this.fail = fail;
            this.body = body;
        }

        @Override
        public SourceType sourceType() {
            return SourceType.RIOT;
        }

        @Override
        public String adapterId() {
            return "riot-datadragon";
        }

        @Override
        public boolean supports(FetchRequest request) {
            return request.sourceType() == SourceType.RIOT;
        }

        @Override
        public AdapterFetchPayload fetch(FetchRequest request) throws AdapterFetchException {
            if (fail) {
                throw new AdapterFetchException("forced-failure");
            }
            return new AdapterFetchPayload(body, "application/json", Instant.parse("2026-08-22T08:00:00Z"), "14.23");
        }
    }
}
