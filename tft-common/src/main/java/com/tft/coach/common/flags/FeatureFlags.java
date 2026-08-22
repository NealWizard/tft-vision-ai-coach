package com.tft.coach.common.flags;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Feature flags. Live capabilities stay off by default
 * (`P0-FOUND-FeatureFlag-001`).
 */
@ConfigurationProperties(prefix = "tft.flags")
public class FeatureFlags {

    private boolean offlineLab = true;
    private boolean postGame = true;
    private boolean preGameMeta = true;
    private boolean liveExperiment = false;
    private boolean liveDynamicRecommendation = false;
    private boolean liveOpponentAnalysis = false;

    public boolean isOfflineLab() {
        return offlineLab;
    }

    public void setOfflineLab(boolean offlineLab) {
        this.offlineLab = offlineLab;
    }

    public boolean isPostGame() {
        return postGame;
    }

    public void setPostGame(boolean postGame) {
        this.postGame = postGame;
    }

    public boolean isPreGameMeta() {
        return preGameMeta;
    }

    public void setPreGameMeta(boolean preGameMeta) {
        this.preGameMeta = preGameMeta;
    }

    public boolean isLiveExperiment() {
        return liveExperiment;
    }

    public void setLiveExperiment(boolean liveExperiment) {
        this.liveExperiment = liveExperiment;
    }

    public boolean isLiveDynamicRecommendation() {
        return liveDynamicRecommendation;
    }

    public void setLiveDynamicRecommendation(boolean liveDynamicRecommendation) {
        this.liveDynamicRecommendation = liveDynamicRecommendation;
    }

    public boolean isLiveOpponentAnalysis() {
        return liveOpponentAnalysis;
    }

    public void setLiveOpponentAnalysis(boolean liveOpponentAnalysis) {
        this.liveOpponentAnalysis = liveOpponentAnalysis;
    }

    /** Live track is considered active only when any live flag is on. */
    public boolean anyLiveEnabled() {
        return liveExperiment || liveDynamicRecommendation || liveOpponentAnalysis;
    }
}
