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
                "set17-16.16",
                "set17",
                Instant.parse("2026-08-01T00:00:00Z"),
                null,
                PatchStatus.CURRENT,
                Duration.ofDays(7)));

        assertEquals("set17-16.16", manager.require("set17-16.16").id());
        assertThrows(PatchRequiredException.class, () -> manager.require(null));
    }
}
