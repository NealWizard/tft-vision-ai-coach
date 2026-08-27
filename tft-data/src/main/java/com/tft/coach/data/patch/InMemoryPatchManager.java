package com.tft.coach.data.patch;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** In-memory patch lifecycle manager (`P1-DATA-Patch-001`). */
public final class InMemoryPatchManager implements PatchManager {

    private final Map<String, PatchRecord> patches = new LinkedHashMap<>();
    private volatile String currentPatchId;

    @Override
    public void register(PatchRecord patch) {
        Objects.requireNonNull(patch, "patch");
        patches.put(patch.id(), patch);
        if (patch.status() == PatchStatus.CURRENT || currentPatchId == null) {
            currentPatchId = patch.id();
        }
    }

    @Override
    public String currentPatch() {
        if (currentPatchId == null) {
            throw PatchRequiredException.missing();
        }
        return currentPatchId;
    }

    @Override
    public PatchRecord require(String patchId) {
        if (patchId == null || patchId.isBlank()) {
            throw PatchRequiredException.missing();
        }
        PatchRecord patch = patches.get(patchId);
        if (patch == null) {
            throw PatchRequiredException.unknown(patchId);
        }
        if (patch.isExpired(Instant.now())) {
            throw PatchRequiredException.unknown(patchId);
        }
        return patch;
    }

    @Override
    public Optional<PatchRecord> find(String patchId) {
        return Optional.ofNullable(patches.get(patchId));
    }
}
