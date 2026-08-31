package com.tft.coach.meta;

import com.tft.coach.data.entity.EntityKind;
import com.tft.coach.data.patch.PatchDiffService;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public final class PatchImpactService {

    private final MetaScorer scorer;
    private final PatchDiffService patchDiffService;

    public PatchImpactService() {
        this(new MetaScorer(), new PatchDiffService());
    }

    public PatchImpactService(MetaScorer scorer, PatchDiffService patchDiffService) {
        this.scorer = scorer;
        this.patchDiffService = patchDiffService;
    }

    public PatchImpact impact(
            String fromPatch,
            String toPatch,
            Optional<StoredMetaSnapshot> from,
            Optional<StoredMetaSnapshot> to,
            Instant now
    ) {
        if (from.isEmpty() || to.isEmpty()) {
            return new PatchImpact(
                    fromPatch,
                    toPatch,
                    0.0,
                    true,
                    List.of(),
                    "MISSING_PATCH_SNAPSHOT");
        }
        if (fromPatch.equals(toPatch)) {
            return new PatchImpact(
                    fromPatch,
                    toPatch,
                    0.0,
                    true,
                    List.of("evidence:meta:" + from.get().id()),
                    "SAME_PATCH");
        }
        StoredMetaSnapshot left = from.get();
        StoredMetaSnapshot right = to.get();
        MetaScore before = scorer.score(left.snapshot(), left.snapshot().patch(), now);
        MetaScore after = scorer.score(right.snapshot(), right.snapshot().patch(), now);
        patchDiffService.diff(
                fromPatch,
                toPatch,
                EntityKind.CHAMP,
                List.of(),
                List.of());
        return new PatchImpact(
                fromPatch,
                toPatch,
                after.total() - before.total(),
                true,
                List.of("evidence:meta:" + left.id(), "evidence:meta:" + right.id()),
                "ENTITY_DIFF_UNAVAILABLE");
    }
}
