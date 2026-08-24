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
    private byte[] mcpRawJson;
    private StubMcpClient mcpClient;

    @BeforeEach
    void setUp() throws Exception {
        bundleJson = readResource("opgg/meta-bundle-sample.json");
        mcpRawJson = readResource("opgg/mcp-meta-decks-raw-sample.json");
        mcpClient = new StubMcpClient();
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
    void normalizerMapsMcpDeckPayloadToMetaBundle() throws Exception {
        FetchRequest request = new FetchRequest(
                SourceType.STATS,
                OpGgMcpStatsAdapter.ADAPTER_ID,
                OpGgStatsResource.META_BUNDLE.resourceKey(),
                OpGgMcpStatsAdapter.mcpToolUrl(OpGgMcpStatsAdapter.TOOL_NAME),
                "set17-16.16",
                Map.of(
                        OpGgStatsAdapter.PARAM_REGION, "global",
                        OpGgStatsAdapter.PARAM_TIME_WINDOW, "24h"
                )
        );

        byte[] normalized = new OpGgMcpMetaBundleNormalizer().normalize(mcpRawJson, request);
        MetaSnapshot snapshot = new OpGgMetaSnapshotParser().parse(normalized);

        assertEquals("opgg", snapshot.sourceId());
        assertEquals("global", snapshot.region());
        assertEquals("24h", snapshot.timeWindow());
        assertEquals(1, snapshot.comps().size());
        assertEquals("Stargazer Xayah", snapshot.comps().getFirst().name());
        assertEquals(8679, snapshot.sampleSize());
    }

    @Test
    void fetchServiceReturnsMetaSnapshotWithEvidence() throws Exception {
        OpGgStatsFetchService service = OpGgStatsFetchService.createDefault(tempDir, mcpClient);
        var outcome = service.fetchMetaBundle("set17-16.16", "global", "24h");

        assertTrue(outcome.result().live());
        assertEquals(SourceType.STATS, outcome.evidence().sourceType());
        assertEquals(OpGgMcpStatsAdapter.ADAPTER_ID, outcome.evidence().sourceId());
        assertEquals("24h", outcome.snapshot().timeWindow());
        assertEquals(
                OpGgMcpStatsAdapter.mcpToolUrl(OpGgMcpStatsAdapter.TOOL_NAME),
                outcome.evidence().sourceUrl());
    }

    @Test
    void degradesToCachedSnapshotWhenLiveFails() throws Exception {
        FileSystemRawSnapshotStore store = new FileSystemRawSnapshotStore(tempDir);
        OpGgMcpStatsAdapter adapter = new OpGgMcpStatsAdapter(mcpClient);
        SourceFetchService fetchService = new SourceFetchService(
                new SourceAdapterRegistry(List.of(adapter)), store);

        FetchRequest request = new FetchRequest(
                SourceType.STATS,
                OpGgMcpStatsAdapter.ADAPTER_ID,
                OpGgStatsResource.META_BUNDLE.resourceKey(),
                OpGgMcpStatsAdapter.mcpToolUrl(OpGgMcpStatsAdapter.TOOL_NAME),
                "set17-16.16",
                Map.of()
        );
        fetchService.fetch(request);

        mcpClient.failNext = true;
        var result = fetchService.fetch(request);

        assertFalse(result.live());
        assertTrue(result.degraded());
        MetaSnapshot snapshot = new OpGgMetaSnapshotParser().parse(result.body());
        assertEquals(578607, snapshot.sampleSize());
    }

    private static byte[] readResource(String path) throws Exception {
        try (var in = OpGgStatsAdapterTest.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Missing test resource: " + path);
            }
            return in.readAllBytes();
        }
    }

    private final class StubMcpClient implements OpGgMcpClient {
        private boolean failNext;

        @Override
        public byte[] callTool(String toolName, Map<String, Object> arguments) throws AdapterFetchException {
            if (failNext) {
                failNext = false;
                throw new AdapterFetchException("simulated opgg mcp outage");
            }
            if (OpGgMcpStatsAdapter.TOOL_NAME.equals(toolName)) {
                return bundleJson;
            }
            throw new AdapterFetchException("Unexpected MCP tool: " + toolName);
        }
    }
}
