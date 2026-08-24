package com.tft.coach.knowledge.tools;

import java.util.List;
import java.util.Map;

/** Structured knowledge tool SPI. */
public interface KnowledgeTool {

    String toolId();

    String version();

    Map<String, Object> get(String patch, String id);

    List<Map<String, Object>> search(String patch, String query);
}
