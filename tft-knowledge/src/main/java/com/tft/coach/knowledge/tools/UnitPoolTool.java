package com.tft.coach.knowledge.tools;

import com.tft.coach.data.patch.PatchManager;
import com.tft.coach.knowledge.catalog.KnowledgeCatalog;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Unit pool configuration from catalog (`P1-KNOW-UnitPool-001`). */
public final class UnitPoolTool implements KnowledgeTool {

    public static final String TOOL_ID = "unit-pool-tool";
    private final PatchManager patchManager;
    private final KnowledgeCatalog catalog;

    public UnitPoolTool(PatchManager patchManager, KnowledgeCatalog catalog) {
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
    public Map<String, Object> get(String patch, String costTier) {
        patchManager.require(patch);
        int cost = Integer.parseInt(costTier.replaceAll("\\D", "1").substring(0, 1));
        return catalog.unitPool(cost)
                .map(row -> {
                    Map<String, Object> copy = new HashMap<>(row);
                    copy.put("patch", patch);
                    return copy;
                })
                .orElseThrow(() -> new IllegalArgumentException("Unknown unit pool cost: " + costTier));
    }

    @Override
    public List<Map<String, Object>> search(String patch, String query) {
        return List.of(get(patch, query.replaceAll("[^0-9]", "1")));
    }
}
