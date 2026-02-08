package com.pingyu.codehubbackend.runner;

import com.pingyu.codehubbackend.tool.ReadFileTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.function.Function;

/**
 * 智码 - 工具功能冒烟测试
 * 目标：不经过 AI，直接调用 Java 函数，验证读取文件的逻辑是否正常
 */
//@Component
public class ToolFunctionTestRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ToolFunctionTestRunner.class);

    // 注入我们在 ToolsConfig 里注册的 Bean
    // 注意：Bean 的名字就是方法名 readFileTool
    @Resource(name = "readFileTool")
    private Function<ReadFileTool.Request, String> readFileTool;

    @Override
    public void run(String... args) throws Exception {
        log.info("====== 🛠️ 正在测试工具函数 (Tool Function Test) ======");

        // 模拟一个请求：读取项目的 pom.xml
        String targetFile = "pom.xml";
        ReadFileTool.Request request = new ReadFileTool.Request(targetFile);

        log.info(">>> 尝试调用 readFileTool 读取: {}", targetFile);

        // 直接调用 apply 方法
        String content = readFileTool.apply(request);

        // 验证结果
        if (content != null && content.contains("<groupId>com.pingyu</groupId>")) {
            log.info(">>> ✅ 工具调用成功！读取到了 pom.xml 内容，长度: {}", content.length());
            log.info(">>> 内容摘要: {}...", content.substring(0, Math.min(50, content.length())).replace("\n", " "));
        } else {
            log.error(">>> ❌ 工具调用失败或内容不符: \n{}", content);
        }
    }
}