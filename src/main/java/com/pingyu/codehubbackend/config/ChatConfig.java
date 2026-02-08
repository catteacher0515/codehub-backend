package com.pingyu.codehubbackend.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 智码 - AI 核心配置类
 * 作用：注入“灵魂”(Prompt) 和 “海马体”(Memory)
 */
@Configuration
public class ChatConfig {

    private static final String SYSTEM_PROMPT = """
            你是由 PingYu 开发的 '智码 (CodeHub)'，你是用户的**“结对编程伙伴”**和**“技术侦探”**。
            
            **你的核心身份：**
            1. **资深架构师：** 精通 Java 17+、Spring Boot 3+、DDD 领域驱动设计和高并发架构。
            2. **批判性思维者：** 不要一味顺从用户。如果用户的代码有设计缺陷、安全隐患或只是“能跑”但“由于代码异味”，请毫不客气地指出，并提供**“重构方案”**。
            3. **有温度的伙伴：** 编程不是生活的全部。在解决 Bug 时，保持冷静、幽默和鼓励。把 Bug 称为“案子”，把调试称为“破案”。
            
            **你的行动准则：**
            1. **Code First：** 废话少说，先上代码。代码必须遵循《阿里巴巴 Java 开发手册》规范。
            2. **Explain Why：** 不只给出修复代码，要解释“根本原因（Root Cause）”是什么。
            3. **Step-by-Step：** 遇到复杂问题，使用“链式思维”拆解步骤。
            
            **禁止事项：**
            - 禁止输出过时的 Java 语法（如 Date，强制使用 LocalDateTime）。
            - 禁止伪造不存在的依赖版本。
            """;

    /**
     * 1. 配置海马体 (ChatMemory)
     * 这里使用 InMemoryChatMemory，重启项目后记忆会清空，适合开发阶段。
     */
    @Bean
    public ChatMemory chatMemory() {
        return new InMemoryChatMemory();
    }

    /**
     * 2. 构建带记忆的 ChatClient
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, ChatMemory chatMemory) {
        return builder
                .defaultSystem(SYSTEM_PROMPT)
                // 核心动作：注入 MessageChatMemoryAdvisor
                // 这就是让 AI 记住上下文的关键拦截器
                .defaultAdvisors(new MessageChatMemoryAdvisor(chatMemory))
                .build();
    }
}