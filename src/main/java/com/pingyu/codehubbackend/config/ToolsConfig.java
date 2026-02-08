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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class ToolsConfig {

    private static final Logger log = LoggerFactory.getLogger(ToolsConfig.class);
    private final ObjectMapper mapper = new ObjectMapper();

    // ======================================================
    // 1. Client A: 本地文件系统 (不变)
    // ======================================================
    @Bean
    public McpSyncClient filesystemClient() {
        log.info("🔌 [ToolsConfig] 正在连接文件系统 MCP...");
        var parameters = ServerParameters.builder("node")
                .args("C:\\dev\\nodejs\\node_global\\node_modules\\@modelcontextprotocol\\server-filesystem\\dist\\index.js", ".")
                .build();
        return McpClient.sync(new StdioClientTransport(parameters))
                .requestTimeout(Duration.ofSeconds(10))
                .build();
    }

    // ======================================================
    // 2. Client B: Tavily 联网搜索 (双重修复版)
    // ======================================================
    @Bean
    public McpSyncClient tavilyClient() {
        log.info("🔌 [ToolsConfig] 正在从环境变量读取 Key 并连接 Tavily...");

        // 🚨 修复点 1: 从环境变量读取 Key，不再使用硬编码占位符
        String myTavilyKey = System.getenv("TAVILY_API_KEY");

        if (myTavilyKey == null || myTavilyKey.isEmpty()) {
            log.error("❌ 严重错误：找不到环境变量 TAVILY_API_KEY！请在 IDEA 运行配置中设置。");
            // 这里不抛异常，防止整个应用启动失败，但该功能将不可用
        } else {
            // 只打印前几位，确保读到了但又不泄露
            log.info("✅ 成功读取 Tavily Key: {}...", myTavilyKey.substring(0, Math.min(8, myTavilyKey.length())));
        }

        Map<String, String> env = new HashMap<>();
        if (myTavilyKey != null) {
            env.put("TAVILY_API_KEY", myTavilyKey);
        }

        // 🚨 修复点 2: Windows 环境适配 (解决 CreateProcess error=2)
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        String npxCommand = isWindows ? "npx.cmd" : "npx";
        log.info("💻 检测到操作系统: {}, 使用命令: {}", System.getProperty("os.name"), npxCommand);

        var parameters = ServerParameters.builder(npxCommand) // 使用适配后的命令
                .args("-y", "tavily-mcp")
                .env(env) // 注入正确的 Key
                .build();

        // 增加超时时间，防止网络波动
        return McpClient.sync(new StdioClientTransport(parameters))
                .requestTimeout(Duration.ofSeconds(60))
                .build();
    }

    // ======================================================
    // 3. 工具装配车间 (不变)
    // ======================================================
    @Bean
    @Primary
    public ToolCallbackProvider mcpToolCallbackProvider(McpSyncClient filesystemClient, McpSyncClient tavilyClient) {
        return () -> {
            log.info("📦 [ToolsConfig] 正在组装全能工具箱 (Filesystem + Tavily + Terminate)...");
            List<ToolCallback> tools = new ArrayList<>();

            // --- 工具 1: read_file ---
            tools.add(createMcpTool(filesystemClient, "read_file", "Reads a file from the local filesystem",
                    "{\"type\":\"object\",\"properties\":{\"path\":{\"type\":\"string\",\"description\":\"Relative path\"}},\"required\":[\"path\"]}"));

            // --- 工具 2: tavily_search ---
            tools.add(createMcpTool(tavilyClient, "tavily_search",
                    "Performs a web search optimized for AI agents. Returns consolidated answers and source links.",
                    "{\"type\":\"object\",\"properties\":{\"query\":{\"type\":\"string\",\"description\":\"The search query\"}},\"required\":[\"query\"]}"));

            // --- 工具 3: terminate ---
            tools.add(new ToolCallback() {
                @Override
                public String getName() { return "terminate"; }
                @Override
                public String getDescription() { return "Terminate the task."; }
                @Override
                public ToolDefinition getToolDefinition() {
                    return ToolDefinition.builder()
                            .name("terminate")
                            .description("Call this tool when task is completed.")
                            .inputSchema("{\"type\":\"object\",\"properties\":{\"reason\":{\"type\":\"string\"}},\"required\":[\"reason\"]}")
                            .build();
                }
                @Override
                public String call(String jsonArgs) {
                    log.info("🏁 [ManualBridge] 收到终止信号: {}", jsonArgs);
                    throw new RuntimeException("TERMINATE_AGENT");
                }
            });

            return tools.toArray(new ToolCallback[0]);
        };
    }

    // 通用构建器 (保持不变)
    private ToolCallback createMcpTool(McpSyncClient client, String toolName, String description, String schema) {
        return new ToolCallback() {
            @Override
            public String getName() { return toolName; }
            @Override
            public String getDescription() { return description; }
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name(toolName).description(description).inputSchema(schema).build();
            }
            @Override
            public String call(String jsonArgs) {
                try {
                    log.info("⚡ [ToolsConfig] 调用 MCP 工具: {}({})", toolName, jsonArgs);
                    Map<String, Object> args = mapper.readValue(jsonArgs, new TypeReference<>() {});
                    McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(toolName, args);
                    McpSchema.CallToolResult result = client.callTool(request);

                    StringBuilder contentBuilder = new StringBuilder();
                    for (Object contentItem : result.content()) {
                        if (contentItem instanceof McpSchema.TextContent textContent) {
                            contentBuilder.append(textContent.text()).append("\n");
                        }
                    }
                    String content = contentBuilder.toString();
                    String logContent = content.length() > 200 ? content.substring(0, 200) + "..." : content;
                    log.info("✅ [ToolsConfig] 执行成功: {}", logContent);
                    return content;
                } catch (Exception e) {
                    log.error("❌ [ToolsConfig] 执行失败", e);
                    return "Error: " + e.getMessage();
                }
            }
        };
    }
}