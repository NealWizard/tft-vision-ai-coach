package com.tft.coach.data.entity;

import com.tft.coach.data.meta.MetaSnapshot;

/** Registers aliases from normalized meta snapshots (stats sources). */
public final class MetaSnapshotAliasRegistry {

    public void register(CanonicalEntityResolver resolver, MetaSnapshot snapshot) {
        String sourceType = snapshot.sourceId();
        snapshot.comps().forEach(comp -> {
            String canonical = CanonicalIdSlugs.fromSourceId(EntityKind.COMP, comp.compId());
            if (canonical != null) {
                resolver.registerAlias(sourceType, EntityKind.COMP, comp.compId(), canonical);
            }
        });
        snapshot.units().forEach(unit -> {
            String canonical = CanonicalIdSlugs.fromSourceId(EntityKind.CHAMP, unit.unitId());
            if (canonical != null) {
                resolver.registerAlias(sourceType, EntityKind.CHAMP, unit.unitId(), canonical);
            }
        });
        snapshot.items().forEach(item -> {
            String canonical = CanonicalIdSlugs.fromSourceId(EntityKind.ITEM, item.itemId());
            if (canonical != null) {
                resolver.registerAlias(sourceType, EntityKind.ITEM, item.itemId(), canonical);
            }
        });
        snapshot.augments().forEach(augment -> {
            String canonical = CanonicalIdSlugs.fromSourceId(EntityKind.AUGMENT, augment.augmentId());
            if (canonical != null) {
                resolver.registerAlias(sourceType, EntityKind.AUGMENT, augment.augmentId(), canonical);
            }
        });
    }
}
