package com.tft.coach.knowledge.tools;

import com.tft.coach.data.entity.EntityKind;
import com.tft.coach.data.normalize.CanonicalKnowledgeStore;
import com.tft.coach.data.patch.PatchManager;

/** Trait lookup tool (`P1-KNOW-Trait-001`). */
public final class TraitTool extends EntityKnowledgeTool {

    public static final String TOOL_ID = "trait-tool";

    public TraitTool(PatchManager patchManager, CanonicalKnowledgeStore store) {
        super(TOOL_ID, EntityKind.TRAIT, patchManager, store);
    }
}
