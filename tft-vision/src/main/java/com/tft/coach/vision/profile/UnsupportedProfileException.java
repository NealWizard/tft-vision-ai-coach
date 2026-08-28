package com.tft.coach.vision.profile;

/**
 * Thrown when no profile matches or ROI is invalid.
 */
public final class UnsupportedProfileException extends RuntimeException {

    private final String errorCode;

    public UnsupportedProfileException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String errorCode() {
        return errorCode;
    }
}
