package com.tft.coach.vision.provider;

/**
 * Cloud Vision is optional and off by default. High confidence never calls cloud.
 */
public final class CloudVisionFallback {

    public static final double LOW_SCORE = 0.80;

    private CloudVisionFallback() {
    }

    public static boolean shouldCallCloud(double confidence, boolean cloudEnabled) {
        return cloudEnabled && confidence < LOW_SCORE;
    }
}
