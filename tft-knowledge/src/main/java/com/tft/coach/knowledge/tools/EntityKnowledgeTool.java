package com.tft.coach.knowledge.tools;

import com.tft.coach.data.entity.EntityKind;
import com.tft.coach.data.normalize.CanonicalKnowledgeStore;
import com.tft.coach.data.normalize.NormalizedEntity;
import com.tft.coach.data.patch.PatchManager;

import java.util.List;
import java.util.Map;

abstract class EntityKnowledgeTool implements KnowledgeTool {

    private final String toolId;
    private final EntityKind kind;
    private final PatchManager patchManager;
    private final CanonicalKnowledgeStore store;

    protected EntityKnowledgeTool(
            String toolId,
            EntityKind kind,
            PatchManager patchManager,
            CanonicalKnowledgeStore store
    ) {
        this.toolId = toolId;
        this.kind = kind;
        this.patchManager = patchManager;
        this.store = store;
    }

    @Override
    public String toolId() {
        return toolId;
    }

    @Override
    public String version() {
        return "1.0.0";
    }

    @Override
    public Map<String, Object> get(String patch, String id) {
        patchManager.require(patch);
        NormalizedEntity entity = store.get(patch, id)
                .orElseThrow(() -> new IllegalArgumentException("Unknown entity: " + id));
        return entity.canonical();
    }

    @Override
    public List<Map<String, Object>> search(String patch, String query) {
        patchManager.require(patch);
        return store.searchByName(patch, kind, query).stream()
                .map(NormalizedEntity::canonical)
                .toList();
    }
}
