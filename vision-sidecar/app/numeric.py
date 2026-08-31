"""Numeric OCR glyph cleanup shared with Java NumericNormalizer."""

from __future__ import annotations

import re

INT_FIELDS = {
    "player.gold",
    "player.level",
    "player.hp",
    "player.xp",
    "player.streak",
}
STAGE_RE = re.compile(r"(\d{1,2})\s*[-–—]\s*(\d{1,2})")
DIGITS_RE = re.compile(r"(\d+)")


def canonicalize_raw(raw: str | None) -> str:
    if not raw:
        return ""
    return (
        raw.strip()
        .replace("I", "1")
        .replace("l", "1")
        .replace("|", "1")
        .replace("O", "0")
        .replace("o", "0")
        .replace("〇", "0")
    )


def normalize(field: str | None, raw_value: str | None):
    canonical = canonicalize_raw(raw_value)
    if not canonical or not field:
        return None
    if field == "stage":
        m = STAGE_RE.search(canonical)
        return f"{m.group(1)}-{m.group(2)}" if m else None
    if field.lower() in INT_FIELDS:
        m = DIGITS_RE.search(canonical)
        return int(m.group(1)) if m else None
    return None
