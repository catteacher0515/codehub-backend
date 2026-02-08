package com.pingyu.codehubbackend.runner;

import com.pingyu.codehubbackend.model.CodeAnalysis;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

@Component
public class StructuredOutputTestRunner implements CommandLineRunner {

    @Resource
    private ChatClient chatClient;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("====== 正在测试 AI 结构化输出 (Structured Output) ======");

        // 故意给一段有问题的代码
        String badCode = """
                public class DateUtils {
                    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    
                    public static String format(Date date) {
                        return sdf.format(date);
                    }
                }
                """;

        try {
            // 核心修正：
            // 1. 在 text() 中使用 {code} 作为占位符，不要直接拼接字符串
            // 2. 使用 param("code", badCode) 安全地注入代码
            CodeAnalysis report = chatClient.prompt()
                    .user(u -> u.text("请帮我分析这段代码，并生成一份诊断报告：\n{code}")
                            .param("code", badCode)) // ✅ 安全注入
                    .call()
                    .entity(CodeAnalysis.class);

            System.out.println(">>> 诊断报告生成成功！");
            System.out.println("--------------------------------------------------");
            System.out.println("📂 案件标题: " + report.title());
            System.out.println("❤️ 健康评分: " + report.score());
            System.out.println("🐛 发现嫌疑: " + report.issues());
            System.out.println("🕵️ 侦探分析: " + report.analysis());
            System.out.println("✨ 修复方案: \n" + report.improvedCode());
            System.out.println("--------------------------------------------------");

        } catch (Exception e) {
            System.err.println("====== 结构化解析失败 ======");
            e.printStackTrace();
        }
    }
}