package com.tft.coach.state.benchmark;

import com.tft.coach.state.observation.Observation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Compares expected field→value against observations (offline fixture benchmark).
 */
public final class VisionBenchmark {

    private VisionBenchmark() {
    }

    public record Report(
            int total,
            int matched,
            int lowConfidence,
            double accuracy,
            double precision,
            double recall
    ) {
    }

    public static Report compare(Map<String, Object> expected, List<Observation> actual) {
        Objects.requireNonNull(expected, "expected");
        Objects.requireNonNull(actual, "actual");
        Map<String, Observation> byField = new LinkedHashMap<>();
        for (Observation obs : actual) {
            byField.put(obs.field(), obs);
        }
        int matched = 0;
        int low = 0;
        for (Map.Entry<String, Object> e : expected.entrySet()) {
            Observation obs = byField.get(e.getKey());
            if (obs != null && Objects.equals(String.valueOf(obs.value()), String.valueOf(e.getValue()))) {
                matched++;
                if (obs.confidence() != null && obs.confidence().score() < 0.80) {
                    low++;
                }
            }
        }
        int total = expected.size();
        int predicted = byField.size();
        double accuracy = total == 0 ? 1.0 : (double) matched / total;
        double precision = predicted == 0 ? 1.0 : (double) matched / predicted;
        return new Report(total, matched, low, accuracy, precision, accuracy);
    }
}
