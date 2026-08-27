package com.tft.coach.vision.sidecar;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SidecarClientTest {

    @Test
    void downSidecarReturnsDegradedQuickly() {
        SidecarClient client = new SidecarClient(
                new SidecarClientConfig("http://127.0.0.1:1", 200, 500)
        );
        SidecarHealthResult result = client.health();
        assertTrue(result.degraded());
        assertEquals("DEGRADED", result.status());
        assertEquals("INTERNAL_ERROR", result.errorCode());
    }

    @Test
    void envelopeWithErrorCodeIsDegraded() {
        SidecarEnvelope envelope = new SidecarEnvelope(
                "req-1",
                "OK",
                "MODEL_NOT_READY",
                Map.of("ocr_ready", false),
                new SidecarEnvelope.SidecarMeta("0.1.0", 1L)
        );
        SidecarHealthResult result = SidecarHealthResult.fromEnvelope(envelope, Map.of("ocr_ready", false), 1L);
        assertTrue(result.degraded());
        assertEquals("MODEL_NOT_READY", result.errorCode());
    }

    @Test
    void okEnvelopeIsNotDegraded() {
        SidecarEnvelope envelope = new SidecarEnvelope(
                "req-1",
                "OK",
                null,
                Map.of("ocr_ready", false, "ready", true),
                new SidecarEnvelope.SidecarMeta("0.1.0", 1L)
        );
        SidecarHealthResult result = SidecarHealthResult.fromEnvelope(
                envelope, Map.of("ocr_ready", false, "ready", true), 2L);
        assertFalse(result.degraded());
        assertEquals("OK", result.status());
    }
}
