package com.pingyu.codehubbackend.runner;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

@Component
public class SpringAiTestRunner implements CommandLineRunner {

    // 注意：这里注入的不再是 ChatModel，而是我们刚配置好的 ChatClient
    @Resource
    private ChatClient chatClient;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("====== 正在测试 AI 模型连接 (ChatClient 模式) ======");

        try {
            // 测试能否触发“侦探”人设
            // 我们故意写一个稍微有点问题的 Prompt，看它怎么回
            String promptText = "嘿，智码，我最近写代码写得很烦，而且我觉得 Java 的 Date 类挺好用的，为啥非要换？";

            String response = chatClient.prompt()
                    .user(promptText)
                    .call()
                    .content();

            System.out.println("====== AI 响应成功 ======");
            System.out.println(response);
            System.out.println("==========================");
        } catch (Exception e) {
            System.err.println("====== AI 连接失败 ======");
            e.printStackTrace();
        }
    }
}