package com.tft.coach.data.opgg;

import com.tft.coach.data.spi.AdapterFetchException;
import com.tft.coach.data.spi.AdapterFetchPayload;
import com.tft.coach.data.spi.FetchRequest;
import com.tft.coach.data.spi.SourceAdapter;
import com.tft.coach.data.spi.SourceType;

import java.time.Instant;
import java.util.Objects;

/**
 * Captures current TFT meta decks through the official OP.GG MCP endpoint.
 * Normalization into Canonical Meta DTO is handled by {@link OpGgMcpMetaBundleNormalizer}.
 */
public final class OpGgMcpStatsAdapter implements SourceAdapter {

    public static final String ADAPTER_ID = "opgg";
    public static final String TOOL_NAME = "tft_list_meta_decks";

    private final OpGgMcpMetaBundleAssembler assembler;

    public OpGgMcpStatsAdapter(OpGgMcpClient client) {
        this(new OpGgMcpMetaBundleAssembler(client));
    }

    OpGgMcpStatsAdapter(OpGgMcpMetaBundleAssembler assembler) {
        this.assembler = Objects.requireNonNull(assembler, "assembler");
    }

    public static String mcpToolUrl(String toolName) {
        return OfficialOpGgMcpClient.DEFAULT_BASE_URL
                + OfficialOpGgMcpClient.DEFAULT_ENDPOINT
                + "#"
                + toolName;
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
        if (resource != OpGgStatsResource.META_BUNDLE) {
            throw new AdapterFetchException(
                    "OP.GG MCP only supports meta-bundle in P1: " + resource.resourceKey());
        }
        byte[] body = assembler.fetchMetaBundle(request);
        String patch = request.patch() == null || request.patch().isBlank()
                ? "current"
                : request.patch();
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
