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

    public PatchImpact impact(Optional<StoredMetaSnapshot> from, Optional<StoredMetaSnapshot> to, Instant now) {
        if (from.isEmpty() || to.isEmpty()) {
            return new PatchImpact(
                    from.map(s -> s.snapshot().patch()).orElse(""),
                    to.map(s -> s.snapshot().patch()).orElse(""),
                    0.0,
                    true,
                    List.of(),
                    "MISSING_PATCH_SNAPSHOT");
        }
        StoredMetaSnapshot left = from.get();
        StoredMetaSnapshot right = to.get();
        if (left.snapshot().patch().equals(right.snapshot().patch())) {
            return new PatchImpact(
                    left.snapshot().patch(),
                    right.snapshot().patch(),
                    0.0,
                    true,
                    List.of("evidence:meta:" + left.id()),
                    "SAME_PATCH");
        }
        MetaScore before = scorer.score(left.snapshot(), left.snapshot().patch(), now);
        MetaScore after = scorer.score(right.snapshot(), right.snapshot().patch(), now);
        patchDiffService.diff(
                left.snapshot().patch(),
                right.snapshot().patch(),
                EntityKind.CHAMP,
                List.of(),
                List.of());
        return new PatchImpact(
                left.snapshot().patch(),
                right.snapshot().patch(),
                after.total() - before.total(),
                true,
                List.of("evidence:meta:" + left.id(), "evidence:meta:" + right.id()),
                "ENTITY_DIFF_UNAVAILABLE");
    }
}
