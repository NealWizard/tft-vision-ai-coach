package com.tft.coach.vision.ocr;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NumericNormalizerTest {

    @Test
    void mapsConfusableGlyphsToIntegers() {
        assertEquals(41, NumericNormalizer.normalize("player.gold", "4l").orElseThrow());
        assertEquals(50, NumericNormalizer.normalize("player.hp", "5O").orElseThrow());
        assertEquals(6, NumericNormalizer.normalize("player.level", "Lv.6").orElseThrow());
        assertEquals(2, NumericNormalizer.normalize("shop.0.cost", "2").orElseThrow());
        assertTrue(NumericNormalizer.normalize("player.gold", "").isEmpty());
        assertTrue(NumericNormalizer.normalize("stage", "??").isEmpty());
    }
}
