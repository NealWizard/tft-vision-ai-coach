package com.tft.coach.data.opgg;

import com.tft.coach.data.spi.AdapterFetchException;

/**
 * HTTP client for OP.GG stats endpoints (mockable in tests).
 */
public interface OpGgStatsHttpClient {

    byte[] getBytes(String url) throws AdapterFetchException;
}
