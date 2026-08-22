package com.tft.coach.data.opgg;

import com.tft.coach.data.spi.AdapterFetchException;
import com.tft.coach.data.spi.AdapterFetchPayload;
import com.tft.coach.data.spi.FetchRequest;
import com.tft.coach.data.spi.SourceAdapter;
import com.tft.coach.data.spi.SourceType;

import java.time.Instant;
import java.util.Objects;

/**
 * Fetches TFT meta statistics from OP.GG (PRD primary stats source).
 */
public class OpGgStatsAdapter implements SourceAdapter {

    public static final String ADAPTER_ID = "opgg";
    public static final String PARAM_REGION = "region";
    public static final String PARAM_TIME_WINDOW = "time_window";
    public static final String DEFAULT_REGION = "global";
    public static final String DEFAULT_TIME_WINDOW = "24h";

    private final OpGgStatsHttpClient httpClient;
    private final String baseUrl;

    public OpGgStatsAdapter(OpGgStatsHttpClient httpClient) {
        this(httpClient, OpGgStatsUrls.DEFAULT_BASE);
    }

    public OpGgStatsAdapter(OpGgStatsHttpClient httpClient, String baseUrl) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.baseUrl = baseUrl == null || baseUrl.isBlank() ? OpGgStatsUrls.DEFAULT_BASE : baseUrl;
    }

    @Override
    public SourceType sourceType() {
        return SourceType.STATS;
    }

    @Override
    public String adapterId() {
        return ADAPTER_ID;
    }

    @Override
    public boolean supports(FetchRequest request) {
        return request.sourceType() == SourceType.STATS
                && ADAPTER_ID.equals(request.sourceId())
                && isKnownResource(request.resourceKey());
    }

    @Override
    public AdapterFetchPayload fetch(FetchRequest request) throws AdapterFetchException {
        OpGgStatsResource resource = OpGgStatsResource.fromResourceKey(request.resourceKey());
        String region = request.params().getOrDefault(PARAM_REGION, DEFAULT_REGION);
        String timeWindow = request.params().getOrDefault(PARAM_TIME_WINDOW, DEFAULT_TIME_WINDOW);
        String patch = request.patch() == null ? "latest" : request.patch();
        String url = request.sourceUrl();
        if (url == null || url.isBlank()) {
            url = OpGgStatsUrls.metaBundle(baseUrl, region, patch, timeWindow);
        }
        byte[] body = httpClient.getBytes(url);
        return new AdapterFetchPayload(body, "application/json", Instant.now(), patch);
    }

    private static boolean isKnownResource(String resourceKey) {
        try {
            OpGgStatsResource.fromResourceKey(resourceKey);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
