package com.tft.coach.data.entity;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Derives stable canonical slugs from source-native identifiers. */
public final class CanonicalIdSlugs {

    private static final Pattern TFT_UNIT = Pattern.compile("^TFT\\d+_(.+)$");
    private static final Pattern TFT_TRAIT = Pattern.compile("^TFT\\d+_(.+)$");
    private static final Pattern TFT_AUGMENT = Pattern.compile("^TFT\\d+_(.+)$");

    private CanonicalIdSlugs() {}

    public static String canonicalId(EntityKind kind, String slug) {
        if (slug == null || slug.isBlank()) {
            return null;
        }
        String normalized = slug.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_.:-]+", "-")
                .replaceAll("^-|-$", "");
        if (normalized.isBlank()) {
            return null;
        }
        return kind.typePrefix() + "." + normalized;
    }

    public static String fromSourceId(EntityKind kind, String sourceId) {
        if (sourceId == null || sourceId.isBlank()) {
            return null;
        }
        return switch (kind) {
            case CHAMP -> fromTftPrefixed(kind, TFT_UNIT, sourceId);
            case TRAIT -> fromTftPrefixed(kind, TFT_TRAIT, sourceId);
            case AUGMENT -> fromTftPrefixed(kind, TFT_AUGMENT, sourceId);
            case ITEM -> fromItemSourceId(sourceId);
            case COMP -> canonicalId(kind, sourceId);
        };
    }

    public static String fromDisplayName(EntityKind kind, String displayName) {
        if (displayName == null || displayName.isBlank()) {
            return null;
        }
        return canonicalId(kind, displayName.replace(' ', '-'));
    }

    private static String fromTftPrefixed(EntityKind kind, Pattern pattern, String sourceId) {
        Matcher matcher = pattern.matcher(sourceId);
        if (!matcher.matches()) {
            return null;
        }
        return canonicalId(kind, matcher.group(1));
    }

    private static String fromItemSourceId(String sourceId) {
        if (sourceId.startsWith("TFT_Item_")) {
            return canonicalId(EntityKind.ITEM, sourceId.substring("TFT_Item_".length()));
        }
        if (sourceId.startsWith("TFT5_Item_")) {
            return canonicalId(EntityKind.ITEM, sourceId.substring("TFT5_Item_".length()));
        }
        return null;
    }
}
