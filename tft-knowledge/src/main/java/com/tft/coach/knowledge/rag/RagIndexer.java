package com.tft.coach.knowledge.rag;

import com.tft.coach.knowledge.rag.chunk.SemanticChunker;
import com.tft.coach.knowledge.rag.chunk.TextChunk;
import com.tft.coach.knowledge.rag.embedding.EmbeddingProvider;
import com.tft.coach.knowledge.rag.ingest.IngestDocument;
import com.tft.coach.knowledge.rag.metadata.RagMetadataValidator;
import com.tft.coach.knowledge.rag.search.Bm25Index;
import com.tft.coach.knowledge.rag.vector.InMemoryVectorStore;
import com.tft.coach.knowledge.rag.vector.VectorRecord;

/** Indexes ingested documents into BM25 + vector stores. */
public final class RagIndexer {

    private final SemanticChunker chunker = new SemanticChunker();
    private final RagMetadataValidator validator = new RagMetadataValidator();
    private final Bm25Index bm25Index;
    private final InMemoryVectorStore vectorStore;
    private final EmbeddingProvider embeddingProvider;

    public RagIndexer(Bm25Index bm25Index, InMemoryVectorStore vectorStore, EmbeddingProvider embeddingProvider) {
        this.bm25Index = bm25Index;
        this.vectorStore = vectorStore;
        this.embeddingProvider = embeddingProvider;
    }

    public void index(IngestDocument document) {
        for (TextChunk chunk : chunker.chunk(document, 512)) {
            validator.validate(chunk);
            bm25Index.index(chunk);
            float[] embedding = embeddingProvider.embed(chunk);
            vectorStore.upsert(new VectorRecord(
                    chunk.chunkId(),
                    chunk.documentId(),
                    chunk.patch(),
                    chunk.setId(),
                    chunk.sourceType(),
                    null,
                    null,
                    embedding,
                    chunk.text()));
        }
    }
}
