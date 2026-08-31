package com.tft.coach.knowledge.llm;

public interface ChatModelProvider {

    String providerId();

    ChatResponse chat(ChatRequest request);
}
