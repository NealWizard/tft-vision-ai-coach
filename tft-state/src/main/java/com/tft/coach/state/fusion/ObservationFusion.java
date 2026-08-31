package com.tft.coach.state.fusion;

import com.tft.coach.state.observation.Observation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One observation per field: higher confidence wins; ties use later timestamp.
 * Scores below {@link #MIN_SCORE} are dropped so low confidence cannot pollute GameState.
 */
public final class ObservationFusion {

    public static final double MIN_SCORE = 0.80;

    private ObservationFusion() {
    }

    public static List<Observation> fuse(List<Observation> observations) {
        Map<String, Observation> byField = new LinkedHashMap<>();
        for (Observation obs : observations) {
            if (obs.confidence() == null || obs.confidence().score() < MIN_SCORE) {
                continue;
            }
            Observation existing = byField.get(obs.field());
            if (existing == null || better(obs, existing)) {
                byField.put(obs.field(), obs);
            }
        }
        return new ArrayList<>(byField.values());
    }

    private static boolean better(Observation candidate, Observation current) {
        int cmp = Double.compare(candidate.confidence().score(), current.confidence().score());
        if (cmp != 0) {
            return cmp > 0;
        }
        return candidate.timestamp().isAfter(current.timestamp());
    }
}
