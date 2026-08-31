package com.tft.coach.data.normalize;

import com.tft.coach.data.entity.CanonicalEntityResolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class KnowledgeNormalizerTest {

    @Test
    void ingestsChampionPayloadWithoutLosingRawFields() throws Exception {
        byte[] payload = readResource("datadragon/tft-champion-sample.json");
        KnowledgeNormalizer normalizer = new KnowledgeNormalizer();
        CanonicalEntityResolver resolver = new CanonicalEntityResolver();

        var entities = normalizer.ingestChampions(payload, "set18-18.1", resolver);

        assertEquals(1, entities.size());
        assertEquals("champ.ahri", entities.getFirst().canonicalId());
        assertFalse(entities.getFirst().rawPayload().length == 0);
        assertEquals("Ahri", normalizer.requireEntity("set18-18.1", "champ.ahri").canonical().get("name"));
    }

    private static byte[] readResource(String path) throws Exception {
        try (var in = KnowledgeNormalizerTest.class.getClassLoader().getResourceAsStream(path)) {
            return in.readAllBytes();
        }
    }
}
