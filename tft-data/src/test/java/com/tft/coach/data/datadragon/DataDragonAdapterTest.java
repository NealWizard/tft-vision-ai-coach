package com.tft.coach.data.datadragon;

import com.tft.coach.data.evidence.FetchEvidence;
import com.tft.coach.data.fetch.SourceFetchService;
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

class DataDragonAdapterTest {

    @TempDir
    Path tempDir;

    private byte[] championJson;
    private byte[] versionsJson;
    private StubHttpClient httpClient;

    @BeforeEach
    void setUp() throws Exception {
        championJson = readResource("datadragon/tft-champion-sample.json");
        versionsJson = readResource("datadragon/versions-sample.json");
        httpClient = new StubHttpClient();
    }

    private static byte[] readResource(String path) throws Exception {
        try (var in = DataDragonAdapterTest.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Missing test resource: " + path);
            }
            return in.readAllBytes();
        }
    }

    @Test
    void fetchByPatchReturnsLivePayload() throws Exception {
        DataDragonAdapter adapter = new DataDragonAdapter(httpClient);
        FetchRequest request = new FetchRequest(
                SourceType.RIOT,
                DataDragonAdapter.ADAPTER_ID,
                DataDragonResource.CHAMPION.resourceKey(),
                DataDragonUrls.dataJson("16.16.1", "en_US", DataDragonResource.CHAMPION),
                "16.16.1",
                Map.of(DataDragonAdapter.PARAM_LOCALE, "en_US")
        );

        var payload = adapter.fetch(request);

        assertEquals("16.16.1", payload.patch());
        assertTrue(new String(payload.body()).contains("TFT17_Ahri"));
    }

    @Test
    void fetchServiceDegradesToSnapshotOnFailure() throws Exception {
        FileSystemRawSnapshotStore store = new FileSystemRawSnapshotStore(tempDir);
        DataDragonAdapter adapter = new DataDragonAdapter(httpClient);
        SourceFetchService service = new SourceFetchService(
                new SourceAdapterRegistry(List.of(adapter)), store);

        FetchRequest request = new FetchRequest(
                SourceType.RIOT,
                DataDragonAdapter.ADAPTER_ID,
                DataDragonResource.CHAMPION.resourceKey(),
                DataDragonUrls.dataJson("16.16.1", "en_US", DataDragonResource.CHAMPION),
                "16.16.1",
                Map.of()
        );
        service.fetch(request);

        httpClient.failNext = true;
        var result = service.fetch(request);

        assertFalse(result.live());
        assertTrue(result.degraded());
        assertTrue(new String(result.body()).contains("TFT17_Ahri"));
    }

    @Test
    void adapterSupportsAllDataDragonResources() {
        DataDragonAdapter adapter = new DataDragonAdapter(httpClient);
        for (DataDragonResource resource : DataDragonResource.values()) {
            FetchRequest request = new FetchRequest(
                    SourceType.RIOT,
                    DataDragonAdapter.ADAPTER_ID,
                    resource.resourceKey(),
                    DataDragonUrls.dataJson("16.16.1", "en_US", resource),
                    "16.16.1",
                    Map.of()
            );
            assertTrue(adapter.supports(request), resource.name());
        }
    }

    @Test
    void fetchServiceProducesEvidenceMetadata() throws Exception {
        DataDragonFetchService ddragon = DataDragonFetchService.createDefault(tempDir, httpClient);
        var outcome = ddragon.fetch(DataDragonResource.CHAMPION, "16.16.1", "en_US");

        FetchEvidence evidence = outcome.evidence();
        assertEquals(FetchEvidence.SCHEMA_VERSION, evidence.schemaVersion());
        assertEquals(SourceType.RIOT, evidence.sourceType());
        assertEquals(DataDragonAdapter.ADAPTER_ID, evidence.sourceId());
        assertEquals("16.16.1", evidence.patch());
        assertEquals(outcome.result().payloadRef(), evidence.payloadRef());
    }

    private final class StubHttpClient implements DataDragonHttpClient {
        private boolean failNext;

        @Override
        public byte[] getBytes(String url) throws AdapterFetchException {
            if (failNext) {
                failNext = false;
                throw new AdapterFetchException("simulated network failure");
            }
            if (url.contains("tft-champion.json")) {
                return championJson;
            }
            throw new AdapterFetchException("Unexpected URL: " + url);
        }

        @Override
        public String getVersionsJson() throws AdapterFetchException {
            return new String(versionsJson);
        }
    }
}
