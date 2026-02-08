package com.pingyu.codehubbackend.runner;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

/**
 * 智码 - AI 模型连接测试
 * 作用：在项目启动时自动执行一次 AI 调用，验证配置是否成功
 */
@Component
public class SpringAiTestRunner implements CommandLineRunner {

    // 注入 Spring AI 自动配置好的 ChatModel
    // 它是与 AI 交互的核心对象
    @Resource
    private ChatModel chatModel;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("====== 正在测试 AI 模型连接 ======");

        try {
            // 发送一条简单的 Prompt 进行测试 [cite: 1506]
            String promptText = "你好，我是萍雨，这是智码项目的第一次测试。";

            // 调用 call 方法获取 AI 的响应
            AssistantMessage output = chatModel.call(new Prompt(promptText))
                    .getResult()
                    .getOutput();

            System.out.println("====== AI 响应成功 ======");
            System.out.println(output.getText());
            System.out.println("==========================");
        } catch (Exception e) {
            System.err.println("====== AI 连接失败 ======");
            System.err.println("请检查 API Key 是否配置正确，或网络是否通畅。");
            e.printStackTrace();
        }
    }
}