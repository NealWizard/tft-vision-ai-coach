package com.tft.coach.data.lolchess;

import com.tft.coach.data.spi.AdapterFetchException;

public interface LoLChessStatsHttpClient {

    byte[] getBytes(String url) throws AdapterFetchException;
}
