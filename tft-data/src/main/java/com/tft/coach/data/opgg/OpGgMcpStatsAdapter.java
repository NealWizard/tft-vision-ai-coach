package com.tft.coach.data.opgg;

import com.tft.coach.data.spi.AdapterFetchException;
import com.tft.coach.data.spi.AdapterFetchPayload;
import com.tft.coach.data.spi.FetchRequest;
import com.tft.coach.data.spi.SourceAdapter;
import com.tft.coach.data.spi.SourceType;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Captures current TFT meta decks through the official OP.GG MCP endpoint.
 * Normalization into Canonical Meta DTO is handled by the downstream normalizer.
 */
public final class OpGgMcpStatsAdapter implements SourceAdapter {

    public static final String ADAPTER_ID = "opgg";
    public static final String RESOURCE_KEY = "meta-decks";
    public static final String TOOL_NAME = "tft_list_meta_decks";

    private final OpGgMcpClient client;

    public OpGgMcpStatsAdapter(OpGgMcpClient client) {
        this.client = Objects.requireNonNull(client, "client");
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
                && RESOURCE_KEY.equals(request.resourceKey());
    }

    @Override
    public AdapterFetchPayload fetch(FetchRequest request) throws AdapterFetchException {
        byte[] body = client.callTool(TOOL_NAME, Map.of());
        String patch = request.patch() == null || request.patch().isBlank()
                ? "current"
                : request.patch();
        return new AdapterFetchPayload(body, "application/json", Instant.now(), patch);
    }
}
