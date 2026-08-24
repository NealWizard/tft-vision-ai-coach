package com.tft.coach.data.opgg;

import com.tft.coach.data.spi.AdapterFetchException;
import com.tft.coach.data.spi.FetchRequest;

import java.util.Map;
import java.util.Objects;

/** Fetches OP.GG MCP meta decks and normalizes them into meta-bundle JSON. */
public final class OpGgMcpMetaBundleAssembler {

    private final OpGgMcpClient client;
    private final OpGgMcpMetaBundleNormalizer normalizer;

    public OpGgMcpMetaBundleAssembler(OpGgMcpClient client) {
        this(client, new OpGgMcpMetaBundleNormalizer());
    }

    public OpGgMcpMetaBundleAssembler(OpGgMcpClient client, OpGgMcpMetaBundleNormalizer normalizer) {
        this.client = Objects.requireNonNull(client, "client");
        this.normalizer = Objects.requireNonNull(normalizer, "normalizer");
    }

    public byte[] fetchMetaBundle(FetchRequest request) throws AdapterFetchException {
        byte[] raw = client.callTool(OpGgMcpStatsAdapter.TOOL_NAME, Map.of());
        return normalizer.normalize(raw, request);
    }
}
