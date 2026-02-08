package com.pingyu.codehubbackend.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pingyu.codehubbackend.agent.model.AgentState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.model.function.FunctionCallback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 工具调用代理 (The Hands) - 修复异常拦截版
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class ToolCallAgent extends ReActAgent {

    private final FunctionCallback[] availableTools;
    private ChatResponse toolCallChatResponse;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ToolCallAgent(FunctionCallback[] availableTools, ChatClient chatClient) {
        this.availableTools = availableTools;
        this.setChatClient(chatClient);

        log.info("🔧 [ToolCallAgent] 初始化完成，共加载 {} 个工具", availableTools.length);
        if (availableTools.length > 0) {
            Arrays.stream(availableTools).forEach(t -> log.info("   👉 可用工具: [{}]", t.getName()));
        }
    }

    @Override
    public boolean think() {
        if (getNextStepPrompt() != null) {
            getMessageList().add(new UserMessage(getNextStepPrompt()));
        }

        try {
            log.info("🧠 CodeManus 正在思考...");

            ChatResponse response = getChatClient().prompt()
                    .system(getSystemPrompt())
                    .messages(getMessageList())
                    .functions(availableTools)
                    .call()
                    .chatResponse();

            this.toolCallChatResponse = response;

            AssistantMessage output = response.getResult().getOutput();
            String text = output.getText();
            List<AssistantMessage.ToolCall> toolCalls = output.getToolCalls();

            if (text != null && !text.isEmpty()) {
                log.info("💭 思考内容: {}", text);
            }

            if (toolCalls != null && !toolCalls.isEmpty()) {
                log.info("🛠️ [标准模式] 决定调用 {} 个工具", toolCalls.size());
                return true;
            } else if (isFakeToolCall(text)) {
                log.warn("⚠️ [义肢模式] 检测到 AI 将工具调用写在了文本里，启动手动执行程序...");
                return true;
            } else {
                getMessageList().add(output);
                return false;
            }

        } catch (Exception e) {
            // 🚨🚨🚨 核心修复：如果是终止信号，不要拦截，直接往上抛！ 🚨🚨🚨
            if (isTerminationException(e)) {
                throw (RuntimeException) e;
            }

            // 只有真正的错误才打印日志并吞掉
            log.error("思考过程出错", e);
            try { Thread.sleep(1000); } catch (InterruptedException ex) {}
            return false;
        }
    }

    @Override
    public String act() {
        if (toolCallChatResponse == null) return "无需执行";

        AssistantMessage output = toolCallChatResponse.getResult().getOutput();
        List<AssistantMessage.ToolCall> toolCalls = output.getToolCalls();
        String textContent = output.getText();

        boolean isProstheticMode = (toolCalls == null || toolCalls.isEmpty()) && isFakeToolCall(textContent);

        if (isProstheticMode) {
            toolCalls = parseFakeToolCalls(textContent);
        }

        if (toolCalls == null || toolCalls.isEmpty()) {
            return "无需执行工具";
        }

        log.info("⚡ CodeManus 正在行动...");
        List<String> executionResults = new ArrayList<>();
        List<ToolResponseMessage.ToolResponse> standardResponses = new ArrayList<>();

        for (AssistantMessage.ToolCall toolCall : toolCalls) {
            try {
                String targetName = toolCall.name();
                Optional<FunctionCallback> matchedTool = Arrays.stream(availableTools)
                        .filter(t -> t.getName().contains(targetName) || targetName.contains(t.getName()))
                        .findFirst();

                String resultJson;
                if (matchedTool.isPresent()) {
                    String realName = matchedTool.get().getName();
                    log.info("✅ 命中工具: [{}] (原始请求: {})", realName, targetName);

                    // 执行工具
                    resultJson = matchedTool.get().call(toolCall.arguments());

                    // 检查返回值是否包含终止信号 (针对义肢模式或未抛异常的情况)
                    if ("terminate".equals(realName) || "TERMINATE_SIGNAL".equals(resultJson) || "TERMINATE_NOW".equals(resultJson)) {
                        log.info("🛑 [优雅退场] 捕获到终止信号，CodeManus 任务完成。");
                        this.setState(AgentState.FINISHED);
                        // 抛出异常以打断流程
                        throw new RuntimeException("TERMINATE_AGENT");
                    }

                } else {
                    log.warn("❌ 未找到工具: {}", targetName);
                    String allToolNames = Arrays.stream(availableTools).map(FunctionCallback::getName).collect(Collectors.joining(", "));
                    resultJson = "Error: Tool '" + targetName + "' not found. Available tools: [" + allToolNames + "]";
                }

                executionResults.add(String.format("工具 [%s] 结果: %s", targetName, resultJson));

                if (!isProstheticMode) {
                    standardResponses.add(new ToolResponseMessage.ToolResponse(toolCall.id(), toolCall.name(), resultJson));
                }

            } catch (Exception e) {
                // 🚨🚨🚨 同样，如果是终止信号，往上抛 🚨🚨🚨
                if (isTerminationException(e)) {
                    throw (RuntimeException) e;
                }

                log.error("❌ 工具执行异常", e);
                executionResults.add("Error: " + e.getMessage());
            }
        }

        getMessageList().add(output);

        if (isProstheticMode) {
            String systemReport = "【系统执行报告】\n" + String.join("\n", executionResults);
            getMessageList().add(new UserMessage(systemReport));
        } else {
            if (!standardResponses.isEmpty()) {
                getMessageList().add(new ToolResponseMessage(standardResponses));
            }
        }

        return String.join("\n", executionResults);
    }

    // 辅助方法：判断是否为终止信号异常
    private boolean isTerminationException(Throwable e) {
        if (e == null) return false;
        // 检查消息内容
        if (e.getMessage() != null && e.getMessage().contains("TERMINATE_AGENT")) return true;
        // 递归检查 Cause
        return isTerminationException(e.getCause());
    }

    // ... (isFakeToolCall 和 parseFakeToolCalls 保持不变，请直接复制之前的即可) ...
    private boolean isFakeToolCall(String text) {
        if (text == null) return false;
        String trimmed = text.trim();
        boolean hasKey = trimmed.contains("\"name\"") || trimmed.contains("\"tool\"") || trimmed.contains("\"function\"");
        boolean hasJsonStruct = trimmed.startsWith("{") || trimmed.contains("```json");
        return hasKey && hasJsonStruct;
    }

    private List<AssistantMessage.ToolCall> parseFakeToolCalls(String text) {
        List<AssistantMessage.ToolCall> fakeCalls = new ArrayList<>();
        try {
            String jsonString = text;
            if (jsonString.contains("```json")) {
                jsonString = jsonString.substring(jsonString.indexOf("```json") + 7);
                if (jsonString.contains("```")) jsonString = jsonString.substring(0, jsonString.indexOf("```"));
            } else if (jsonString.contains("```")) {
                jsonString = jsonString.substring(jsonString.indexOf("```") + 3);
                if (jsonString.contains("```")) jsonString = jsonString.substring(0, jsonString.indexOf("```"));
            }
            jsonString = jsonString.trim();
            int firstBrace = jsonString.indexOf("{");
            int lastBrace = jsonString.lastIndexOf("}");
            if (firstBrace != -1 && lastBrace != -1) {
                jsonString = jsonString.substring(firstBrace, lastBrace + 1);
            }
            JsonNode node = objectMapper.readTree(jsonString);
            String name = "unknown";
            if (node.has("name")) name = node.get("name").asText();
            else if (node.has("tool")) name = node.get("tool").asText();
            String args = "{}";
            if (node.has("arguments")) args = node.get("arguments").toString();
            else if (node.has("tool_input")) args = node.get("tool_input").toString();
            else if (node.has("parameters")) args = node.get("parameters").toString();
            if (name.contains("read_file") && args.contains("file_path")) {
                args = args.replace("file_path", "path");
            }
            fakeCalls.add(new AssistantMessage.ToolCall("manual_id_" + System.currentTimeMillis(), "function", name, args));
            log.info("🕵️ [兼容模式] 解析指令: {} -> {}", name, args);
        } catch (Exception e) {
            log.warn("❌ 解析伪造 ToolCall 失败: {}", e.getMessage());
        }
        return fakeCalls;
    }
}