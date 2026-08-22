package com.tft.coach.data.opgg;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tft.coach.data.meta.AugmentStat;
import com.tft.coach.data.meta.CompStat;
import com.tft.coach.data.meta.ItemStat;
import com.tft.coach.data.meta.MetaSnapshot;
import com.tft.coach.data.meta.MetaSnapshotParser;
import com.tft.coach.data.meta.UnitStat;
import com.tft.coach.data.spi.AdapterFetchException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses normalized OP.GG meta JSON into {@link MetaSnapshot}.
 */
public class OpGgMetaSnapshotParser implements MetaSnapshotParser {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public MetaSnapshot parse(byte[] rawJson) throws AdapterFetchException {
        try {
            JsonNode root = mapper.readTree(rawJson);
            Instant capturedAt = Instant.parse(root.path("captured_at").asText());
            return new MetaSnapshot(
                    root.path("source_id").asText("opgg"),
                    root.path("patch").asText(null),
                    root.path("region").asText("global"),
                    root.path("time_window").asText("24h"),
                    capturedAt,
                    root.path("sample_size").asLong(0),
                    parseComps(root.path("comps")),
                    parseUnits(root.path("units")),
                    parseItems(root.path("items")),
                    parseAugments(root.path("augments"))
            );
        } catch (Exception ex) {
            throw new AdapterFetchException("Failed to parse OP.GG meta JSON", ex);
        }
    }

    private List<CompStat> parseComps(JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }
        List<CompStat> comps = new ArrayList<>();
        for (JsonNode item : node) {
            comps.add(new CompStat(
                    item.path("comp_id").asText(),
                    item.path("name").asText(),
                    item.path("tier").asText(),
                    item.path("avg_place").asDouble(),
                    item.path("first_rate").asDouble(),
                    item.path("top4_rate").asDouble(),
                    item.path("pick_rate").asDouble(),
                    item.path("sample_size").asLong(),
                    readStringList(item.path("core_units"))
            ));
        }
        return List.copyOf(comps);
    }

    private List<UnitStat> parseUnits(JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }
        List<UnitStat> units = new ArrayList<>();
        for (JsonNode item : node) {
            units.add(new UnitStat(
                    item.path("unit_id").asText(),
                    item.path("name").asText(),
                    item.path("win_rate").asDouble(),
                    item.path("pick_rate").asDouble(),
                    item.path("avg_place").asDouble(),
                    item.path("sample_size").asLong(),
                    readStringList(item.path("top_items"))
            ));
        }
        return List.copyOf(units);
    }

    private List<ItemStat> parseItems(JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }
        List<ItemStat> items = new ArrayList<>();
        for (JsonNode item : node) {
            items.add(new ItemStat(
                    item.path("item_id").asText(),
                    item.path("name").asText(),
                    item.path("win_rate").asDouble(),
                    item.path("pick_rate").asDouble(),
                    item.path("sample_size").asLong()
            ));
        }
        return List.copyOf(items);
    }

    private List<AugmentStat> parseAugments(JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }
        List<AugmentStat> augments = new ArrayList<>();
        for (JsonNode item : node) {
            augments.add(new AugmentStat(
                    item.path("augment_id").asText(),
                    item.path("name").asText(),
                    item.path("win_rate").asDouble(),
                    item.path("pick_rate").asDouble(),
                    item.path("avg_place").asDouble(),
                    item.path("sample_size").asLong()
            ));
        }
        return List.copyOf(augments);
    }

    private List<String> readStringList(JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        node.forEach(item -> values.add(item.asText()));
        return List.copyOf(values);
    }
}
