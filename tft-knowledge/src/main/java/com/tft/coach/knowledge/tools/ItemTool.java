package com.tft.coach.knowledge.tools;

import com.tft.coach.data.entity.EntityKind;
import com.tft.coach.data.normalize.CanonicalKnowledgeStore;
import com.tft.coach.data.patch.PatchManager;

/** Item lookup tool (`P1-KNOW-Item-001`). */
public final class ItemTool extends EntityKnowledgeTool {

    public static final String TOOL_ID = "item-tool";

    public ItemTool(PatchManager patchManager, CanonicalKnowledgeStore store) {
        super(TOOL_ID, EntityKind.ITEM, patchManager, store);
    }
}
