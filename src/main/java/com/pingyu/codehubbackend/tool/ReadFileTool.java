package com.pingyu.codehubbackend.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Function;

/**
 * 工具实现类：读取项目文件内容
 * 实现标准 Java Function 接口，输入是 Request，输出是 String
 */
public class ReadFileTool implements Function<ReadFileTool.Request, String> {

    private static final Logger log = LoggerFactory.getLogger(ReadFileTool.class);

    // 1. 定义入参结构 (AI 会自动填充这个结构)
    public record Request(String filePath) {}

    // 2. 实现核心逻辑
    @Override
    public String apply(Request request) {
        log.info("⚙️ [工具触发] 正在读取文件: {}", request.filePath());
        try {
            // 为了安全，我们暂时只允许读取当前项目下的文件
            // System.getProperty("user.dir") 获取项目根路径
            Path path = Paths.get(System.getProperty("user.dir"), request.filePath());

            if (!Files.exists(path)) {
                return "❌ 错误：文件不存在 -> " + request.filePath();
            }

            return Files.readString(path);
        } catch (Exception e) {
            log.error("读取文件失败", e);
            return "❌ 读取发生异常: " + e.getMessage();
        }
    }
}