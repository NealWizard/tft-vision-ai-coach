package com.tft.coach.knowledge.rag.ingest;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** URL/HTML/Text ingestion pipeline (`P1-RAG-Ingest-001`). */
public final class DocumentIngestionPipeline {

    private final List<IngestDocument> documents = new ArrayList<>();

    public IngestDocument ingestText(String sourceType, String patch, String setId, String text) {
        IngestDocument document = IngestDocument.ofText(sourceType, patch, setId, text);
        documents.add(document);
        return document;
    }

    public IngestDocument ingestHtml(String sourceType, String patch, String setId, String html) {
        String text = html.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
        IngestDocument document = new IngestDocument(
                java.util.UUID.randomUUID().toString(),
                sourceType,
                "ingest://" + sourceType + "/html",
                patch,
                setId,
                java.time.Instant.now(),
                "text/html",
                text);
        documents.add(document);
        return document;
    }

    public List<IngestDocument> documents() {
        return List.copyOf(documents);
    }
}
