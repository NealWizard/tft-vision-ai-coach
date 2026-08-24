package com.tft.coach.data.meta;

import com.tft.coach.data.lolchess.LoLChessStatsAdapter;
import com.tft.coach.data.lolchess.LoLChessStatsHttpClient;
import com.tft.coach.data.opgg.OpGgMcpClient;
import com.tft.coach.data.opgg.OpGgMcpStatsAdapter;
import com.tft.coach.data.opgg.OpGgStatsAdapter;
import com.tft.coach.data.spi.AdapterFetchException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultiSourceMetaFetchServiceTest {

    @TempDir
    Path tempDir;

    private byte[] opggJson;
    private byte[] lolchessJson;
    private DualStatsClient dualClient;

    @BeforeEach
    void setUp() throws Exception {
        opggJson = readResource("opgg/meta-bundle-sample.json");
        lolchessJson = readResource("lolchess/meta-bundle-sample.json");
        dualClient = new DualStatsClient();
    }

    @Test
    void sameQueryReturnsBothSourcesWithSourceId() throws Exception {
        MultiSourceMetaFetchService service = MultiSourceMetaFetchService.createDefault(
                tempDir, dualClient, dualClient);

        MultiSourceMetaResult result = service.fetch(MetaSnapshotQuery.of("set17-16.16", "global", "24h"));

        assertEquals(2, result.outcomes().size());
        assertEquals(OpGgMcpStatsAdapter.ADAPTER_ID, result.outcomes().get(0).sourceId());
        assertEquals(LoLChessStatsAdapter.ADAPTER_ID, result.outcomes().get(1).sourceId());
        assertEquals("opgg", result.outcomes().get(0).snapshot().sourceId());
        assertEquals("lolchess", result.outcomes().get(1).snapshot().sourceId());
        assertEquals(OpGgMcpStatsAdapter.ADAPTER_ID, result.outcomes().get(0).evidence().sourceId());
        assertEquals(LoLChessStatsAdapter.ADAPTER_ID, result.outcomes().get(1).evidence().sourceId());
    }

    @Test
    void oneSourceDegradedDoesNotBlockTheOther() throws Exception {
        MultiSourceMetaFetchService service = MultiSourceMetaFetchService.createDefault(
                tempDir, dualClient, dualClient);

        service.fetch(MetaSnapshotQuery.of("set17-16.16", "global", "24h"));
        dualClient.failOpggNext = true;

        MultiSourceMetaResult result = service.fetch(MetaSnapshotQuery.of("set17-16.16", "global", "24h"));

        assertTrue(result.outcomes().get(0).fetchResult().degraded());
        assertTrue(result.outcomes().get(1).fetchResult().live());
    }

    private static byte[] readResource(String path) throws Exception {
        try (var in = MultiSourceMetaFetchServiceTest.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Missing test resource: " + path);
            }
            return in.readAllBytes();
        }
    }

    /** Routes OP.GG MCP and LoLChess HTTP calls to separate fixture payloads. */
    private final class DualStatsClient implements OpGgMcpClient, LoLChessStatsHttpClient {
        private boolean failOpggNext;

        @Override
        public byte[] callTool(String toolName, Map<String, Object> arguments) throws AdapterFetchException {
            if (OpGgMcpStatsAdapter.TOOL_NAME.equals(toolName)) {
                if (failOpggNext) {
                    failOpggNext = false;
                    throw new AdapterFetchException("simulated opgg mcp outage");
                }
                return opggJson;
            }
            throw new AdapterFetchException("Unexpected MCP tool: " + toolName);
        }

        @Override
        public byte[] getBytes(String url) throws AdapterFetchException {
            if (url.contains("lolchess.gg")) {
                return lolchessJson;
            }
            throw new AdapterFetchException("Unexpected URL: " + url);
        }
    }
}
