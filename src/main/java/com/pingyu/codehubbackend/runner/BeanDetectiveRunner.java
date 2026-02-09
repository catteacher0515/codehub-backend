package com.pingyu.codehubbackend.runner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 智码 - Spring 容器侦探
 * 目标：查清 MCP 相关的 Bean 到底存在不存在，以及它们到底是什么类型。
 */
//@Component
public class BeanDetectiveRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(BeanDetectiveRunner.class);

    private final ApplicationContext applicationContext;

    public BeanDetectiveRunner(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("====== 🕵️ 启动 Bean 侦探 (Bean Detective) ======");

        // 1. 搜查所有包含 "mcp" 字样的 Bean
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        var mcpBeans = Arrays.stream(beanNames)
                .filter(name -> name.toLowerCase().contains("mcp"))
                .toList();

        if (mcpBeans.isEmpty()) {
            log.error(">>> ❌ 破案了：容器里连一个带 'mcp' 名字的 Bean 都没有！");
            log.info(">>> 嫌疑原因 1: spring-ai-mcp-client-spring-boot-starter 依赖没生效。");
            log.info(">>> 嫌疑原因 2: application.yml 配置前缀写错了 (检查缩进)。");
        } else {
            log.info(">>> ✅ 发现 {} 个嫌疑 Bean，请仔细核对类型：", mcpBeans.size());
            for (String beanName : mcpBeans) {
                Object bean = applicationContext.getBean(beanName);
                log.info("--------------------------------------------------");
                log.info("🔍 Bean 名称: {}", beanName);
                log.info("💎 Bean 类型: {}", bean.getClass().getName());
                log.info("🧬 父类/接口: {}", Arrays.toString(bean.getClass().getInterfaces()));
                log.info("--------------------------------------------------");
            }
        }
    }
}