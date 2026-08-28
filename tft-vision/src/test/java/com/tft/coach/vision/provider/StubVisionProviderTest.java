package com.tft.coach.vision.provider;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class StubVisionProviderTest {

    @Test
    void stubReportsModelNotReady() {
        VisionProvider provider = new StubVisionProvider();
        assertEquals("stub", provider.name());
        assertFalse(Boolean.TRUE.equals(provider.capabilities().get("ocr")));
        Map<String, Object> result = provider.analyze(Map.of("field", "player.gold"));
        assertEquals("MODEL_NOT_READY", result.get("error_code"));
    }
}
