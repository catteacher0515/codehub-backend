package com.pingyu.codehubbackend.runner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 智码 - MCP 浏览器自动化实战测试
 * 场景：利用 Puppeteer MCP Server 让 AI 具备访问网页、抓取实时信息的能力。
 */
@Component
public class McpWebSearchTestRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(McpWebSearchTestRunner.class);

    private final ChatClient chatClient;

    // 构造注入，ChatClient 已经在 ChatConfig 中配置了 defaultTools(toolCallbackProvider)
    public McpWebSearchTestRunner(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public void run(String... args) throws Exception {
        // 1. 稍微延迟启动，避开项目启动初期的日志洪峰
        Thread.sleep(3000);

        log.info("====== 🌍 正在测试 MCP 浏览器自动化能力 (Puppeteer) ======");

        // 2. 设计一个需要实时联网访问才能回答的问题
        // 我们让 AI 去 Spring 官网看看最新的版本信息，这是它预训练模型里不一定准确的数据
        String query = "请帮我访问 https://spring.io/projects/spring-ai 并告诉我现在最新的 Spring AI 版本号是多少？";

        log.info("🙋‍♂️ 萍雨发起任务: {}", query);
        log.info("💡 侦探提示：Puppeteer 正在启动 Chromium 浏览器并抓取页面，这可能需要 20 秒左右，请耐心等待...");

        try {
            // 3. 发起对话
            // AI 会通过 Reasoning (推理) 发现自己拥有 puppeteer_navigate 等工具
            // 它会先调用工具获取网页内容，然后再整理成语言回答你
            String response = chatClient.prompt()
                    .user(query)
                    .call()
                    .content();

            // 4. 输出结果
            log.info("--------------------------------------------------");
            log.info("🤖 智码执行结果: \n{}", response);
            log.info("--------------------------------------------------");

            // 5. 简单校验逻辑
            if (response.contains("1.0.0") || response.toLowerCase().contains("milestone") || response.contains("snapshot")) {
                log.info("✅ [成功] 智码已成功穿透本地环境，通过浏览器获取了实时 Web 信息！");
            } else {
                log.warn("⚠️ [提示] AI 回答中未发现明显的版本号，请检查控制台是否有 Tool Call 记录。");
            }

        } catch (Exception e) {
            log.error("❌ [失败] 浏览器访问任务异常，请检查：", e);
            log.error("👉 排查点：1. 是否执行了 npm install -g @modelcontextprotocol/server-puppeteer");
            log.error("👉 排查点：2. ToolsConfig 中的 index.js 物理路径是否指向正确");
        }
    }
}