package com.tft.coach.knowledge.rag.eval;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/** Loads RAG/QA evaluation cases from classpath JSON. */
public final class EvalDatasetLoader {

    private EvalDatasetLoader() {}

    public static List<RagEvaluationRunner.RagEvalCase> loadQa100() {
        return load("knowledge/eval/qa_100.json");
    }

    public static List<RagEvaluationRunner.RagEvalCase> load(String resourcePath) {
        try (InputStream in = EvalDatasetLoader.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IllegalStateException("Missing eval dataset: " + resourcePath);
            }
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(in);
            List<RagEvaluationRunner.RagEvalCase> cases = new ArrayList<>();
            for (JsonNode node : root.path("cases")) {
                List<String> terms = new ArrayList<>();
                node.path("expected_terms").forEach(t -> terms.add(t.asText()));
                cases.add(new RagEvaluationRunner.RagEvalCase(
                        node.path("query").asText(),
                        node.path("patch").asText("set18-18.1"),
                        List.copyOf(terms)));
            }
            return List.copyOf(cases);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load eval dataset: " + resourcePath, ex);
        }
    }
}
