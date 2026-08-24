package com.tft.coach.knowledge.llm;

/** Cloud LLM provider SPI (`P1-LLM-Gateway-001`). */
public interface LlmProvider {

    String providerId();

    LlmResponse complete(LlmRequest request);
}
