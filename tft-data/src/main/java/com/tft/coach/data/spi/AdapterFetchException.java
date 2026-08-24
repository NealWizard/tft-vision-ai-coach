package com.tft.coach.data.spi;

/**
 * Thrown when a {@link SourceAdapter} cannot fetch live data.
 */
public class AdapterFetchException extends Exception {

    public AdapterFetchException(String message) {
        super(message);
    }

    public AdapterFetchException(String message, Throwable cause) {
        super(message, cause);
    }
}
