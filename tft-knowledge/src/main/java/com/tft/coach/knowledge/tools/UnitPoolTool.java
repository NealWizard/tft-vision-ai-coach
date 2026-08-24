package com.tft.coach.knowledge.tools;

import com.tft.coach.data.patch.PatchManager;

import java.util.List;
import java.util.Map;

/** Unit pool configuration lookup (`P1-KNOW-UnitPool-001`). */
public final class UnitPoolTool implements KnowledgeTool {

    public static final String TOOL_ID = "unit-pool-tool";
    private final PatchManager patchManager;

    public UnitPoolTool(PatchManager patchManager) {
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
    public Map<String, Object> get(String patch, String costTier) {
        patchManager.require(patch);
        int copies = switch (costTier) {
            case "1" -> 30;
            case "2" -> 25;
            case "3" -> 18;
            case "4" -> 10;
            case "5" -> 9;
            default -> 0;
        };
        return Map.of(
                "schema_version", "1.0.0",
                "patch", patch,
                "cost", Integer.parseInt(costTier.replaceAll("\\D", "1")),
                "pool_copies", copies);
    }

    @Override
    public List<Map<String, Object>> search(String patch, String query) {
        return List.of(get(patch, query.replaceAll("[^0-9]", "1")));
    }
}
