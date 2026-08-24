package com.tft.coach.data.patch;

import com.tft.coach.data.entity.EntityKind;
import com.tft.coach.data.normalize.NormalizedEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Generates entity diffs between two patches (`P1-DATA-Patch-002`). */
public final class PatchDiffService {

    public PatchDiff diff(
            String fromPatch,
            String toPatch,
            EntityKind kind,
            List<NormalizedEntity> fromEntities,
            List<NormalizedEntity> toEntities
    ) {
        Objects.requireNonNull(fromPatch, "fromPatch");
        Objects.requireNonNull(toPatch, "toPatch");
        Map<String, NormalizedEntity> fromById = index(fromEntities);
        Map<String, NormalizedEntity> toById = index(toEntities);

        Set<String> added = new HashSet<>(toById.keySet());
        added.removeAll(fromById.keySet());
        Set<String> removed = new HashSet<>(fromById.keySet());
        removed.removeAll(toById.keySet());

        List<EntityValueChange> changed = new ArrayList<>();
        for (String id : fromById.keySet()) {
            if (!toById.containsKey(id)) {
                continue;
            }
            Map<String, Object> before = fromById.get(id).canonical();
            Map<String, Object> after = toById.get(id).canonical();
            if (!before.equals(after)) {
                changed.add(new EntityValueChange(id, before, after));
            }
        }
        return new PatchDiff(fromPatch, toPatch, kind, List.copyOf(added), List.copyOf(removed), List.copyOf(changed));
    }

    private static Map<String, NormalizedEntity> index(List<NormalizedEntity> entities) {
        Map<String, NormalizedEntity> index = new HashMap<>();
        for (NormalizedEntity entity : entities) {
            index.put(entity.canonicalId(), entity);
        }
        return index;
    }

    public record PatchDiff(
            String fromPatch,
            String toPatch,
            EntityKind kind,
            List<String> added,
            List<String> removed,
            List<EntityValueChange> changed
    ) {}

    public record EntityValueChange(String canonicalId, Map<String, Object> before, Map<String, Object> after) {}
}
