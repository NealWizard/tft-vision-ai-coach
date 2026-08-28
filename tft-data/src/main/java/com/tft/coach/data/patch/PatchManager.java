package com.tft.coach.data.patch;

import java.util.Optional;

/** Manages Set/Patch lifecycle, current version and TTL (`P1-DATA-Patch-001`). */
public interface PatchManager {

    void register(PatchRecord patch);

    String currentPatch();

    PatchRecord require(String patchId);

    Optional<PatchRecord> find(String patchId);
}
