package com.tft.coach.vision.provider;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CloudVisionFallbackTest {

    @Test
    void neverCallsCloudWhenDisabled() {
        assertFalse(CloudVisionFallback.shouldCallCloud(0.1, false));
        assertFalse(CloudVisionFallback.shouldCallCloud(0.99, false));
    }

    @Test
    void callsCloudOnlyForLowConfidenceWhenEnabled() {
        assertTrue(CloudVisionFallback.shouldCallCloud(0.5, true));
        assertFalse(CloudVisionFallback.shouldCallCloud(0.95, true));
    }
}
