package com.pingyu.codehubbackend.runner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;

//@Component
public class RagTestRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(RagTestRunner.class);

    @jakarta.annotation.Resource
    private ChatClient chatClient;

    @jakarta.annotation.Resource
    private VectorStore vectorStore;

    @Value("classpath:codehub-manual.md")
    private Resource manualResource;

    @Override
    public void run(String... args) throws Exception {
        log.info("====== 🚀 启动 RAG 全流程实战测试 (RAG Action) ======");

        // --- 1. 知识预热 (Pre-load Knowledge) ---
        // 因为 SimpleVectorStore 是内存的，重启就没了，所以我们在测试前先灌入数据
        try {
            TextReader textReader = new TextReader(manualResource);
            textReader.getCustomMetadata().put("charset", "UTF-8");
            List<Document> documents = textReader.read();
            TokenTextSplitter splitter = new TokenTextSplitter();
            List<Document> splitDocs = splitter.apply(documents);

            vectorStore.add(splitDocs);
            log.info("📚 知识库加载完毕！当前存有 {} 个切片。", splitDocs.size());
        } catch (Exception e) {
            log.error("❌ 知识库加载失败，后续测试可能不准", e);
            return;
        }

        // --- 2. 模拟提问 (Ask Question) ---
        // 这个问题如果你没读过 codehub-manual.md，是绝对答不对的
        String query = "智码，我在写一个新接口，对于返回值格式和时间类型有什么强制要求吗？";
        String chatId = "rag-session-007";

        log.info("--------------------------------------------------");
        log.info("🙋‍♂️ 萍雨提问: {}", query);
        log.info("--------------------------------------------------");

        try {
            // 💡 关键点：这里直接调用 call()，ChatClient 会自动触发 QuestionAnswerAdvisor
            // 它会去 vectorStore 检索相关文档，并把文档内容拼接到 Prompt 里
            String response = chatClient.prompt()
                    .user(query)
                    .advisors(a -> a.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId))
                    .call()
                    .content();

            log.info("🤖 智码回答: \n{}", response);
            log.info("--------------------------------------------------");

            // --- 3. 结果验证 ---
            if (response.contains("Result") && (response.contains("LocalDateTime") || response.contains("datetime"))) {
                log.info("✅ 测试通过！智码成功引用了《内部开发规范》。");
            } else {
                log.warn("⚠️ 测试存疑：回答似乎未包含关键规范，请检查 RAG 链路。");
            }

        } catch (Exception e) {
            log.error("❌ RAG 调用失败", e);
        }
    }
}