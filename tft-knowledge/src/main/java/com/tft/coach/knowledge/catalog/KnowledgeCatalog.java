package com.tft.coach.knowledge.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Loads structured TFT rule/odds/pool/mechanic facts from classpath JSON
 * (not hardcoded in Tool classes).
 */
public final class KnowledgeCatalog {

    private final Map<String, Map<String, Object>> rulesByKey = new HashMap<>();
    private final Map<Integer, Map<String, Object>> poolsByCost = new HashMap<>();
    private final Map<Integer, Map<String, Object>> oddsByLevel = new HashMap<>();
    private final Map<String, Map<String, Object>> mechanicsById = new HashMap<>();
    private Map<String, Object> threeStar = Map.of();
    private String patch = "unknown";

    public static KnowledgeCatalog loadDefault() {
        KnowledgeCatalog catalog = new KnowledgeCatalog();
        ObjectMapper mapper = new ObjectMapper();
        catalog.loadRules(mapper, "knowledge/catalog/rules.json");
        catalog.loadUnitPool(mapper, "knowledge/catalog/unit_pool.json");
        catalog.loadShopOdds(mapper, "knowledge/catalog/shop_odds.json");
        catalog.loadMechanics(mapper, "knowledge/catalog/mechanics.json");
        return catalog;
    }

    public String patch() {
        return patch;
    }

    public Optional<Map<String, Object>> rule(String key) {
        return Optional.ofNullable(rulesByKey.get(key)).map(HashMap::new);
    }

    public List<Map<String, Object>> searchRules(String query) {
        String needle = query.toLowerCase(Locale.ROOT);
        List<Map<String, Object>> matches = new ArrayList<>();
        for (Map<String, Object> rule : rulesByKey.values()) {
            String blob = (rule.getOrDefault("key", "") + " " + rule.getOrDefault("summary", ""))
                    .toLowerCase(Locale.ROOT);
            if (blob.contains(needle)) {
                matches.add(new HashMap<>(rule));
            }
        }
        return matches;
    }

    public Optional<Map<String, Object>> unitPool(int cost) {
        return Optional.ofNullable(poolsByCost.get(cost)).map(HashMap::new);
    }

    public Optional<Map<String, Object>> shopOdds(int level) {
        return Optional.ofNullable(oddsByLevel.get(level)).map(HashMap::new);
    }

    public Map<String, Object> threeStar() {
        return new HashMap<>(threeStar);
    }

    public Optional<Map<String, Object>> mechanic(String id) {
        return Optional.ofNullable(mechanicsById.get(id)).map(HashMap::new);
    }

    public List<Map<String, Object>> searchMechanics(String query) {
        String needle = query.toLowerCase(Locale.ROOT);
        List<Map<String, Object>> matches = new ArrayList<>();
        for (Map<String, Object> mechanic : mechanicsById.values()) {
            String blob = (mechanic.getOrDefault("id", "") + " "
                    + mechanic.getOrDefault("name", "") + " "
                    + mechanic.getOrDefault("description", "")).toLowerCase(Locale.ROOT);
            if (blob.contains(needle)) {
                matches.add(new HashMap<>(mechanic));
            }
        }
        return matches;
    }

    private void loadRules(ObjectMapper mapper, String path) {
        JsonNode root = read(mapper, path);
        patch = root.path("patch").asText(patch);
        for (JsonNode node : root.path("rules")) {
            Map<String, Object> rule = new HashMap<>();
            String key = node.path("key").asText();
            rule.put("schema_version", "1.0.0");
            rule.put("id", "rule." + key.replace('.', '-'));
            rule.put("patch", patch);
            rule.put("category", node.path("category").asText());
            rule.put("key", key);
            rule.put("summary", node.path("summary").asText());
            rule.put("value", mapper.convertValue(node.path("value"), Map.class));
            rule.put("source_url", node.path("source_url").asText(null));
            rulesByKey.put(key, rule);
        }
    }

    private void loadUnitPool(ObjectMapper mapper, String path) {
        JsonNode root = read(mapper, path);
        patch = root.path("patch").asText(patch);
        String sourceUrl = root.path("source_url").asText(null);
        for (JsonNode node : root.path("pools")) {
            int cost = node.path("cost").asInt();
            Map<String, Object> row = new HashMap<>();
            row.put("schema_version", "1.0.0");
            row.put("patch", patch);
            row.put("cost", cost);
            row.put("pool_copies", node.path("pool_copies").asInt());
            row.put("source_url", sourceUrl);
            poolsByCost.put(cost, row);
        }
    }

    private void loadShopOdds(ObjectMapper mapper, String path) {
        JsonNode root = read(mapper, path);
        patch = root.path("patch").asText(patch);
        String sourceUrl = root.path("source_url").asText(null);
        for (JsonNode node : root.path("by_level")) {
            int level = node.path("level").asInt();
            List<Integer> odds = new ArrayList<>();
            node.path("odds").forEach(o -> odds.add(o.asInt()));
            Map<String, Object> row = new HashMap<>();
            row.put("schema_version", "1.0.0");
            row.put("patch", patch);
            row.put("key", "shop.roll");
            row.put("level", level);
            row.put("odds_pct_by_cost", List.copyOf(odds));
            row.put("source_url", sourceUrl);
            oddsByLevel.put(level, row);
        }
        threeStar = mapper.convertValue(root.path("three_star"), Map.class);
        threeStar = threeStar == null ? Map.of() : new HashMap<>(threeStar);
        threeStar.put("patch", patch);
        threeStar.put("key", "shop.three-star");
        threeStar.put("source_url", sourceUrl);
    }

    private void loadMechanics(ObjectMapper mapper, String path) {
        JsonNode root = read(mapper, path);
        patch = root.path("patch").asText(patch);
        String sourceUrl = root.path("source_url").asText(null);
        for (JsonNode node : root.path("mechanics")) {
            Map<String, Object> mechanic = new HashMap<>();
            String id = node.path("id").asText();
            mechanic.put("schema_version", "1.0.0");
            mechanic.put("id", id);
            mechanic.put("patch", patch);
            mechanic.put("kind", node.path("kind").asText());
            mechanic.put("name", node.path("name").asText());
            mechanic.put("description", node.path("description").asText());
            mechanic.put("source_url", sourceUrl);
            mechanicsById.put(id, mechanic);
        }
    }

    private static JsonNode read(ObjectMapper mapper, String path) {
        try (InputStream in = KnowledgeCatalog.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Missing catalog resource: " + path);
            }
            return mapper.readTree(in);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load catalog: " + path, ex);
        }
    }
}
