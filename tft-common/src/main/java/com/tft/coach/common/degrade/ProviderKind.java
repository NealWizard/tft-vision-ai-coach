package com.tft.coach.common.degrade;

/** Cloud-capable providers covered by the P0 fallback matrix. */
public enum ProviderKind {
    LLM,
    EMBEDDING,
    RERANKER,
    VISION
}
