package com.tft.coach.knowledge.research;

import java.util.List;

/** Web search SPI for the Research Agent. */
public interface WebSearchProvider {

    List<WebSearchHit> search(String query, int maxResults);
}
