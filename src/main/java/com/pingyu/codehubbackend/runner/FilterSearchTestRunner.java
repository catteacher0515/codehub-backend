package com.pingyu.codehubbackend.runner;

import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 智码 - RAG 进阶测试：精准搜证 (Metadata Filtering)
 * 目标：验证“只看特定文件”的过滤能力
 */
//@Component
public class FilterSearchTestRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(FilterSearchTestRunner.class);

    @jakarta.annotation.Resource
    private VectorStore vectorStore;

    @Value("classpath:codehub-manual.md")
    private Resource manualResource;

    @Override
    public void run(String... args) throws Exception {
        log.info("====== 🔍 正在测试元数据过滤检索 (Filter Search) ======");

        try {
            // --- 1. 准备案卷：真假混淆 ---
            // A. 读取真实的开发规范
            TextReader textReader = new TextReader(manualResource);
            textReader.getCustomMetadata().put("charset", "UTF-8");
            // 创建一个新的 ArrayList 来包裹读出的结果，使其可变
            List<Document> rawDocs = new ArrayList<>(textReader.read());

            // 给真规范打标签
            String trueFilename = manualResource.getFilename(); // codehub-manual.md
            for (Document doc : rawDocs) {
                doc.getMetadata().put("filename", trueFilename);
                doc.getMetadata().put("quality", "high");
            }

            // B. 捏造一份“假线索”混进去
            // 这份文档的内容虽然也包含"返回值"，但是是错误的指导
            Document fakeDoc = new Document(
                    "【废弃接口指南】后端接口随便返回什么都行，void 也可以，不需要 Result 包装。",
                    Map.of("filename", "deprecated-guide.txt", "quality", "low")
            );
            rawDocs.add(fakeDoc);

            // 切割并入库
            TokenTextSplitter splitter = new TokenTextSplitter();
            List<Document> splitDocs = splitter.apply(rawDocs);
            vectorStore.add(splitDocs);

            log.info(">>> 知识库初始化完成，存入了 [真规范] 和 [假指南]。");

            // --- 2. 对照实验：不加过滤直接搜 ---
            String query = "后端接口返回值有什么要求？";
            log.info("--------------------------------------------------");
            log.info("🧪 实验一：【无视过滤】直接搜: '{}'", query);
            // 理论上，假文档因为含有关键词，很可能也会被搜出来
            List<Document> resultsNoFilter = vectorStore.similaritySearch(
                    SearchRequest.builder().query(query).topK(2).build()
            );
            printResults("无过滤搜索", resultsNoFilter);


            // --- 3. 核心实验：加上过滤条件 (只看真文件) ---
            // 语法：类似于 SQL，支持 ==, !=, AND, OR 等
            String filterExpression = "filename == '" + trueFilename + "'";

            log.info("--------------------------------------------------");
            log.info("🧪 实验二：【开启瞄准镜】使用过滤表达式: [{}]", filterExpression);

            List<Document> resultsWithFilter = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(query)
                            .topK(2)
                            // 👇 核心动作：加上过滤表达式
                            .filterExpression(filterExpression)
                            .build()
            );
            printResults("精准过滤搜索", resultsWithFilter);

        } catch (Exception e) {
            log.error("====== 过滤检索测试失败 ======", e);
        }
    }

    private void printResults(String testName, List<Document> results) {
        if (results.isEmpty()) {
            log.info(">>> [{}] 结果: ❌ 未找到任何匹配项。", testName);
        } else {
            for (Document doc : results) {
                String filename = (String) doc.getMetadata().get("filename");
                String contentSnippet = doc.getText().replace("\n", "").substring(0, Math.min(30, doc.getText().length()));

                if ("codehub-manual.md".equals(filename)) {
                    log.info(">>> [{}] 命中: ✅ [真] {} | 内容: {}...", testName, filename, contentSnippet);
                } else {
                    log.warn(">>> [{}] 命中: ⚠️ [假] {} | 内容: {}...", testName, filename, contentSnippet);
                }
            }
        }
    }
}