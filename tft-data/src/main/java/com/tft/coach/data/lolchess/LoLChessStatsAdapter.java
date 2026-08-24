package com.tft.coach.data.lolchess;

import com.tft.coach.data.spi.AdapterFetchException;
import com.tft.coach.data.spi.AdapterFetchPayload;
import com.tft.coach.data.spi.FetchRequest;
import com.tft.coach.data.spi.SourceAdapter;
import com.tft.coach.data.spi.SourceType;

import java.time.Instant;
import java.util.Objects;

/**
 * Fetches TFT meta statistics from LoLChess.gg (PRD secondary stats source).
 */
public class LoLChessStatsAdapter implements SourceAdapter {

    public static final String ADAPTER_ID = "lolchess";
    public static final String PARAM_REGION = "region";
    public static final String PARAM_TIME_WINDOW = "time_window";
    public static final String DEFAULT_REGION = "global";
    public static final String DEFAULT_TIME_WINDOW = "24h";

    private final LoLChessStatsHttpClient httpClient;
    private final String baseUrl;

    public LoLChessStatsAdapter(LoLChessStatsHttpClient httpClient) {
        this(httpClient, LoLChessStatsUrls.DEFAULT_BASE);
    }

    public LoLChessStatsAdapter(LoLChessStatsHttpClient httpClient, String baseUrl) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.baseUrl = baseUrl == null || baseUrl.isBlank() ? LoLChessStatsUrls.DEFAULT_BASE : baseUrl;
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
                && LoLChessStatsResource.META_BUNDLE.resourceKey().equals(request.resourceKey());
    }

    @Override
    public AdapterFetchPayload fetch(FetchRequest request) throws AdapterFetchException {
        String region = request.params().getOrDefault(PARAM_REGION, DEFAULT_REGION);
        String timeWindow = request.params().getOrDefault(PARAM_TIME_WINDOW, DEFAULT_TIME_WINDOW);
        String patch = request.patch() == null ? "latest" : request.patch();
        String url = request.sourceUrl();
        if (url == null || url.isBlank()) {
            url = LoLChessStatsUrls.metaBundle(baseUrl, region, patch, timeWindow);
        }
        byte[] body = httpClient.getBytes(url);
        return new AdapterFetchPayload(body, "application/json", Instant.now(), patch);
    }
}
