package com.pingyu.codehubbackend.runner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 智码 - 向量库功能冒烟测试
 * 流程：ETL (读取+切割) -> Store (向量化入库) -> Search (语义检索)
 */
//@Component
public class VectorStoreTestRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreTestRunner.class);

    // 1. 注入之前的 Markdown 秘籍
    @Value("classpath:codehub-manual.md")
    private Resource manualResource;

    // 2. 注入我们刚配好的向量库
    @jakarta.annotation.Resource
    private VectorStore vectorStore;

    @Override
    public void run(String... args) throws Exception {
        log.info("====== 正在进行向量入库 (Vector Load Test) ======");

        try {
            // --- 步骤 A: ETL (复用之前的逻辑) ---
            TextReader textReader = new TextReader(manualResource);
            textReader.getCustomMetadata().put("charset", "UTF-8");
            List<Document> rawDocs = textReader.read();

            TokenTextSplitter splitter = new TokenTextSplitter();
            List<Document> splitDocs = splitter.apply(rawDocs);
            log.info(">>> 文档切割完毕，准备将 {} 个切片存入向量库...", splitDocs.size());

            // --- 步骤 B: 入库 (Embedding & Store) ---
            // 这一步会调用阿里云 API 消耗 Token，将文本转为向量存入内存
            vectorStore.add(splitDocs);
            log.info(">>> ✅ 入库成功！文档指纹已建立。");

            // --- 步骤 C: 语义检索 (Retrieval Verification) ---
            // 我们故意不用文档原词，而是用意思相近的词来搜
            // 文档里写的是 "Result<T>" 和 "统一响应格式"
            String query = "后端接口应该怎么返回数据？";

            log.info(">>> 🕵️ 侦探发起检索: '{}'", query);
            List<Document> results = vectorStore.similaritySearch(query);

            if (!results.isEmpty()) {
                log.info(">>> 🎯 命中目标！Top 1 证据如下:");
                log.info("--------------------------------------------------");
                // 注意：使用 getText() 而不是 getContent()
                log.info(results.get(0).getText());
                log.info("--------------------------------------------------");
            } else {
                log.warn(">>> ❌ 未搜索到相关内容，请检查 Embedding 配置或文档内容。");
            }

        } catch (Exception e) {
            log.error("====== 向量库测试失败 ======", e);
        }
    }
}