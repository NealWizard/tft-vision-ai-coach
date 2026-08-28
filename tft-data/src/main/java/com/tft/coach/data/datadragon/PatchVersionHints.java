package com.tft.coach.data.datadragon;

/** Extracts Data Dragon version hints from internal patch ids like {@code set17-16.16}. */
public final class PatchVersionHints {

    private PatchVersionHints() {}

    public static String toDragonHint(String patchId) {
        if (patchId == null || patchId.isBlank()) {
            return "";
        }
        String trimmed = patchId.trim();
        int dash = trimmed.lastIndexOf('-');
        if (dash >= 0 && dash + 1 < trimmed.length()) {
            return trimmed.substring(dash + 1);
        }
        return trimmed;
    }

    public static String toSetId(String patchId) {
        if (patchId == null || patchId.isBlank()) {
            return "unknown";
        }
        String trimmed = patchId.trim();
        int dash = trimmed.indexOf('-');
        if (dash > 0) {
            return trimmed.substring(0, dash);
        }
        return trimmed;
    }
}
