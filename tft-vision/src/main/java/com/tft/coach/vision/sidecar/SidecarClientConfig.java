package com.tft.coach.vision.sidecar;

/**
 * Configuration for local vision sidecar HTTP client.
 */
public record SidecarClientConfig(
        String baseUrl,
        int connectTimeoutMs,
        int readTimeoutMs
) {
    public static SidecarClientConfig defaults() {
        return new SidecarClientConfig("http://127.0.0.1:19090", 500, 2000);
    }
}
