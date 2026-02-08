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
import java.util.stream.Collectors;

/**
 * 智码 - RAG 最佳实践测试：Prompt 增强与引用溯源
 */
@Component
public class AdvancedRagTestRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdvancedRagTestRunner.class);

    @jakarta.annotation.Resource
    private ChatClient chatClient;

    @jakarta.annotation.Resource
    private VectorStore vectorStore;

    @Value("classpath:codehub-manual.md")
    private Resource manualResource;

    @Override
    public void run(String... args) throws Exception {
        log.info("====== 🛡️ 启动 RAG 最佳实践测试 (Prompt Engineering) ======");

        // --- 1. 增强型 ETL (Enriched ETL) ---
        // 目标：把文件名“烙印”在文本内容里，让 AI 不想看都不行
        TextReader textReader = new TextReader(manualResource);
        textReader.getCustomMetadata().put("charset", "UTF-8");
        List<Document> rawDocs = textReader.read();
        String filename = manualResource.getFilename();

        // 💡 关键动作：内容增强 (Content Enrichment)
        List<Document> enrichedDocs = rawDocs.stream().map(doc -> {
            // 把 "Source: xxx" 加到正文最前面
            String newContent = "=== 来源文件: " + filename + " ===\n" + doc.getText();
            // 记得要把修改后的内容写回去，同时保留 metadata
            return new Document(newContent, doc.getMetadata());
        }).toList();

        // 切割并入库
        TokenTextSplitter splitter = new TokenTextSplitter();
        List<Document> splitDocs = splitter.apply(enrichedDocs);
        vectorStore.add(splitDocs);

        log.info(">>> 知识库加载完成，已将文件名 '{}' 注入到文档正文中。", filename);


        // --- 2. 提问验证 (Citation Verification) ---
        String query = "后端接口返回值的规范是什么？";

        log.info("--------------------------------------------------");
        log.info("🙋‍♂️ 提问: {}", query);
        log.info("--------------------------------------------------");

        try {
            // 使用 lambda 写法，同时传入 text 和 param
            String response = chatClient.prompt()
                    .user(u -> u.text(query)
                            .param("question", query)) // 👈 关键！把变量填进去
                    .call()
                    .content();

            log.info("🤖 智码 (增强版) 回答: \n{}", response);
            log.info("--------------------------------------------------");

            // --- 3. 侦探查证 ---
            boolean hasCitation = response.contains("codehub-manual.md") || response.contains("来源文件");
            boolean hasContent = response.contains("Result");

            if (hasCitation && hasContent) {
                log.info("✅ 测试完美通过！AI 既回答了问题，又给出了引用出处。");
            } else if (hasContent) {
                log.warn("⚠️ 勉强通过：回答了内容，但没能引用出处，可能 Prompt 权重不够。");
            } else {
                log.error("❌ 测试失败：AI 似乎在胡言乱语。");
            }

        } catch (Exception e) {
            log.error("❌ 调用失败", e);
        }
    }
}