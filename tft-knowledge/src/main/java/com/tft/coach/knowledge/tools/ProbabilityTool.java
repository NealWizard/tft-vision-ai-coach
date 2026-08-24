package com.tft.coach.knowledge.tools;

import com.tft.coach.data.patch.PatchManager;

import java.util.List;
import java.util.Map;

/** Shop roll / 3-star probability calculator (`P1-KNOW-Probability-001`). */
public final class ProbabilityTool implements KnowledgeTool {

    public static final String TOOL_ID = "probability-tool";
    private final PatchManager patchManager;

    public ProbabilityTool(PatchManager patchManager) {
        this.patchManager = patchManager;
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
        if ("shop.three-star".equals(key)) {
            return Map.of(
                    "schema_version", "1.0.0",
                    "patch", patch,
                    "key", key,
                    "copies_needed", 9,
                    "formula", "pool_remaining / total_pool");
        }
        return Map.of("patch", patch, "key", key, "probability", 0.02);
    }

    @Override
    public List<Map<String, Object>> search(String patch, String query) {
        return List.of(get(patch, query.contains("three") ? "shop.three-star" : "shop.roll"));
    }
}
