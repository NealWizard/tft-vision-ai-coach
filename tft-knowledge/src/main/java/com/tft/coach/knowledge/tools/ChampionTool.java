package com.tft.coach.knowledge.tools;

import com.tft.coach.data.entity.EntityKind;
import com.tft.coach.data.normalize.CanonicalKnowledgeStore;
import com.tft.coach.data.patch.PatchManager;

/** Champion lookup tool (`P1-KNOW-Champion-001`). */
public final class ChampionTool extends EntityKnowledgeTool {

    public static final String TOOL_ID = "champion-tool";

    public ChampionTool(PatchManager patchManager, CanonicalKnowledgeStore store) {
        super(TOOL_ID, EntityKind.CHAMP, patchManager, store);
    }
}
