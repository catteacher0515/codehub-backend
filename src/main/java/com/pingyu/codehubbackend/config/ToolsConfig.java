package com.pingyu.codehubbackend.config;

import com.pingyu.codehubbackend.tool.ReadFileTool;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.function.Function;

@Configuration
public class ToolsConfig {

    /**
     * 注册文件读取工具
     * 💡 关键点：@Description 里的文字就是给 AI 看的“使用说明书”。
     * AI 会根据这段话来判断什么时候调用这个工具。
     */
    @Bean
    @Description("用于读取项目根目录下的文件内容，输入参数为相对路径（例如：pom.xml, src/main/java/Main.java）")
    public Function<ReadFileTool.Request, String> readFileTool() {
        return new ReadFileTool();
    }
}