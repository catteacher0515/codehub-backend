package com.pingyu.codehubbackend.runner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 智码 - RAG 进阶测试：元数据 (Metadata) 管理
 * 目标：验证给文档打标签后，切片是否能自动继承这些标签
 */
@Component
public class MetadataEtlTestRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(MetadataEtlTestRunner.class);

    @Value("classpath:codehub-manual.md")
    private Resource manualResource;

    @Override
    public void run(String... args) throws Exception {
        log.info("====== 🏷️ 正在测试元数据管理 (Metadata ETL) ======");

        try {
            // 1. 读取原始文档
            TextReader textReader = new TextReader(manualResource);
            textReader.getCustomMetadata().put("charset", "UTF-8");
            List<Document> rawDocs = textReader.read();

            // 2. 核心动作：给原始文档打标签 (Enrich Metadata)
            // 就像给证物袋贴条形码
            String filename = manualResource.getFilename();
            for (Document doc : rawDocs) {
                // 放入文件名
                doc.getMetadata().put("filename", filename);
                // 放入一个自定义版本号，模拟多版本管理
                doc.getMetadata().put("version", "v1.0");
                // 放入文档类型
                doc.getMetadata().put("category", "internal-doc");
            }
            log.info(">>> 原始文档标签已注入: filename={}, version={}", filename, "v1.0");

            // 3. 切割文档 (Transform)
            // 我们要验证切割后的碎片（Chunks）是否还记得自己来自哪个文件
            TokenTextSplitter splitter = new TokenTextSplitter();
            List<Document> splitDocs = splitter.apply(rawDocs);

            log.info(">>> 文档切割完成，共生成 {} 个切片。", splitDocs.size());

            // 4. 验证标签继承 (Verification)
            boolean passed = true;
            for (int i = 0; i < splitDocs.size(); i++) {
                Document chunk = splitDocs.get(i);
                Object chunkFilename = chunk.getMetadata().get("filename");
                Object chunkVersion = chunk.getMetadata().get("version");

                log.info(">>> 切片 [{}] Metadata: {}", i, chunk.getMetadata());

                // 侦探查证：切片必须持有原文件的名字
                if (!filename.equals(chunkFilename) || !"v1.0".equals(chunkVersion)) {
                    log.error("❌ 案情严重！切片丢失了元数据身份！");
                    passed = false;
                }
            }

            if (passed) {
                log.info("✅ 测试通过！所有切片均完美继承了父文档的元数据身份证。");
            }

        } catch (Exception e) {
            log.error("====== 元数据测试失败 ======", e);
        }
    }
}