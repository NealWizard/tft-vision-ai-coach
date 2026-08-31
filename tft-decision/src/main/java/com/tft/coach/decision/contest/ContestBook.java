package com.tft.coach.decision.contest;

import com.tft.coach.data.meta.UnitStat;
import com.tft.coach.decision.candidate.CandidateSet;
import com.tft.coach.meta.StoredMetaSnapshot;

import java.util.ArrayList;
import java.util.List;

/** Deterministic contest from Meta only. Does not invent live opponent counts. */
public final class ContestBook {

    private ContestBook() {}

    public static List<ContestSnapshot> fromMeta(StoredMetaSnapshot stored) {
        if (stored == null || stored.snapshot().units().isEmpty()) {
            return List.of();
        }
        List<ContestSnapshot> out = new ArrayList<>();
        for (UnitStat unit : stored.snapshot().units()) {
            double level = Math.max(0.0, Math.min(1.0, unit.pickRate()));
            out.add(new ContestSnapshot(
                    unit.unitId(),
                    null,
                    level,
                    "META",
                    new CandidateSet.Confidence(0.5, "medium"),
                    stored.snapshot().capturedAt()));
        }
        return List.copyOf(out);
    }
}
