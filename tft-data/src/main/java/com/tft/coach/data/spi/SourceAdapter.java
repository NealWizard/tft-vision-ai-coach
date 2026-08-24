package com.tft.coach.data.spi;

/**
 * Unified SPI for external data sources (Riot, stats sites, community feeds).
 */
public interface SourceAdapter {

    SourceType sourceType();

    /** Stable adapter id, e.g. {@code riot-datadragon}. */
    String adapterId();

    boolean supports(FetchRequest request);

    /**
     * Fetch live payload. Implementations must not mutate snapshot history.
     */
    AdapterFetchPayload fetch(FetchRequest request) throws AdapterFetchException;
}
