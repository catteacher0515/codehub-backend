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
 * 智码 - ETL (Extract, Transform, Load) 测试
 * 作用：模拟从文件中读取数据，并将其切割成小块 (Chunk)
 */
//@Component
public class EtlTestRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(EtlTestRunner.class);

    // 1. 注入我们刚才创建的 Markdown 文件
    @Value("classpath:codehub-manual.md")
    private Resource manualResource;

    @Override
    public void run(String... args) throws Exception {
        log.info("====== 正在测试 ETL 文档切割 (Splitter Test) ======");

        try {
            // --- 第一步：Extract (读取) ---
            // 使用 TextReader 读取纯文本/Markdown 文件
            TextReader textReader = new TextReader(manualResource);
            // 这是一个常用的配置，设置字符集
            textReader.getCustomMetadata().put("charset", "UTF-8");

            List<Document> rawDocuments = textReader.read();
            log.info(">>> 1. 文件读取成功，原始文档数量: {}", rawDocuments.size());
            // 打印一下原始文档的前 100 个字符看看
            String preview = rawDocuments.get(0).getText().substring(0, Math.min(100, rawDocuments.get(0).getText().length()));
            log.info(">>> 原始文档预览: \n{}", preview);


            // --- 第二步：Transform (切割) ---
            // 使用 TokenTextSplitter (按 Token 切割，更符合大模型胃口)
            // defaultChunkSize: 默认切片大小，通常 500-1000 左右
            // minChunkSizeChars: 最小字符数
            // minChunkLengthToEmbed: 最小嵌入长度
            // keepSeparator: 是否保留分隔符
            TokenTextSplitter splitter = new TokenTextSplitter();

            List<Document> splitDocuments = splitter.apply(rawDocuments);

            log.info(">>> 2. 文档切割完成，切片(Chunk)数量: {}", splitDocuments.size());

            // 遍历打印切片内容，检查是否切得合理
            for (int i = 0; i < splitDocuments.size(); i++) {
                Document doc = splitDocuments.get(i);
                log.info(">>> Chunk [{}]: 长度={}, 内容片段: {}",
                        i, doc.getText().length(),
                        doc.getText().replace("\n", " ").substring(0, Math.min(50, doc.getText().length())) + "...");
            }

            log.info("====== ETL 测试结束，随时准备入库 ======");

        } catch (Exception e) {
            log.error("====== ETL 测试失败 ======", e);
        }
    }
}