package com.tft.coach.data.opgg;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tft.coach.data.spi.AdapterFetchException;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/** Official OP.GG Streamable HTTP MCP client. */
public final class OfficialOpGgMcpClient implements OpGgMcpClient, AutoCloseable {

    public static final String DEFAULT_BASE_URL = "https://mcp-api.op.gg";
    public static final String DEFAULT_ENDPOINT = "/mcp";

    private final ObjectMapper mapper = new ObjectMapper();
    private final McpSyncClient client;

    public OfficialOpGgMcpClient() {
        this(DEFAULT_BASE_URL, DEFAULT_ENDPOINT);
    }

    public OfficialOpGgMcpClient(String baseUrl, String endpoint) {
        Objects.requireNonNull(baseUrl, "baseUrl");
        Objects.requireNonNull(endpoint, "endpoint");
        var transport = HttpClientStreamableHttpTransport.builder(baseUrl)
                .endpoint(endpoint)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.client = McpClient.sync(transport)
                .clientInfo(new McpSchema.Implementation("tft-vision-ai-coach", "0.1.0"))
                .requestTimeout(Duration.ofSeconds(20))
                .build();
    }

    @Override
    public synchronized byte[] callTool(String toolName, Map<String, Object> arguments)
            throws AdapterFetchException {
        try {
            if (!client.isInitialized()) {
                client.initialize();
            }
            McpSchema.CallToolResult result = client.callTool(
                    McpSchema.CallToolRequest.builder(toolName)
                            .arguments(arguments == null ? Map.of() : arguments)
                            .build());
            if (Boolean.TRUE.equals(result.isError())) {
                throw new AdapterFetchException("OP.GG MCP tool returned an error: " + toolName);
            }
            if (result.structuredContent() != null) {
                return mapper.writeValueAsBytes(result.structuredContent());
            }
            if (result.content().size() == 1
                    && result.content().getFirst() instanceof McpSchema.TextContent text) {
                return text.text().getBytes(java.nio.charset.StandardCharsets.UTF_8);
            }
            return mapper.writeValueAsBytes(result.content());
        } catch (AdapterFetchException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AdapterFetchException("OP.GG MCP call failed: " + toolName, ex);
        }
    }

    @Override
    public void close() {
        client.close();
    }
}
