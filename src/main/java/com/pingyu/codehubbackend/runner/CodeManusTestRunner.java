package com.pingyu.codehubbackend.runner;

import com.pingyu.codehubbackend.agent.BaseAgent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CodeManusTestRunner implements CommandLineRunner {

    private final BaseAgent codeManus;

    public CodeManusTestRunner(BaseAgent codeManus) {
        this.codeManus = codeManus;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("========================================");
        log.info("🤖 CodeManus 智能体 - CoT 思维链测试");
        log.info("========================================");

        // 🕵️ 一个需要推理的复杂任务
        String request = "请像侦探一样分析当前项目：先读取 pom.xml 查看依赖，然后读取 application.yml 查看配置，最后告诉我：这个项目使用的是什么数据库（Database）？";

        // 启动！
        String result = codeManus.run(request);

        log.info("🏁 最终侦查报告:\n{}", result);
        log.info("========================================");

        // 强制退出 Spring Boot，防止后台挂着
        System.exit(0);
    }
}