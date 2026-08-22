package com.tft.coach.data.lolchess;

import com.tft.coach.data.opgg.OpGgMetaSnapshotParser;
import com.tft.coach.data.meta.MetaSnapshot;
import com.tft.coach.data.meta.MetaSnapshotParser;
import com.tft.coach.data.spi.AdapterFetchException;

/**
 * Parses normalized LoLChess meta JSON (same bundle schema as OP.GG capture format).
 */
public class LoLChessMetaSnapshotParser implements MetaSnapshotParser {

    private final OpGgMetaSnapshotParser delegate = new OpGgMetaSnapshotParser();

    @Override
    public MetaSnapshot parse(byte[] rawJson) throws AdapterFetchException {
        return delegate.parse(rawJson);
    }
}
