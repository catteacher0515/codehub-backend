package com.pingyu.codehubbackend.runner;

import com.pingyu.codehubbackend.agent.CodeManus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class CodeManusTestRunner implements CommandLineRunner {

    private final CodeManus codeManus;

    @Override
    public void run(String... args) throws Exception {
        // 这里的 sleep 是为了避开应用启动时的日志干扰
        Thread.sleep(3000);

        log.info("========================================");
        log.info("🤖 CodeManus 智能体已就绪");
        log.info("========================================");

        // 提出一个需要 "思考 -> 查文件/查网 -> 回答" 的复杂问题
        // 假设你项目根目录下有一个 README.md 或者你可以让它查 Spring 官网
        String request = "请读取当前项目根目录下的 'pom.xml' 文件，告诉我这个项目的 groupId 和 artifactId 是什么？";

        log.info("🙋‍♂️ 任务: {}", request);

        // 启动智能体
        String result = codeManus.run(request);

        log.info("========================================");
        log.info("🏁 最终结果:\n{}", result);
        log.info("========================================");
    }
}