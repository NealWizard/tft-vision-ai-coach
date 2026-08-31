package com.tft.coach.vision.sidecar;

/**
 * Result of POST /vision/analyze (always returned; check {@link #degraded()}).
 */
public record SidecarAnalyzeResult(
        boolean degraded,
        String status,
        String errorCode,
        String field,
        String rawValue,
        Object value,
        Double confidence,
        String provider,
        String model,
        String detail
) {
    public static SidecarAnalyzeResult fromEnvelope(SidecarEnvelope envelope) {
        Object dataObj = envelope.data();
        @SuppressWarnings("unchecked")
        var data = dataObj instanceof java.util.Map<?, ?> m
                ? (java.util.Map<String, Object>) m
                : java.util.Map.<String, Object>of();
        String status = envelope.status() == null ? "OK" : envelope.status();
        boolean degraded = !"OK".equalsIgnoreCase(status) || envelope.errorCode() != null;
        return new SidecarAnalyzeResult(
                degraded,
                status,
                envelope.errorCode(),
                asString(data.get("field")),
                asString(data.get("raw_value")),
                data.get("value"),
                asDouble(data.get("confidence")),
                asString(data.get("provider")),
                asString(data.get("model")),
                degraded ? "envelope status=" + status : null
        );
    }

    public static SidecarAnalyzeResult degraded(String errorCode, String detail) {
        return new SidecarAnalyzeResult(
                true, "DEGRADED", errorCode, null, null, null, 0.0, null, null, detail
        );
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static Double asDouble(Object value) {
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
