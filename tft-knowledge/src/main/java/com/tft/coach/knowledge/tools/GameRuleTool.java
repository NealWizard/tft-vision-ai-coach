package com.tft.coach.knowledge.tools;

import com.tft.coach.data.evidence.EvidenceRecord;
import com.tft.coach.data.evidence.EvidenceStore;
import com.tft.coach.data.patch.PatchManager;
import com.tft.coach.knowledge.catalog.KnowledgeCatalog;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Structured economy/shop/xp rules from catalog (`P1-KNOW-Rules-001`). */
public final class GameRuleTool implements KnowledgeTool {

    public static final String TOOL_ID = "game-rule-tool";
    private final PatchManager patchManager;
    private final EvidenceStore evidenceStore;
    private final KnowledgeCatalog catalog;

    public GameRuleTool(PatchManager patchManager, EvidenceStore evidenceStore, KnowledgeCatalog catalog) {
        this.patchManager = patchManager;
        this.evidenceStore = evidenceStore;
        this.catalog = catalog;
    }

    @Override
    public String toolId() {
        return TOOL_ID;
    }

    @Override
    public String version() {
        return "1.0.0";
    }

    @Override
    public Map<String, Object> get(String patch, String key) {
        patchManager.require(patch);
        Map<String, Object> rule = catalog.rule(key)
                .orElseThrow(() -> new IllegalArgumentException("Unknown rule: " + key));
        rule = new HashMap<>(rule);
        rule.put("patch", patch);
        String evidenceId = ensureEvidence(patch, key, rule);
        rule.put("evidence", List.of(Map.of(
                "evidence_id", evidenceId,
                "source_type", "manual",
                "source_url", rule.get("source_url"))));
        return rule;
    }

    @Override
    public List<Map<String, Object>> search(String patch, String query) {
        patchManager.require(patch);
        List<Map<String, Object>> matches = catalog.searchRules(query);
        if (matches.isEmpty()) {
            String key = query.toLowerCase().contains("interest") ? "economy.interest" : "shop.pool";
            return List.of(get(patch, key));
        }
        return matches.stream().map(rule -> get(patch, String.valueOf(rule.get("key")))).toList();
    }

    private String ensureEvidence(String patch, String key, Map<String, Object> rule) {
        String id = "evidence:rule:" + key + ":" + patch;
        if (evidenceStore.findById(id).isEmpty()) {
            Object sourceUrl = rule.getOrDefault("source_url", "manual://rules/" + key);
            evidenceStore.save(new EvidenceRecord(
                    EvidenceRecord.SCHEMA_VERSION,
                    id,
                    "manual",
                    "rules",
                    String.valueOf(sourceUrl),
                    Instant.parse("2026-08-22T00:00:00Z"),
                    patch,
                    0,
                    0,
                    1.0,
                    "catalog/rules/" + key));
        }
        return id;
    }
}
