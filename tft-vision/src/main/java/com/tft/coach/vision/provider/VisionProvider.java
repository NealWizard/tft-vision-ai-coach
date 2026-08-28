package com.tft.coach.vision.provider;

import java.util.Map;

/**
 * Pluggable vision backend SPI. Batch A ships {@link StubVisionProvider} only.
 */
public interface VisionProvider {

    String name();

    Map<String, Object> capabilities();

    /**
     * Analyze a request map (field / image refs). Stub returns MODEL_NOT_READY semantics.
     */
    Map<String, Object> analyze(Map<String, Object> request);
}
