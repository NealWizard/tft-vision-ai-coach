package com.tft.coach.data.patch;

import java.util.Objects;

/** Thrown when a query omits required patch context. */
public final class PatchRequiredException extends RuntimeException {

    public PatchRequiredException(String message) {
        super(message);
    }

    public static PatchRequiredException missing() {
        return new PatchRequiredException("Patch is required for knowledge queries");
    }

    public static PatchRequiredException unknown(String patchId) {
        return new PatchRequiredException("Unknown or unregistered patch: " + Objects.requireNonNull(patchId));
    }
}
