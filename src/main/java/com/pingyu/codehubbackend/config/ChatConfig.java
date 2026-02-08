package com.pingyu.codehubbackend.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatConfig {

    // 1. 定义人设 (System Prompt)
    private static final String SYSTEM_PROMPT = """
            你是 '智码 (CodeHub)'，一个严谨的代码审查官。
            你的职责是根据提供的【内部开发规范】回答用户问题。
            """;

    // 2. 定义 RAG 专用模板 (User Prompt with Context)
    // {question_answer_context} 是 Spring AI 的占位符，检索到的文档会自动填在这里
    private static final String RAG_PROMPT_TEMPLATE = """
            请仅根据以下提供的【内部开发规范】上下文来回答用户的问题。
            
            【🔍 内部规范数据】
            ---------------------
            {question_answer_context}
            ---------------------
            
            【回答要求】
            1. **引用来源**：如果上下文包含 "Source:" 或文件名信息，请在回答中明确引用，例如："根据《codehub-manual.md》..."。
            2. **严禁瞎编**：如果规范里没提到的内容，请直接回答“规范中未找到相关定义”，不要用你的通用知识去编造。
            3. **风格简练**：直接给结论，不要废话。
            
            用户问题：{question}
            """;

    @Bean
    public ChatMemory chatMemory() {
        return new InMemoryChatMemory();
    }

    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, ChatMemory chatMemory, VectorStore vectorStore) {
        return builder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        new MessageChatMemoryAdvisor(chatMemory),

                        // 👇 核心动作：植入自定义 Prompt 模板
                        // 参数1: 向量库
                        // 参数2: 检索请求 (Top 4)
                        // 参数3: 我们刚才定义的“严厉模板”
                        new QuestionAnswerAdvisor(vectorStore, SearchRequest.builder().build(), RAG_PROMPT_TEMPLATE)
                )
                .build();
    }
}