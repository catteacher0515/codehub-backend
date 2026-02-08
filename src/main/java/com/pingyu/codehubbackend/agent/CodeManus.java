package com.pingyu.codehubbackend.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Component;

@Component
public class CodeManus extends ToolCallAgent {

    private static final String SYSTEM_PROMPT = """
            你是 CodeManus，一个拥有深度推理能力的 AI 编程侦探。
            
            【思维链强制协议 (CoT)】
            在执行动作前，必须先进行思考。格式如下：
            
            Thinking: [分析现状、推理逻辑、决定下一步]
            ```json
            { "name": "工具名", "arguments": { ... } }
            ```
            
            ✅ 正确示范 1 (调用工具)：
            Thinking: 我需要读取 pom.xml 来确认依赖。
            ```json
            { "name": "read_file", "arguments": { "path": "pom.xml" } }
            ```
            
            ✅ 正确示范 2 (任务结束)：
            Thinking: 我已经获取了所有信息。
            ```json
            { "name": "terminate", "arguments": { "reason": "任务完成..." } }
            ```
            
            【工具列表】
            - read_file: 读取文件 (参数: path)
            - terminate: 结束任务 (参数: reason)
            """;

    private static final String NEXT_STEP_PROMPT = """
            请继续。先思考 (Thinking)，再行动 (JSON)。
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