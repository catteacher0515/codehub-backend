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
import org.springframework.context.annotation.Primary;

@Configuration
public class ChatConfig {

    // 1. 定义人设 (System Prompt)
    private static final String SYSTEM_PROMPT = """
            你是 '智码 (CodeHub)'，一个严谨的代码审查官。
            你的职责是根据提供的【内部开发规范】回答用户问题。
            """;

    // 2. 定义 RAG 专用模板
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

    // --- 复杂客户端：带 RAG 和工具能力 (用于高级功能) ---
    @Bean
    @Primary // 默认注入这个
    public ChatClient chatClient(ChatClient.Builder builder, ChatMemory chatMemory, VectorStore vectorStore) {
        return builder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        new MessageChatMemoryAdvisor(chatMemory),
                        new QuestionAnswerAdvisor(vectorStore, SearchRequest.builder().build(), RAG_PROMPT_TEMPLATE)
                )
                // 挂载工具 (注意：SimpleChatClient 不需要这个)
                .defaultFunctions("readFileTool")
                .build();
    }

    // --- ⭐ 新增：简单客户端 (用于页面 1：智码助手) ---
    // 不带 RAG，不带工具，只做纯粹的对话
    @Bean("simpleChatClient")
    public ChatClient simpleChatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem("你是一个友好的 AI 编程助手，名字叫'智码助手'。请用简洁、专业的语言回答用户的编程问题。")
                .build(); // 没有任何花哨的 Advisor 或 Function
    }
}