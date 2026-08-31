package com.tft.coach.vision.sidecar;

/**
 * Configuration for local vision sidecar HTTP client.
 */
public record SidecarClientConfig(
        String baseUrl,
        int connectTimeoutMs,
        int readTimeoutMs,
        int analyzeTimeoutMs
) {
    public SidecarClientConfig(String baseUrl, int connectTimeoutMs, int readTimeoutMs) {
        this(baseUrl, connectTimeoutMs, readTimeoutMs, 15_000);
    }

    public static SidecarClientConfig defaults() {
        return new SidecarClientConfig("http://127.0.0.1:19090", 500, 2000, 15_000);
    }
}
