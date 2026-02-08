package com.pingyu.codehubbackend.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
// 🟢 修复点 1: 导入 McpSchema (所有请求/响应类都在这)
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
            log.info("📦 [ToolsConfig] 正在手动装配 read_file 工具...");

            ToolCallback readFileTool = new ToolCallback() {
                private final ObjectMapper mapper = new ObjectMapper();

                @Override
                public String getName() {
                    return "read_file";
                }

                @Override
                public String getDescription() {
                    return "Reads a file from the local filesystem";
                }

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
                        log.info("⚡ [ManualBridge] 收到调用请求: read_file({})", jsonArgs);
                        Map<String, Object> args = mapper.readValue(jsonArgs, new TypeReference<>() {});

                        // 🟢 修复点 2: 使用 McpSchema.CallToolRequest
                        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("read_file", args);
                        McpSchema.CallToolResult result = filesystemClient.callTool(request);

                        // 🟢 修复点 3: 正确提取内容 (result.content() 是一个 List)
                        // 我们遍历列表，找到 TextContent 并拼接起来
                        StringBuilder contentBuilder = new StringBuilder();
                        for (Object contentItem : result.content()) {
                            if (contentItem instanceof McpSchema.TextContent textContent) {
                                contentBuilder.append(textContent.text());
                            }
                        }

                        String content = contentBuilder.toString();
                        log.info("✅ [ManualBridge] 执行成功，返回长度: {}", content.length());
                        return content;

                    } catch (Exception e) {
                        log.error("❌ [ManualBridge] 执行失败", e);
                        return "Error executing read_file: " + e.getMessage();
                    }
                }
            };
            return new ToolCallback[] { readFileTool };
        };
    }
}