package com.tft.coach.knowledge.llm;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Prompt template registry (`P1-LLM-Prompt-001`). */
public final class PromptTemplateRegistry {

    public record PromptTemplate(
            String id,
            String version,
            String body,
            Map<String, String> variableSchema,
            String status
    ) {
        public PromptTemplate {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(version, "version");
            Objects.requireNonNull(body, "body");
            variableSchema = variableSchema == null ? Map.of() : Map.copyOf(variableSchema);
            status = status == null ? "published" : status;
        }

        public String render(Map<String, String> variables) {
            String rendered = body;
            for (var entry : variables.entrySet()) {
                rendered = rendered.replace("{{" + entry.getKey() + "}}", entry.getValue());
            }
            return rendered;
        }
    }

    private final Map<String, PromptTemplate> templates = new HashMap<>();

    public void register(PromptTemplate template) {
        templates.put(template.id() + "@" + template.version(), template);
    }

    public Optional<PromptTemplate> find(String id, String version) {
        return Optional.ofNullable(templates.get(id + "@" + version));
    }
}
