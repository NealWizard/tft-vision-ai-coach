package com.tft.coach.vision.provider;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Batch A placeholder provider (no real OCR).
 */
public final class StubVisionProvider implements VisionProvider {

    @Override
    public String name() {
        return "stub";
    }

    @Override
    public Map<String, Object> capabilities() {
        Map<String, Object> caps = new LinkedHashMap<>();
        caps.put("ocr", false);
        caps.put("icon", false);
        caps.put("crop", false);
        return caps;
    }

    @Override
    public Map<String, Object> analyze(Map<String, Object> request) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("provider", name());
        out.put("field", request == null ? null : request.get("field"));
        out.put("raw_value", null);
        out.put("value", null);
        out.put("confidence", 0.0);
        out.put("error_code", "MODEL_NOT_READY");
        out.put("message", "Batch A stub; real OCR in Batch B");
        return out;
    }
}
