package com.tft.coach.meta;

import java.util.List;

public record PatchImpact(
        String fromPatch,
        String toPatch,
        double scoreDelta,
        boolean degraded,
        List<String> evidence,
        String reason
) {
    public PatchImpact {
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
    }
}
