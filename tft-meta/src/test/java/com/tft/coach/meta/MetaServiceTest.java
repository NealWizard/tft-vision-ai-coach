package com.tft.coach.meta;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetaServiceTest {

    @Test
    void queryRequiresPatch() {
        assertThrows(IllegalArgumentException.class, () -> MetaQuery.of(" ", "global", "24h"));
    }

    @Test
    void fixtureSearchIsReproducibleAndDoesNotOverwrite() {
        MetaService service = MetaService.createDefault();
        Instant now = Instant.parse("2026-08-31T12:00:00Z");
        MetaService.SearchResult first = service.search(MetaQuery.of("set18-18.1", "global", "24h"), now);
        MetaService.SearchResult second = service.search(MetaQuery.of("set18-18.1", "global", "24h"), now);
        assertTrue(first.snapshot().isPresent());
        assertEquals(first.snapshot().get().id(), second.snapshot().get().id());
        assertEquals("meta-fixture-24h", first.snapshot().get().id());
        assertTrue(first.degradedReasons().contains("SNAPSHOT_FIXTURE"));
        assertEquals(3, first.comps().size());
        assertTrue(first.trend().isPresent());
        assertThrows(IllegalStateException.class, () ->
                service.store().save(new StoredMetaSnapshot("meta-fixture-24h", first.snapshot().get().snapshot())));
    }

    @Test
    void scoreIsProductOfExplainableParts() {
        MetaService service = MetaService.createDefault();
        var stored = service.snapshot("meta-fixture-24h").orElseThrow();
        MetaScore score = new MetaScorer().score(stored.snapshot(), "set18-18.1", Instant.parse("2026-08-31T12:00:00Z"));
        assertEquals(
                score.reliability() * score.sampleSize() * score.freshness() * score.patchMatch(),
                score.total(),
                1e-9);
        assertEquals(1.0, score.patchMatch());
        MetaScore mismatch = new MetaScorer().score(stored.snapshot(), "set18-pbe", Instant.parse("2026-08-31T12:00:00Z"));
        assertEquals(0.0, mismatch.total());
    }

    @Test
    void rankAndQueueDegradeWithoutInventing() {
        MetaService service = MetaService.createDefault();
        MetaQuery query = new MetaQuery("set18-18.1", "global", "24h", "diamond+", "ranked");
        MetaService.SearchResult result = service.search(query, Instant.parse("2026-08-31T12:00:00Z"));
        assertTrue(result.degradedReasons().contains("RANK_UNAVAILABLE"));
        assertTrue(result.degradedReasons().contains("QUEUE_UNAVAILABLE"));
        assertTrue(result.snapshot().isPresent());
    }

    @Test
    void missingSecondPatchImpactIsDegraded() {
        MetaService service = MetaService.createDefault();
        PatchImpact impact = service.patchImpact("set18-18.1", "set18-18.2", Instant.parse("2026-08-31T12:00:00Z"));
        assertTrue(impact.degraded());
        assertEquals("MISSING_PATCH_SNAPSHOT", impact.reason());
        assertEquals("set18-18.1", impact.fromPatch());
        assertEquals("set18-18.2", impact.toPatch());
        assertFalse(impact.evidence().size() > 0 && impact.reason() == null);
    }
}
