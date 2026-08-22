package com.tft.coach.data.meta;

import com.tft.coach.data.spi.AdapterFetchException;

/**
 * Parses vendor-specific raw JSON into {@link MetaSnapshot}.
 */
public interface MetaSnapshotParser {

    MetaSnapshot parse(byte[] rawJson) throws AdapterFetchException;
}
