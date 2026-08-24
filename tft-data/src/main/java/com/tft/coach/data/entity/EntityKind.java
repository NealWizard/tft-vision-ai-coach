package com.tft.coach.data.entity;

/** Canonical entity kinds aligned with `{type}.{slug}` IDs. */
public enum EntityKind {
    CHAMP("champ"),
    TRAIT("trait"),
    ITEM("item"),
    AUGMENT("augment"),
    COMP("comp");

    private final String typePrefix;

    EntityKind(String typePrefix) {
        this.typePrefix = typePrefix;
    }

    public String typePrefix() {
        return typePrefix;
    }
}
