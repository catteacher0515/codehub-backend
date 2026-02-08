package com.pingyu.codehubbackend.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Component;

@Component
public class CodeManus extends ToolCallAgent {

    // 🚨 核心修改：增加了【工具调用协议】部分，强制 JSON 格式
    private static final String SYSTEM_PROMPT = """
            你是 CodeManus，一个全能的 AI 编程助手。
            
            【工具调用协议 - 极其重要】
            1. 你的目标是解决用户的问题，一旦获取了足够的信息，**必须**立即停止。
            2. 使用 `read_file` 获取信息。
            3. 当任务完成时，**必须**调用 `terminate` 工具来结束对话，不要在该工具之外输出长篇大论。
            4. 如果原生 Function Call 失效，请使用 JSON 格式：
            ```json
            { "name": "terminate", "arguments": { "reason": "已找到 pom.xml 信息" } }
            ```
            5. ❌ 严禁使用 "tool", "tool_input", "function" 等其他字段名，必须使用 "name" 和 "arguments"。
            6. ❌ 严禁自己编造工具参数，例如 `read_file` 的参数是 `path`，不是 `file_path`。
            
            你可以使用的能力：
            - read_file: 读取本地文件 (参数: path)
            - write_file: 写入文件 (参数: path, content)
            - list_directory: 列出目录 (参数: path)
            """;

    private static final String NEXT_STEP_PROMPT = """
            基于当前状态，你的下一步行动是什么？
            如果需要调用工具，请务必严格遵守 JSON 格式协议。
            """;

    public CodeManus(ToolCallbackProvider toolCallbackProvider, ChatClient.Builder chatClientBuilder) {
        super(toolCallbackProvider.getToolCallbacks(),
                chatClientBuilder.build());

        this.setName("CodeManus");
        this.setSystemPrompt(SYSTEM_PROMPT);
        this.setNextStepPrompt(NEXT_STEP_PROMPT);
        this.setMaxSteps(15);
    }
}