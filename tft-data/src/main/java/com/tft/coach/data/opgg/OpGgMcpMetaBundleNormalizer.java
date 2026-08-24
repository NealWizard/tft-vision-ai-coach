package com.tft.coach.data.opgg;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tft.coach.data.spi.AdapterFetchException;
import com.tft.coach.data.spi.FetchRequest;

import java.time.Instant;
import java.util.Locale;

/**
 * Converts raw OP.GG MCP tool payloads into the normalized meta-bundle JSON consumed by
 * {@link OpGgMetaSnapshotParser}.
 */
public final class OpGgMcpMetaBundleNormalizer {

    private final ObjectMapper mapper = new ObjectMapper();

    public byte[] normalize(byte[] rawMcpJson, FetchRequest request) throws AdapterFetchException {
        try {
            JsonNode root = mapper.readTree(rawMcpJson);
            if (root.has("source_id") && root.has("comps")) {
                return rawMcpJson;
            }
            return mapper.writeValueAsBytes(toBundle(root, request));
        } catch (AdapterFetchException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AdapterFetchException("Failed to normalize OP.GG MCP meta decks", ex);
        }
    }

    private ObjectNode toBundle(JsonNode root, FetchRequest request) throws AdapterFetchException {
        JsonNode decks = findDecksNode(root);
        ObjectNode bundle = mapper.createObjectNode();
        bundle.put("source_id", OpGgMcpStatsAdapter.ADAPTER_ID);
        bundle.put("patch", blankToDefault(request.patch(), "current"));
        bundle.put(
                "region",
                request.params().getOrDefault(OpGgStatsAdapter.PARAM_REGION, OpGgStatsAdapter.DEFAULT_REGION));
        bundle.put(
                "time_window",
                request.params().getOrDefault(
                        OpGgStatsAdapter.PARAM_TIME_WINDOW, OpGgStatsAdapter.DEFAULT_TIME_WINDOW));
        bundle.put("captured_at", Instant.now().toString());

        ArrayNode comps = mapper.createArrayNode();
        long totalSample = 0;
        for (JsonNode deck : decks) {
            ObjectNode comp = mapper.createObjectNode();
            String name = text(deck, "name", "unknown");
            comp.put("comp_id", slug(name));
            comp.put("name", name);
            comp.put("tier", text(deck, "tier", "?"));
            comp.put("avg_place", number(deck, "avg_place", "avgPlace"));
            comp.put("first_rate", number(deck, "first_rate", "firstRate", "win_rate", "winRate"));
            comp.put("top4_rate", number(deck, "top4_rate", "top4Rate"));
            comp.put("pick_rate", number(deck, "pick_rate", "pickRate"));
            long sampleSize = (long) number(deck, "sample_size", "sampleSize");
            comp.put("sample_size", sampleSize);
            totalSample += sampleSize;
            comp.set("core_units", readStringArray(deck.path("core_units").isMissingNode()
                    ? deck.path("coreUnits")
                    : deck.path("core_units")));
            comps.add(comp);
        }
        bundle.set("comps", comps);
        bundle.put("sample_size", totalSample);
        bundle.set("units", mapper.createArrayNode());
        bundle.set("items", mapper.createArrayNode());
        bundle.set("augments", mapper.createArrayNode());
        return bundle;
    }

    private JsonNode findDecksNode(JsonNode root) throws AdapterFetchException {
        if (root.has("decks") && root.get("decks").isArray()) {
            return root.get("decks");
        }
        if (root.path("data").has("decks") && root.path("data").get("decks").isArray()) {
            return root.path("data").get("decks");
        }
        if (root.isArray()) {
            return root;
        }
        throw new AdapterFetchException("Unrecognized OP.GG MCP meta decks payload");
    }

    private ArrayNode readStringArray(JsonNode node) {
        ArrayNode values = mapper.createArrayNode();
        if (node.isArray()) {
            node.forEach(item -> values.add(item.asText()));
        }
        return values;
    }

    private static String text(JsonNode node, String field, String fallback) {
        String value = node.path(field).asText(null);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static double number(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.path(field);
            if (!value.isMissingNode() && value.isNumber()) {
                return value.asDouble();
            }
        }
        return 0.0;
    }

    private static String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String slug(String value) {
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }
}
