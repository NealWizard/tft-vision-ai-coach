package com.tft.coach.common.flags;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeatureFlagsTest {

    @Test
    void liveFlagsDefaultOff() {
        FeatureFlags flags = new FeatureFlags();
        assertTrue(flags.isOfflineLab());
        assertTrue(flags.isPostGame());
        assertTrue(flags.isPreGameMeta());
        assertFalse(flags.isLiveExperiment());
        assertFalse(flags.isLiveDynamicRecommendation());
        assertFalse(flags.isLiveOpponentAnalysis());
        assertFalse(flags.anyLiveEnabled());
    }

    @Test
    void liveTrackRequiresExplicitFlag() {
        FeatureFlags flags = new FeatureFlags();

        flags.setLiveExperiment(true);

        assertTrue(flags.anyLiveEnabled());
    }
}
