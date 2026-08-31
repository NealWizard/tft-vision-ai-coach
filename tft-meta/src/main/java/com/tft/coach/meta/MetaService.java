package com.tft.coach.meta;

import com.tft.coach.data.meta.CompStat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Facade for Snapshot / Score / Trend / PatchImpact. */
public final class MetaService {

    private final MetaSnapshotStore store;
    private final MetaScorer scorer;
    private final MetaTrendService trendService;
    private final PatchImpactService patchImpactService;

    public MetaService(MetaSnapshotStore store) {
        this(store, new MetaScorer(), new MetaTrendService(), new PatchImpactService());
    }

    public MetaService(
            MetaSnapshotStore store,
            MetaScorer scorer,
            MetaTrendService trendService,
            PatchImpactService patchImpactService
    ) {
        this.store = Objects.requireNonNull(store, "store");
        this.scorer = scorer;
        this.trendService = trendService;
        this.patchImpactService = patchImpactService;
    }

    public static MetaService createDefault() {
        InMemoryMetaSnapshotStore store = new InMemoryMetaSnapshotStore();
        FixtureMetaSnapshots.seed(store);
        return new MetaService(store);
    }

    public MetaSnapshotStore store() {
        return store;
    }

    public Optional<StoredMetaSnapshot> snapshot(String id) {
        return store.findById(id);
    }

    public SearchResult search(MetaQuery query, Instant now) {
        List<String> degraded = new ArrayList<>();
        if (query.rank() != null && !query.rank().isBlank()) {
            degraded.add("RANK_UNAVAILABLE");
        }
        if (query.queue() != null && !query.queue().isBlank()) {
            degraded.add("QUEUE_UNAVAILABLE");
        }
        Optional<StoredMetaSnapshot> found = store.findLatest(query);
        if (found.isEmpty()) {
            degraded.add("SNAPSHOT_MISSING");
            return new SearchResult(Optional.empty(), Optional.empty(), List.of(), degraded);
        }
        StoredMetaSnapshot stored = found.get();
        if ("fixture".equals(stored.snapshot().sourceId())) {
            degraded.add("SNAPSHOT_FIXTURE");
        }
        MetaScore score = scorer.score(stored.snapshot(), query.patch(), now);
        Optional<StoredMetaSnapshot> otherWindow = otherWindow(query);
        Optional<MetaTrend> trend = otherWindow.flatMap(other -> {
            StoredMetaSnapshot from = earlier(stored, other);
            StoredMetaSnapshot to = from.id().equals(stored.id()) ? other : stored;
            return trendService.trend(from, to);
        });
        List<ScoredComp> comps = stored.snapshot().comps().stream()
                .sorted(Comparator.comparingDouble(CompStat::top4Rate).reversed())
                .map(comp -> new ScoredComp(comp, score.total() * comp.top4Rate()))
                .toList();
        return new SearchResult(Optional.of(stored), trend, comps, degraded);
    }

    public PatchImpact patchImpact(String fromPatch, String toPatch, Instant now) {
        Optional<StoredMetaSnapshot> from = store.findLatest(MetaQuery.of(fromPatch, "global", "24h"));
        Optional<StoredMetaSnapshot> to = store.findLatest(MetaQuery.of(toPatch, "global", "24h"));
        return patchImpactService.impact(fromPatch, toPatch, from, to, now);
    }

    private Optional<StoredMetaSnapshot> otherWindow(MetaQuery query) {
        String other = "24h".equals(query.timeWindow()) ? "7d" : "24h";
        return store.findLatest(new MetaQuery(query.patch(), query.region(), other, null, null));
    }

    private static StoredMetaSnapshot earlier(StoredMetaSnapshot a, StoredMetaSnapshot b) {
        return a.snapshot().capturedAt().isBefore(b.snapshot().capturedAt()) ? a : b;
    }

    public record ScoredComp(CompStat comp, double score) {
    }

    public record SearchResult(
            Optional<StoredMetaSnapshot> snapshot,
            Optional<MetaTrend> trend,
            List<ScoredComp> comps,
            List<String> degradedReasons
    ) {
        public SearchResult {
            comps = comps == null ? List.of() : List.copyOf(comps);
            degradedReasons = degradedReasons == null ? List.of() : List.copyOf(degradedReasons);
        }

        public boolean degraded() {
            return !degradedReasons.isEmpty();
        }
    }
}
