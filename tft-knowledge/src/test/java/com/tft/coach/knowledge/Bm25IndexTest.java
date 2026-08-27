package com.tft.coach.knowledge;

import com.tft.coach.knowledge.rag.chunk.TextChunk;
import com.tft.coach.knowledge.rag.search.Bm25Index;
import com.tft.coach.knowledge.rag.search.TextSearchIndex;
import com.tft.coach.knowledge.rag.vector.VectorFilter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Bm25IndexTest {

    @Test
    void searchReturnsScoredHitsForMatchingTerms() {
        TextSearchIndex index = new Bm25Index();
        index.index(new TextChunk("c1", "d1", "set17-16.16", "set17", "manual", 0, "interest gold cap at 50"));
        index.index(new TextChunk("c2", "d1", "set17-16.16", "set17", "manual", 1, "Ahri is a champion"));

        var hits = index.search("interest gold", VectorFilter.ofPatch("set17-16.16"), 3);

        assertEquals(1, hits.size());
        assertEquals("c1", hits.getFirst().chunkId());
        assertTrue(hits.getFirst().score() > 0);
    }

    @Test
    void getReturnsIndexedChunk() {
        Bm25Index index = new Bm25Index();
        TextChunk chunk = new TextChunk("c1", "d1", "set17-16.16", "set17", "manual", 0, "text");
        index.index(chunk);
        assertTrue(index.get("c1").isPresent());
        assertEquals("text", index.get("c1").orElseThrow().text());
    }
}
