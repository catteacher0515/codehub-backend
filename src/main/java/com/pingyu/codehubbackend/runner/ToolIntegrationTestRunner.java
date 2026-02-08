package com.pingyu.codehubbackend.runner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 智码 - 工具集成测试 (Tool Integration Test)
 * 目标：验证 AI 能否听懂人话，自动调用 readFileTool
 */
//@Component
public class ToolIntegrationTestRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ToolIntegrationTestRunner.class);

    @jakarta.annotation.Resource
    private ChatClient chatClient;

    @Override
    public void run(String... args) throws Exception {
        log.info("====== 🤖 正在测试 AI 工具调用能力 (Agent Action) ======");

        // 1. 构造一个只有读取文件才能回答的问题
        // 如果 AI 没调工具，它绝对猜不到 pom.xml 里的 artifactId 是什么
        String query = "请帮我读取项目根目录下的 pom.xml 文件，并告诉我这个项目的 artifactId 是什么？";

        log.info("--------------------------------------------------");
        log.info("🙋‍♂️ 萍雨指令: {}", query);
        log.info("--------------------------------------------------");

        try {
            // 2. 直接发起对话
            // 注意：这里不需要写任何代码去调用 Tool，Spring AI 会自动处理 "AI -> Tool -> AI" 的回路
            String response = chatClient.prompt()
                    .user(u -> u.text(query).param("question", query)) // 保持 RAG 的参数绑定习惯
                    .call()
                    .content();

            log.info("🤖 智码回答: \n{}", response);
            log.info("--------------------------------------------------");

            // 3. 验证结果
            if (response.contains("codehub-backend") || response.contains("codehub-next")) {
                log.info("✅ 测试通过！AI 成功调用了工具并读取了文件内容。");
            } else {
                log.warn("⚠️ 测试存疑：AI 似乎没有读到正确的文件内容，请检查日志。");
            }

        } catch (Exception e) {
            log.error("❌ 调用失败", e);
        }
    }
}