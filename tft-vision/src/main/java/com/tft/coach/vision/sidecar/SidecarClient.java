package com.tft.coach.vision.sidecar;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * Minimal HTTP client for vision-sidecar. Failures return degraded results.
 */
public final class SidecarClient {

    private final SidecarClientConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper mapper;

    public SidecarClient(SidecarClientConfig config) {
        this.config = Objects.requireNonNull(config, "config");
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(config.connectTimeoutMs()))
                .build();
        this.mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    public SidecarHealthResult health() {
        return getProbe("/health");
    }

    public SidecarHealthResult ready() {
        return getProbe("/ready");
    }

    private SidecarHealthResult getProbe(String path) {
        long started = System.nanoTime();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(trimSlash(config.baseUrl()) + path))
                    .timeout(Duration.ofMillis(config.readTimeoutMs()))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long latencyMs = (System.nanoTime() - started) / 1_000_000L;
            if (response.statusCode() >= 500) {
                return SidecarHealthResult.degraded("INTERNAL_ERROR",
                        "sidecar HTTP " + response.statusCode());
            }
            if (response.statusCode() >= 400) {
                return SidecarHealthResult.degraded("INTERNAL_ERROR",
                        "sidecar HTTP " + response.statusCode());
            }
            SidecarEnvelope envelope = mapper.readValue(response.body(), SidecarEnvelope.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> data = envelope.data() instanceof Map<?, ?> m
                    ? (Map<String, Object>) m
                    : Map.of();
            return SidecarHealthResult.fromEnvelope(envelope, data, latencyMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return SidecarHealthResult.degraded("TIMEOUT", "interrupted");
        } catch (HttpTimeoutException e) {
            return SidecarHealthResult.degraded("TIMEOUT", e.getMessage());
        } catch (ConnectException e) {
            return SidecarHealthResult.degraded("INTERNAL_ERROR", e.getMessage());
        } catch (IOException e) {
            return SidecarHealthResult.degraded(mapIoErrorCode(e), e.getMessage());
        } catch (Exception e) {
            return SidecarHealthResult.degraded("INTERNAL_ERROR", e.getMessage());
        }
    }

    static String mapIoErrorCode(IOException e) {
        Throwable cur = e;
        while (cur != null) {
            if (cur instanceof HttpTimeoutException) {
                return "TIMEOUT";
            }
            if (cur instanceof ConnectException) {
                return "INTERNAL_ERROR";
            }
            String name = cur.getClass().getSimpleName();
            if (name.contains("Timeout")) {
                return "TIMEOUT";
            }
            cur = cur.getCause();
        }
        return "INTERNAL_ERROR";
    }

    private static String trimSlash(String baseUrl) {
        if (baseUrl.endsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl;
    }
}
