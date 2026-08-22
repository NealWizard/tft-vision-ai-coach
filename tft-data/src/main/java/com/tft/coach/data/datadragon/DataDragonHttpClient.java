package com.tft.coach.data.datadragon;

import com.tft.coach.data.spi.AdapterFetchException;

/**
 * HTTP access to Data Dragon (mockable in tests).
 */
public interface DataDragonHttpClient {

    byte[] getBytes(String url) throws AdapterFetchException;

    String getVersionsJson() throws AdapterFetchException;
}
