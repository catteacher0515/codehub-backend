package com.pingyu.codehubbackend.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;
import java.util.Map;

@Configuration
public class ToolsConfig {

    private static final Logger log = LoggerFactory.getLogger(ToolsConfig.class);

    @Bean
    public McpSyncClient filesystemClient() {
        log.info("🔌 [ToolsConfig] 正在启动 Filesystem MCP Client...");
        var parameters = ServerParameters.builder("node")
                .args("C:\\dev\\nodejs\\node_global\\node_modules\\@modelcontextprotocol\\server-filesystem\\dist\\index.js", ".")
                .build();
        var transport = new StdioClientTransport(parameters);
        return McpClient.sync(transport)
                .requestTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Bean
    @Primary
    public ToolCallbackProvider mcpToolCallbackProvider(McpSyncClient filesystemClient) {
        return () -> {
            log.info("📦 [ToolsConfig] 正在手动装配工具箱...");

            // 工具 1: read_file
            ToolCallback readFileTool = new ToolCallback() {
                private final ObjectMapper mapper = new ObjectMapper();
                @Override
                public String getName() { return "read_file"; }
                @Override
                public String getDescription() { return "Reads a file from the local filesystem"; }
                @Override
                public ToolDefinition getToolDefinition() {
                    return ToolDefinition.builder()
                            .name("read_file")
                            .description("Reads a file from the local filesystem")
                            .inputSchema("{\"type\":\"object\",\"properties\":{\"path\":{\"type\":\"string\",\"description\":\"The relative path to the file\"}},\"required\":[\"path\"]}")
                            .build();
                }
                @Override
                public String call(String jsonArgs) {
                    try {
                        log.info("⚡ [ToolsConfig] 收到调用: read_file({})", jsonArgs);
                        Map<String, Object> args = mapper.readValue(jsonArgs, new TypeReference<>() {});
                        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("read_file", args);
                        McpSchema.CallToolResult result = filesystemClient.callTool(request);
                        StringBuilder contentBuilder = new StringBuilder();
                        for (Object contentItem : result.content()) {
                            if (contentItem instanceof McpSchema.TextContent textContent) {
                                contentBuilder.append(textContent.text());
                            }
                        }
                        String content = contentBuilder.toString();
                        log.info("✅ [ToolsConfig] read_file 执行成功 (长度: {})", content.length());
                        return content;
                    } catch (Exception e) {
                        log.error("❌ [ToolsConfig] read_file 失败", e);
                        return "Error: " + e.getMessage();
                    }
                }
            };

            // 工具 2: terminate (信号弹版)
            ToolCallback terminateTool = new ToolCallback() {
                @Override
                public String getName() { return "terminate"; }
                @Override
                public String getDescription() { return "Terminate the task when completed."; }
                @Override
                public ToolDefinition getToolDefinition() {
                    return ToolDefinition.builder()
                            .name("terminate")
                            .description("Call this tool IMMEDIATELY when you have found the answer.")
                            .inputSchema("{\"type\":\"object\",\"properties\":{\"reason\":{\"type\":\"string\",\"description\":\"reason\"}},\"required\":[\"reason\"]}")
                            .build();
                }
                @Override
                public String call(String jsonArgs) {
                    log.info("🏁 [ToolsConfig] 收到终止信号: terminate({})", jsonArgs);
                    // 🚨 抛出特殊异常，强制中断 Spring AI 的执行流
                    throw new RuntimeException("TERMINATE_AGENT");
                }
            };

            return new ToolCallback[] { readFileTool, terminateTool };
        };
    }
}