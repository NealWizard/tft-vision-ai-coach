package com.tft.coach.knowledge.tools;

import com.tft.coach.data.evidence.EvidenceRecord;
import com.tft.coach.data.evidence.EvidenceStore;
import com.tft.coach.data.patch.PatchManager;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Structured economy/shop/xp rules (`P1-KNOW-Rules-001`). */
public final class GameRuleTool implements KnowledgeTool {

    public static final String TOOL_ID = "game-rule-tool";
    private final PatchManager patchManager;
    private final EvidenceStore evidenceStore;

    public GameRuleTool(PatchManager patchManager, EvidenceStore evidenceStore) {
        this.patchManager = patchManager;
        this.evidenceStore = evidenceStore;
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
        String evidenceId = ensureEvidence(patch, key);
        Map<String, Object> rule = baseRule(patch, key);
        rule.put("evidence", List.of(Map.of(
                "evidence_id", evidenceId,
                "source_type", "manual")));
        return rule;
    }

    @Override
    public List<Map<String, Object>> search(String patch, String query) {
        return List.of(get(patch, query.contains("interest") ? "economy.interest" : "shop.pool"));
    }

    private Map<String, Object> baseRule(String patch, String key) {
        Map<String, Object> rule = new HashMap<>();
        rule.put("schema_version", "1.0.0");
        rule.put("id", "rule." + key.replace('.', '-'));
        rule.put("patch", patch);
        rule.put("category", key.startsWith("economy") ? "economy" : "shop");
        rule.put("key", key);
        if ("economy.interest".equals(key)) {
            rule.put("value", Map.of("cap", 5, "tiers", List.of(1, 2, 3, 4, 5)));
        } else {
            rule.put("value", Map.of("shop_size", 5));
        }
        return rule;
    }

    private String ensureEvidence(String patch, String key) {
        String id = "evidence:rule:" + key + ":" + patch;
        if (evidenceStore.findById(id).isEmpty()) {
            evidenceStore.save(new EvidenceRecord(
                    EvidenceRecord.SCHEMA_VERSION,
                    id,
                    "manual",
                    "rules",
                    "manual://rules/" + key,
                    Instant.parse("2026-08-22T00:00:00Z"),
                    patch,
                    0,
                    0,
                    1.0,
                    "manual/rules/" + key));
        }
        return id;
    }
}
