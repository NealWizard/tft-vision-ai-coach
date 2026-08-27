package com.tft.coach.knowledge.research;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Selects Tavily or SerpAPI by config key (`tavily` default, or `serpapi`). */
public final class ConfigurableWebSearchProvider implements WebSearchProvider {

    private final WebSearchProvider delegate;

    public ConfigurableWebSearchProvider(String providerKey, String tavilyApiKey, String serpApiKey) {
        String key = providerKey == null ? "tavily" : providerKey.toLowerCase(Locale.ROOT);
        this.delegate = switch (key) {
            case "serpapi" -> new SerpApiWebSearchProvider(Objects.requireNonNull(serpApiKey, "serpApiKey"));
            case "tavily" -> new TavilyWebSearchProvider(Objects.requireNonNull(tavilyApiKey, "tavilyApiKey"));
            default -> throw new IllegalArgumentException("Unknown web search provider: " + providerKey);
        };
    }

    @Override
    public List<WebSearchHit> search(String query, int maxResults) {
        return delegate.search(query, maxResults);
    }
}
