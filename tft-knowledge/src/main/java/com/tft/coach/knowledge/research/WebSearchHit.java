package com.tft.coach.knowledge.research;

import java.time.Instant;

public record WebSearchHit(String title, String url, String snippet, Instant capturedAt) {}
