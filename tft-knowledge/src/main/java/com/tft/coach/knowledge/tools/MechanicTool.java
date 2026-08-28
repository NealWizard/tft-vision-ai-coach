package com.tft.coach.knowledge.tools;

import com.tft.coach.data.patch.PatchManager;
import com.tft.coach.knowledge.catalog.KnowledgeCatalog;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Set mechanic lookup from catalog (`P1-KNOW-Mechanic-001`). */
public final class MechanicTool implements KnowledgeTool {

    public static final String TOOL_ID = "mechanic-tool";
    private final PatchManager patchManager;
    private final KnowledgeCatalog catalog;

    public MechanicTool(PatchManager patchManager, KnowledgeCatalog catalog) {
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
    public Map<String, Object> get(String patch, String id) {
        patchManager.require(patch);
        String normalized = id.startsWith("mechanic.") ? id : "mechanic." + id;
        Map<String, Object> mechanic = catalog.mechanic(normalized)
                .or(() -> catalog.mechanic(id))
                .orElseThrow(() -> new IllegalArgumentException("Unknown mechanic: " + id));
        Map<String, Object> copy = new HashMap<>(mechanic);
        copy.put("patch", patch);
        return copy;
    }

    @Override
    public List<Map<String, Object>> search(String patch, String query) {
        patchManager.require(patch);
        List<Map<String, Object>> matches = catalog.searchMechanics(query);
        if (matches.isEmpty() && query.toLowerCase(Locale.ROOT).contains("carousel")) {
            matches = catalog.searchMechanics("carousel");
        }
        if (matches.isEmpty() && query.toLowerCase(Locale.ROOT).contains("augment")) {
            matches = catalog.searchMechanics("augment");
        }
        if (matches.isEmpty() && query.toLowerCase(Locale.ROOT).contains("portal")) {
            matches = catalog.searchMechanics("portal");
        }
        return matches.stream().map(m -> {
            Map<String, Object> copy = new HashMap<>(m);
            copy.put("patch", patch);
            return copy;
        }).toList();
    }
}
