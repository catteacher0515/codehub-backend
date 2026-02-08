package com.pingyu.codehubbackend.runner;

import io.modelcontextprotocol.client.McpSyncClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 智码 - MCP 协议连接测试 (终极抓捕版)
 */
@Component
public class McpConnectionTestRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(McpConnectionTestRunner.class);

    // 🔴 直接注入，不带 List 和 Qualifier，看它报不报错
    private final McpSyncClient mcpSyncClient;

    public McpConnectionTestRunner(McpSyncClient mcpSyncClient) {
        this.mcpSyncClient = mcpSyncClient;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("====== 🔌 正在测试手动注册的 MCP 客户端 ======");

        try {
            // 握手并列出工具
            var toolsResult = mcpSyncClient.listTools(null);

            log.info(">>> 🛠️ 远程工具箱清单:");
            toolsResult.tools().forEach(tool -> {
                log.info("   - 工具名称: {}", tool.name());
            });
            log.info(">>> ✅ 恭喜萍雨！代码级连接终于通了！");

        } catch (Exception e) {
            log.error(">>> ❌ 手动连接也失败了，请检查下方报错堆栈：", e);
        }
    }
}