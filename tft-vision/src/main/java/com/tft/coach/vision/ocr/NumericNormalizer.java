package com.tft.coach.vision.ocr;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Maps OCR confusable glyphs to numeric / stage values.
 */
public final class NumericNormalizer {

    private static final Set<String> INT_FIELDS = Set.of(
            "player.gold", "player.level", "player.hp", "player.xp", "player.streak"
    );
    private static final Pattern STAGE = Pattern.compile("(\\d{1,2})\\s*[-–—]\\s*(\\d{1,2})");
    private static final Pattern DIGITS = Pattern.compile("(\\d+)");

    private NumericNormalizer() {
    }

    public static String canonicalizeRaw(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim()
                .replace('I', '1')
                .replace('l', '1')
                .replace('|', '1')
                .replace('O', '0')
                .replace('o', '0')
                .replace('〇', '0');
    }

    public static Optional<Object> normalize(String field, String rawValue) {
        String canonical = canonicalizeRaw(rawValue);
        if (canonical.isEmpty() || field == null) {
            return Optional.empty();
        }
        if ("stage".equals(field)) {
            Matcher m = STAGE.matcher(canonical);
            if (m.find()) {
                return Optional.of(m.group(1) + "-" + m.group(2));
            }
            return Optional.empty();
        }
        if (isIntField(field)) {
            Matcher m = DIGITS.matcher(canonical);
            if (m.find()) {
                return Optional.of(Integer.parseInt(m.group(1)));
            }
        }
        return Optional.empty();
    }

    private static boolean isIntField(String field) {
        if (INT_FIELDS.contains(field.toLowerCase(Locale.ROOT))) {
            return true;
        }
        return field.startsWith("shop.") && field.endsWith(".cost");
    }
}
