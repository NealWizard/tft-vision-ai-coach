package com.tft.coach.knowledge.rag.vector;

import java.util.Objects;

public record VectorFilter(String patch, String setId, String sourceType, String region, String rank) {

    public static VectorFilter ofPatch(String patch) {
        return new VectorFilter(patch, null, null, null, null);
    }

    public boolean matches(VectorRecord record) {
        if (patch != null && !patch.equals(record.patch())) {
            return false;
        }
        if (setId != null && !setId.equals(record.setId())) {
            return false;
        }
        if (sourceType != null && !sourceType.equals(record.sourceType())) {
            return false;
        }
        if (region != null && record.region() != null && !region.equals(record.region())) {
            return false;
        }
        return rank == null || record.rank() == null || rank.equals(record.rank());
    }
}
