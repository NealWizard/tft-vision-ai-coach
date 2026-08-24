package com.tft.coach.data.patch;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Manages Set/Patch lifecycle, current version and TTL (`P1-DATA-Patch-001`). */
public final class PatchManager {

    private final Map<String, PatchRecord> patches = new LinkedHashMap<>();
    private volatile String currentPatchId;

    public void register(PatchRecord patch) {
        Objects.requireNonNull(patch, "patch");
        patches.put(patch.id(), patch);
        if (patch.status() == PatchStatus.CURRENT || currentPatchId == null) {
            currentPatchId = patch.id();
        }
    }

    public String currentPatch() {
        if (currentPatchId == null) {
            throw PatchRequiredException.missing();
        }
        return currentPatchId;
    }

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

    public Optional<PatchRecord> find(String patchId) {
        return Optional.ofNullable(patches.get(patchId));
    }
}
