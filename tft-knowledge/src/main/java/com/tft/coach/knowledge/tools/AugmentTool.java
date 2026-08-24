package com.tft.coach.knowledge.tools;

import com.tft.coach.data.entity.EntityKind;
import com.tft.coach.data.normalize.CanonicalKnowledgeStore;
import com.tft.coach.data.patch.PatchManager;

/** Augment lookup tool (`P1-KNOW-Augment-001`). */
public final class AugmentTool extends EntityKnowledgeTool {

    public static final String TOOL_ID = "augment-tool";

    public AugmentTool(PatchManager patchManager, CanonicalKnowledgeStore store) {
        super(TOOL_ID, EntityKind.AUGMENT, patchManager, store);
    }
}
