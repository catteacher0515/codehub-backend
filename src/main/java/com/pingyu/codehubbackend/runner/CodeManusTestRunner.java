package com.pingyu.codehubbackend.runner;

import com.pingyu.codehubbackend.agent.BaseAgent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

//@Component
@Slf4j
public class CodeManusTestRunner implements CommandLineRunner {

    private final BaseAgent codeManus;

    public CodeManusTestRunner(BaseAgent codeManus) {
        this.codeManus = codeManus;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("========================================");
        log.info("🤖 CodeManus - Tavily 联网能力测试");
        log.info("========================================");

        // 🕵️ 这个问题如果不联网，AI 绝对回答不上来 (因为是 2026 年的实时信息，或者假设它是实时信息)
        // 注意：Spring Boot 3.4.4 是我们假设的当前版本，让它去查真实的最新版本
        String request = "请使用 tavily_search 帮我查询：Spring Boot 目前最新的 GA (稳定) 版本号是多少？并告诉我该版本的发布日期。";

        String result = codeManus.run(request);

        log.info("🏁 最终报告:\n{}", result);
        log.info("========================================");
        System.exit(0);
    }
}