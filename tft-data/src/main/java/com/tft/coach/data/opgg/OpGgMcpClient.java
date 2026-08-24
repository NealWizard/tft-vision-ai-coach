package com.tft.coach.data.opgg;

import com.tft.coach.data.spi.AdapterFetchException;

import java.util.Map;

/** Minimal OP.GG MCP boundary; adapters do not depend on MCP SDK types. */
public interface OpGgMcpClient {

    byte[] callTool(String toolName, Map<String, Object> arguments) throws AdapterFetchException;
}
