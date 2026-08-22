package com.tft.coach.data.snapshot;

import com.tft.coach.data.spi.AdapterFetchPayload;
import com.tft.coach.data.spi.FetchRequest;
import com.tft.coach.data.spi.SourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileSystemRawSnapshotStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void appendOnlyNeverOverwritesHistory() throws Exception {
        FileSystemRawSnapshotStore store = new FileSystemRawSnapshotStore(tempDir);
        FetchRequest request = new FetchRequest(
                SourceType.STATS,
                "meta-source-a",
                "comps",
                "https://stats.example/comps",
                "14.23",
                Map.of()
        );

        RawSnapshot first = store.append(request, payload("v1"));
        RawSnapshot second = store.append(request, payload("v2"));

        assertNotEquals(first.snapshotId(), second.snapshotId());

        List<RawSnapshot> all = store.findByQuery(new RawSnapshotQuery(
                SourceType.STATS, "meta-source-a", "comps"));
        assertEquals(2, all.size());
        assertEquals("v1", new String(store.readBody(all.get(0))));
        assertEquals("v2", new String(store.readBody(all.get(1))));
        assertTrue(second.storedAt().isAfter(first.storedAt()));
        assertEquals(64, second.checksumSha256().length());
        assertEquals("stats/meta-source-a/comps/" + second.snapshotId(), second.payloadRef());
        assertEquals("v2", new String(store.readBody(store.findLatest(
                new RawSnapshotQuery(SourceType.STATS, "meta-source-a", "comps")).orElseThrow())));
    }

    private static AdapterFetchPayload payload(String value) {
        return new AdapterFetchPayload(
                value.getBytes(),
                "application/json",
                Instant.parse("2026-08-22T08:00:00Z"),
                "14.23"
        );
    }
}
