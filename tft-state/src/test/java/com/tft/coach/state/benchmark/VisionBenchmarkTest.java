package com.tft.coach.state.benchmark;

import com.tft.coach.state.observation.ObservationFactory;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VisionBenchmarkTest {

    @Test
    void reportsPerfectAccuracyOnMatchingFixtures() {
        VisionBenchmark.Report report = VisionBenchmark.compare(
                Map.of("player.gold", 65, "player.level", 6),
                List.of(
                        ObservationFactory.fromFixture("player.gold", 65),
                        ObservationFactory.fromFixture("player.level", 6)
                )
        );
        assertEquals(2, report.total());
        assertEquals(2, report.matched());
        assertEquals(1.0, report.accuracy());
        assertEquals(0, report.lowConfidence());
    }
}
