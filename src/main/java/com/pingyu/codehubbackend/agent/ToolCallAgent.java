package com.pingyu.codehubbackend.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pingyu.codehubbackend.agent.model.AgentEvent;
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
    }

    @Override
    public boolean think() {
        if (getNextStepPrompt() != null) {
            getMessageList().add(new UserMessage(getNextStepPrompt()));
        }

        try {
            log.info("🧠 CodeManus 正在大脑风暴 (Thinking)...");
            // 📡 广播：正在思考
            notify(AgentEvent.thinking("🧠 正在思考中..."));

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

            // 📡 广播：思维链内容
            if (text != null && !text.isEmpty()) {
                log.info("\n=== 💭 [CoT] ===\n{}\n================", text.trim());
                notify(AgentEvent.thinking(text.trim()));
            }

            if (toolCalls != null && !toolCalls.isEmpty()) {
                log.info("🛠️ [决策] 决定调用 {} 个工具", toolCalls.size());
                return true;
            } else if (isFakeToolCall(text)) {
                log.warn("⚠️ [义肢模式] 检测到文本指令...");
                return true;
            } else {
                getMessageList().add(output);
                return false;
            }

        } catch (Exception e) {
            if (isTerminationException(e)) throw (RuntimeException) e;
            log.error("思考过程出错", e);
            notify(AgentEvent.error("思考过程出错: " + e.getMessage()));
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
                String args = toolCall.arguments();

                // 📡 广播：准备调用工具
                notify(AgentEvent.action(targetName, args));

                Optional<FunctionCallback> matchedTool = Arrays.stream(availableTools)
                        .filter(t -> t.getName().contains(targetName) || targetName.contains(t.getName()))
                        .findFirst();

                String resultJson;
                if (matchedTool.isPresent()) {
                    String realName = matchedTool.get().getName();
                    log.info("✅ 命中工具: [{}]", realName);

                    // 执行工具
                    resultJson = matchedTool.get().call(args);

                    // 📡 广播：工具执行结果
                    // 如果结果太长（比如网页内容），可以在这里截断再发给前端，防止卡顿
                    notify(AgentEvent.result(resultJson.length() > 500 ? resultJson.substring(0, 500) + "..." : resultJson));

                    // 刹车检测
                    if ("terminate".equals(realName) || "TERMINATE_SIGNAL".equals(resultJson) || "TERMINATE_NOW".equals(resultJson) || "TERMINATE_AGENT".equals(resultJson)) {
                        log.info("🛑 [优雅退场] CodeManus 任务完成。");
                        this.setState(AgentState.FINISHED);

                        // 提取最终原因作为 Answer
                        String finalReason = args;
                        try {
                            JsonNode node = objectMapper.readTree(args);
                            if (node.has("reason")) finalReason = node.get("reason").asText();
                        } catch (Exception ignored) {}

                        notify(AgentEvent.answer(finalReason));

                        throw new RuntimeException("TERMINATE_AGENT");
                    }
                } else {
                    resultJson = "Error: Tool '" + targetName + "' not found.";
                    notify(AgentEvent.error(resultJson));
                }

                executionResults.add(String.format("工具 [%s] 结果: %s", targetName, resultJson));
                if (!isProstheticMode) {
                    standardResponses.add(new ToolResponseMessage.ToolResponse(toolCall.id(), toolCall.name(), resultJson));
                }
            } catch (Exception e) {
                if (isTerminationException(e)) throw (RuntimeException) e;
                log.error("❌ 工具执行异常", e);
                notify(AgentEvent.error("工具执行异常: " + e.getMessage()));
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

    private boolean isFakeToolCall(String text) {
        if (text == null) return false;
        return text.contains("{") && text.contains("}") && text.contains("\"name\"");
    }

    private List<AssistantMessage.ToolCall> parseFakeToolCalls(String text) {
        List<AssistantMessage.ToolCall> fakeCalls = new ArrayList<>();
        try {
            int firstBrace = text.indexOf("{");
            int lastBrace = text.lastIndexOf("}");
            if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
                String jsonString = text.substring(firstBrace, lastBrace + 1);
                JsonNode node = objectMapper.readTree(jsonString);
                if (node.has("name")) {
                    String name = node.get("name").asText();
                    String args = node.has("arguments") ? node.get("arguments").toString() : "{}";
                    if (name.contains("read_file") && args.contains("file_path")) {
                        args = args.replace("file_path", "path");
                    }
                    fakeCalls.add(new AssistantMessage.ToolCall("manual_id_" + System.currentTimeMillis(), "function", name, args));
                }
            }
        } catch (Exception e) {
            log.warn("解析失败", e);
        }
        return fakeCalls;
    }
}