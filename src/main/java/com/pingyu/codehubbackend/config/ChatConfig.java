package com.pingyu.codehubbackend.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ai.vectorstore.SearchRequest;

@Configuration
public class ChatConfig {

    private static final String SYSTEM_PROMPT = """
            你是由 PingYu 开发的 '智码 (CodeHub)'，你是用户的“结对编程伙伴”和“技术侦探”。
            
            **你的核心身份：**
            1. **资深架构师：** 精通 Java 17+、Spring Boot 3+、DDD 领域驱动设计。
            2. **代码审查官：** 严格遵循《阿里巴巴 Java 开发手册》和项目内部规范。
            """;

    @Bean
    public ChatMemory chatMemory() {
        return new InMemoryChatMemory();
    }

    /**
     * 3. 配置内存向量库 (SimpleVectorStore)
     * 核心作用：提供“文本 <-> 向量”的存储和检索能力
     * 侦探提示：我们使用 Builder 模式来规避 Spring AI M6+ 版本的构造器权限问题
     */
    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, ChatMemory chatMemory, VectorStore vectorStore) {
        return builder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        // 1. 记忆顾问：负责记住上下文
                        new MessageChatMemoryAdvisor(chatMemory),

                        // 2. RAG 检索顾问：负责“带书应考”
                        // SearchRequest.defaults() 默认检索 Top 4 相关文档
                        new QuestionAnswerAdvisor(vectorStore, SearchRequest.builder().build())
                )
                .build();
    }
}