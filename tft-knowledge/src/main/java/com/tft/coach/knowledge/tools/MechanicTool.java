package com.tft.coach.knowledge.tools;

import com.tft.coach.data.patch.PatchManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Set mechanic lookup tool (`P1-KNOW-Mechanic-001`). */
public final class MechanicTool implements KnowledgeTool {

    public static final String TOOL_ID = "mechanic-tool";
    private final PatchManager patchManager;

    public MechanicTool(PatchManager patchManager) {
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
    public Map<String, Object> get(String patch, String id) {
        patchManager.require(patch);
        Map<String, Object> mechanic = new HashMap<>();
        mechanic.put("schema_version", "1.0.0");
        mechanic.put("id", id);
        mechanic.put("patch", patch);
        mechanic.put("kind", "encounter");
        mechanic.put("description", "Set mechanic reference for " + id);
        return mechanic;
    }

    @Override
    public List<Map<String, Object>> search(String patch, String query) {
        return List.of(get(patch, "mechanic." + query.replace(' ', '-')));
    }
}
