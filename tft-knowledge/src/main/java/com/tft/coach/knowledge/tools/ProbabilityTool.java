package com.tft.coach.knowledge.tools;

import com.tft.coach.data.patch.PatchManager;
import com.tft.coach.knowledge.catalog.KnowledgeCatalog;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Shop roll / 3-star probability from catalog (`P1-KNOW-Probability-001`). */
public final class ProbabilityTool implements KnowledgeTool {

    public static final String TOOL_ID = "probability-tool";
    private final PatchManager patchManager;
    private final KnowledgeCatalog catalog;

    public ProbabilityTool(PatchManager patchManager, KnowledgeCatalog catalog) {
        this.patchManager = patchManager;
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
        if ("shop.three-star".equals(key)) {
            Map<String, Object> threeStar = new HashMap<>(catalog.threeStar());
            threeStar.put("patch", patch);
            return threeStar;
        }
        int level = parseLevel(key);
        return catalog.shopOdds(level)
                .map(row -> {
                    Map<String, Object> copy = new HashMap<>(row);
                    copy.put("patch", patch);
                    return copy;
                })
                .orElseThrow(() -> new IllegalArgumentException("Unknown shop odds key/level: " + key));
    }

    @Override
    public List<Map<String, Object>> search(String patch, String query) {
        if (query.toLowerCase().contains("three")) {
            return List.of(get(patch, "shop.three-star"));
        }
        return List.of(get(patch, "level." + parseLevel(query)));
    }

    private static int parseLevel(String key) {
        String digits = key.replaceAll("\\D+", "");
        if (digits.isBlank()) {
            return 8;
        }
        int value = Integer.parseInt(digits);
        if (value < 1) {
            return 1;
        }
        if (value > 10) {
            return 10;
        }
        return value;
    }
}
