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

//@Component
public class VectorStoreTestRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreTestRunner.class);

    @Value("classpath:codehub-manual.md")
    private Resource manualResource;

    @jakarta.annotation.Resource
    private VectorStore vectorStore; // 注入我们刚配置的内存向量库

    @Override
    public void run(String... args) throws Exception {
        log.info("====== 正在进行向量入库 (Vector Load Test) ======");

        try {
            // 1. ETL 流程：读取并切割
            TextReader textReader = new TextReader(manualResource);
            List<Document> documents = textReader.read();
            TokenTextSplitter splitter = new TokenTextSplitter();
            List<Document> splitDocs = splitter.apply(documents);

            // 2. 核心动作：入库 (Add to VectorStore)
            // 这一步会自动调用 DashScope 的 API 将文字转为向量并存入内存
            vectorStore.add(splitDocs);
            log.info(">>> 成功将 {} 个文档切片转化为向量并存入库中", splitDocs.size());

            // 3. 语义搜索验证 (Similarity Search)
            // 搜一个文档里没有原词，但意思相近的词，看能不能搜到
            String query = "如何返回接口数据？";
            log.info(">>> 尝试语义搜索: '{}'", query);
            List<Document> results = vectorStore.similaritySearch(query);

            if (!results.isEmpty()) {
                log.info(">>> 搜索结果命中！Top 1 内容: \n{}", results.get(0).getText());
            } else {
                log.warn(">>> 未搜索到相关内容。");
            }

        } catch (Exception e) {
            log.error("====== 向量入库测试失败 ======", e);
        }
    }
}