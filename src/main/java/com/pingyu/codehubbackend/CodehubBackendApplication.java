package com.pingyu.codehubbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class CodehubBackendApplication {

    public static void main(String[] args) {
        // ✅ 修正：只保留这唯一的一次 run 调用，并把返回的 context 存下来
        ConfigurableApplicationContext context = SpringApplication.run(CodehubBackendApplication.class, args);
    }
}