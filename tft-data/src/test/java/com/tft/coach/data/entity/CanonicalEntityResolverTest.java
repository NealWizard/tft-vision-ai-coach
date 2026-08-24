package com.tft.coach.data.entity;

import com.tft.coach.data.datadragon.DataDragonAdapter;
import com.tft.coach.data.meta.MetaSnapshot;
import com.tft.coach.data.opgg.OpGgMetaSnapshotParser;
import com.tft.coach.data.opgg.OpGgMcpStatsAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CanonicalEntityResolverTest {

    private CanonicalEntityResolver resolver;
    private byte[] championJson;
    private byte[] metaJson;

    @BeforeEach
    void setUp() throws Exception {
        resolver = new CanonicalEntityResolver();
        championJson = readResource("datadragon/tft-champion-sample.json");
        metaJson = readResource("opgg/meta-bundle-sample.json");
        new DataDragonAliasRegistry().registerChampions(resolver, championJson);
    }

    @Test
    void riotSourceIdMapsToCanonicalChampion() {
        var outcome = resolver.resolve(DataDragonAdapter.ADAPTER_ID, EntityKind.CHAMP, "TFT17_Ahri");

        assertFalse(outcome.pending());
        assertEquals("champ.ahri", outcome.canonicalId().orElseThrow());
    }

    @Test
    void statsUnitIdMatchesSameCanonicalAsRiot() throws Exception {
        MetaSnapshot snapshot = new OpGgMetaSnapshotParser().parse(metaJson);
        new MetaSnapshotAliasRegistry().register(resolver, snapshot);

        var riot = resolver.resolve(DataDragonAdapter.ADAPTER_ID, EntityKind.CHAMP, "TFT17_Xayah");
        var stats = resolver.resolve(OpGgMcpStatsAdapter.ADAPTER_ID, EntityKind.CHAMP, "TFT17_Xayah");

        assertEquals("champ.xayah", riot.canonicalId().orElseThrow());
        assertEquals(riot.canonicalId(), stats.canonicalId());
    }

    @Test
    void unknownEntityGoesToPendingQueue() {
        var outcome = resolver.resolve("manual", EntityKind.CHAMP, "UNKNOWN_FOO_BAR");

        assertTrue(outcome.pending());
        assertTrue(outcome.canonicalId().isEmpty());
        assertEquals(1, resolver.pendingQueue().size());
        assertEquals("UNKNOWN_FOO_BAR", resolver.pendingQueue().snapshot().getFirst().sourceId());
    }

    @Test
    void itemSourceIdUsesCanonicalPrefix() {
        var outcome = resolver.resolve("opgg", EntityKind.ITEM, "TFT_Item_InfinityEdge");

        assertFalse(outcome.pending());
        assertEquals("item.infinityedge", outcome.canonicalId().orElseThrow());
    }

    private static byte[] readResource(String path) throws Exception {
        try (var in = CanonicalEntityResolverTest.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Missing test resource: " + path);
            }
            return in.readAllBytes();
        }
    }
}
