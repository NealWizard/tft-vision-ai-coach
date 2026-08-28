package com.tft.coach.vision.sidecar;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        assertEquals("TIMEOUT", result.errorCode());
    }
}
