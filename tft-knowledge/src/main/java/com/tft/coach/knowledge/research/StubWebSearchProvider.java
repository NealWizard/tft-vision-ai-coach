package com.tft.coach.knowledge.research;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Deterministic offline web search for tests and bootstrap. */
public final class StubWebSearchProvider implements WebSearchProvider {

    @Override
    public List<WebSearchHit> search(String query, int maxResults) {
        List<WebSearchHit> hits = new ArrayList<>();
        int limit = Math.max(1, maxResults);
        for (int i = 0; i < limit; i++) {
            hits.add(new WebSearchHit(
                    "Community result " + (i + 1) + " for " + query,
                    "https://community.example/tft/" + slug(query) + "/" + (i + 1),
                    "Community discussion about " + query + " (offline stub).",
                    Instant.parse("2026-08-24T00:00:00Z")));
        }
        return hits;
    }

    private static String slug(String query) {
        return query.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
    }
}
