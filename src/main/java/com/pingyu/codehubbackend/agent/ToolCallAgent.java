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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
            log.info("🧠 CodeManus 正在大脑风暴 (Thinking)...");

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

            // 打印思维链
            if (text != null && !text.isEmpty()) {
                log.info("\n==================== 💭 [思维链 CoT] ====================\n{}\n========================================================", text.trim());
            }

            if (toolCalls != null && !toolCalls.isEmpty()) {
                log.info("🛠️ [决策] 决定调用 {} 个工具", toolCalls.size());
                return true;
            } else if (isFakeToolCall(text)) {
                log.warn("⚠️ [义肢模式] 检测到文本指令，准备手动执行...");
                return true;
            } else {
                getMessageList().add(output);
                return false;
            }

        } catch (Exception e) {
            if (isTerminationException(e)) throw (RuntimeException) e;
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
                    resultJson = matchedTool.get().call(toolCall.arguments());

                    // 刹车检测
                    if ("terminate".equals(realName) || "TERMINATE_SIGNAL".equals(resultJson) || "TERMINATE_NOW".equals(resultJson)) {
                        log.info("🛑 [优雅退场] 捕获到终止信号，CodeManus 任务完成。");
                        this.setState(AgentState.FINISHED);
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
                if (isTerminationException(e)) throw (RuntimeException) e;
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

    private boolean isTerminationException(Throwable e) {
        if (e == null) return false;
        if (e.getMessage() != null && e.getMessage().contains("TERMINATE_AGENT")) return true;
        return isTerminationException(e.getCause());
    }

    // 🚨 核心升级：更强大的正则嗅探
    private boolean isFakeToolCall(String text) {
        if (text == null) return false;
        // 只要包含 JSON 结构且里面有 name，就认为是工具调用
        return text.contains("{") && text.contains("}") && text.contains("\"name\"");
    }

    // 🚨 核心升级：正则提取 JSON
    private List<AssistantMessage.ToolCall> parseFakeToolCalls(String text) {
        List<AssistantMessage.ToolCall> fakeCalls = new ArrayList<>();
        try {
            // 使用正则匹配最外层的 JSON 对象 { ... "name": ... }
            // 这个正则通过匹配成对的大括号来提取 JSON
            // 简单版：提取第一个 { 到 最后一个 }
            int firstBrace = text.indexOf("{");
            int lastBrace = text.lastIndexOf("}");

            if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
                String jsonString = text.substring(firstBrace, lastBrace + 1);

                // 尝试解析
                JsonNode node = objectMapper.readTree(jsonString);

                // 必须包含 name 字段才算有效
                if (node.has("name")) {
                    String name = node.get("name").asText();
                    String args = node.has("arguments") ? node.get("arguments").toString() : "{}";

                    // 兼容旧的 file_path 参数
                    if (name.contains("read_file") && args.contains("file_path")) {
                        args = args.replace("file_path", "path");
                    }

                    fakeCalls.add(new AssistantMessage.ToolCall("manual_id_" + System.currentTimeMillis(), "function", name, args));
                    log.info("🕵️ [兼容模式] 成功从文本提取 JSON: {} -> {}", name, args);
                }
            }
        } catch (Exception e) {
            log.warn("❌ 尝试从文本解析 JSON 失败: {}", e.getMessage());
        }
        return fakeCalls;
    }
}