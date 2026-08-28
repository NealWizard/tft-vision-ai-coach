package com.tft.coach.vision.sidecar;

import java.util.Map;
import java.util.Objects;

/**
 * Health probe result. Always usable even when sidecar is down (degraded=true).
 */
public record SidecarHealthResult(
        boolean degraded,
        String status,
        String errorCode,
        String serviceVersion,
        Boolean ocrReady,
        Boolean ready,
        Long latencyMs,
        String detail
) {
    public static SidecarHealthResult ok(SidecarEnvelope envelope, Map<String, Object> data, long latencyMs) {
        Objects.requireNonNull(envelope, "envelope");
        Boolean ocrReady = data == null ? null : asBoolean(data.get("ocr_ready"));
        Boolean ready = data == null ? null : asBoolean(data.get("ready"));
        String version = envelope.meta() == null ? null : envelope.meta().serviceVersion();
        return new SidecarHealthResult(
                false,
                envelope.status() == null ? "OK" : envelope.status(),
                envelope.errorCode(),
                version,
                ocrReady,
                ready,
                latencyMs,
                null
        );
    }

    public static SidecarHealthResult degraded(String errorCode, String detail) {
        return new SidecarHealthResult(true, "DEGRADED", errorCode, null, false, false, null, detail);
    }

    private static Boolean asBoolean(Object value) {
        if (value instanceof Boolean b) {
            return b;
        }
        if (value == null) {
            return null;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }
}
