package com.pingyu.codehubbackend.runner;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;

//@Component
public class SpringAiTestRunner implements CommandLineRunner {

    @Resource
    private ChatClient chatClient;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("====== 正在测试 AI 多轮对话记忆 (Memory Test) ======");

        // 定义一个固定的会话 ID，模拟同一个用户的连续对话
        String testChatId = "test-session-1001";

        try {
            // --- 第 1 轮对话 ---
            System.out.println(">>> 萍雨 (Round 1): 我现在的 UserController.java 里有个空指针异常，好像是 UserService 没注入。");

            String response1 = chatClient.prompt()
                    .user("我现在的 UserController.java 里有个空指针异常，好像是 UserService 没注入。")
                    // 核心动作：传入会话 ID，告诉 AI 这是 "test-session-1001" 的记忆
                    .advisors(a -> a.param(CHAT_MEMORY_CONVERSATION_ID_KEY, testChatId))
                    .call()
                    .content();

            System.out.println(">>> 智码 (Round 1): \n" + response1);
            System.out.println("--------------------------------------------------");

            // --- 第 2 轮对话 ---
            // 注意：我这里没有再提 UserController 或空指针，直接问 "帮我写代码"
            // 如果 AI 能给出 UserController 的修复代码，说明它记住了 Round 1
            System.out.println(">>> 萍雨 (Round 2): 既然你分析得对，那就直接帮我把修复后的代码写出来吧！");

            String response2 = chatClient.prompt()
                    .user("既然你分析得对，那就直接帮我把修复后的代码写出来吧！")
                    // 核心动作：再次传入相同的会话 ID
                    .advisors(a -> a.param(CHAT_MEMORY_CONVERSATION_ID_KEY, testChatId))
                    .call()
                    .content();

            System.out.println(">>> 智码 (Round 2): \n" + response2);
            System.out.println("====== 测试结束 ======");

        } catch (Exception e) {
            System.err.println("====== AI 连接失败 ======");
            e.printStackTrace();
        }
    }
}