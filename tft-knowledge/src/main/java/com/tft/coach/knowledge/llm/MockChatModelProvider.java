package com.tft.coach.knowledge.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/** CI / offline provider. Copies summary into reasoning; never changes scores. */
public final class MockChatModelProvider implements ChatModelProvider {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String providerId() {
        return "mock";
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        try {
            String candidates = request.variables().getOrDefault("candidates", "[]");
            ArrayNode input = (ArrayNode) mapper.readTree(candidates);
            ArrayNode out = mapper.createArrayNode();
            for (var node : input) {
                ObjectNode item = mapper.createObjectNode();
                item.put("candidate_id", node.path("candidate_id").asText());
                item.put("reasoning", node.path("summary").asText("uncertain"));
                out.add(item);
            }
            return new ChatResponse(mapper.writeValueAsString(out), providerId(), false);
        } catch (Exception ex) {
            return new ChatResponse("[]", providerId(), true);
        }
    }
}
