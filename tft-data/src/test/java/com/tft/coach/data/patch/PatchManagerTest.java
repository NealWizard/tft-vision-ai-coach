package com.tft.coach.data.patch;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PatchManagerTest {

    @Test
    void requiresExplicitPatchOnQuery() {
        PatchManager manager = new InMemoryPatchManager();
        manager.register(new PatchRecord(
                "set18-18.1",
                "set18",
                Instant.parse("2026-08-01T00:00:00Z"),
                null,
                PatchStatus.CURRENT,
                Duration.ofDays(7)));

        assertEquals("set18-18.1", manager.require("set18-18.1").id());
        assertThrows(PatchRequiredException.class, () -> manager.require(null));
    }
}
