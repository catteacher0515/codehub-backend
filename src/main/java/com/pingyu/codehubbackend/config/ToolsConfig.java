package com.pingyu.codehubbackend.config;

import com.pingyu.codehubbackend.tool.ReadFileTool;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.spec.ClientMcpTransport;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
//import io.modelcontextprotocol.spec.ServerParameters;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;

@Configuration
public class ToolsConfig {

    @Bean
    public McpSyncClient mcpSyncClient() {
        // 1. 定义服务器启动参数
        var parameters = ServerParameters.builder("node")
                .args("C:\\dev\\nodejs\\node_global\\node_modules\\@modelcontextprotocol\\server-filesystem\\dist\\index.js", ".")
                .build();

        // 2. 创建 Stdio 传输层 (注意类名：StdioClientTransport)
        var transport = new StdioClientTransport(parameters);

        // 3. 构建并返回同步客户端
        return McpClient.sync(transport)
                .requestTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * 注册文件读取工具
     * 💡 关键点：@Description 里的文字就是给 AI 看的“使用说明书”。
     * AI 会根据这段话来判断什么时候调用这个工具。
     */
    @Bean
    @Description("用于读取项目根目录下的文件内容，输入参数为相对路径（例如：pom.xml, src/main/java/Main.java）")
    public Function<ReadFileTool.Request, String> readFileTool() {
        return new ReadFileTool();
    }
}