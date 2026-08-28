package com.tft.coach.vision.sidecar;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Sidecar API envelope (snake_case JSON).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SidecarEnvelope(
        @JsonProperty("request_id") String requestId,
        String status,
        @JsonProperty("error_code") String errorCode,
        Object data,
        SidecarMeta meta
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SidecarMeta(
            @JsonProperty("service_version") String serviceVersion,
            @JsonProperty("latency_ms") Long latencyMs
    ) {
    }
}
