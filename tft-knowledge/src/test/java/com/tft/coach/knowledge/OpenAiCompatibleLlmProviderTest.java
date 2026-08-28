package com.tft.coach.knowledge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tft.coach.knowledge.llm.LlmRequest;
import com.tft.coach.knowledge.llm.OpenAiCompatibleLlmProvider;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class OpenAiCompatibleLlmProviderTest {

    @Test
    void completeParsesChatCompletionResponse() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/chat/completions", exchange -> writeJson(exchange, """
                {
                  "choices":[{"message":{"content":"answer text"}}],
                  "usage":{"prompt_tokens":3,"completion_tokens":5}
                }
                """));
        server.start();
        try {
            int port = server.getAddress().getPort();
            OpenAiCompatibleLlmProvider provider = new OpenAiCompatibleLlmProvider(
                    "http://127.0.0.1:" + port,
                    "test-key",
                    "gpt-test",
                    "openai-test");

            var response = provider.complete(new LlmRequest(
                    "knowledge.answer",
                    "1.0.0",
                    Map.of("question", "What is gold cap?"),
                    64));

            assertEquals("answer text", response.content());
            assertEquals(3, response.promptTokens());
            assertEquals(5, response.completionTokens());
            assertFalse(response.degraded());
        } finally {
            server.stop(0);
        }
    }

    private static void writeJson(HttpExchange exchange, String body) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }
        byte[] bytes = body.getBytes();
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
