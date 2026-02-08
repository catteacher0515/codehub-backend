package com.pingyu.codehubbackend.agent;

import com.pingyu.codehubbackend.agent.model.AgentState;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.ArrayList;
import java.util.List;

@Data
@Slf4j
public abstract class BaseAgent {

    private String name;
    private String systemPrompt;
    private String nextStepPrompt;
    private AgentState state = AgentState.IDLE;
    private List<Message> messageList = new ArrayList<>();
    private int maxSteps = 15;
    private int currentStep = 0;
    private ChatClient chatClient;

    public String run(String userPrompt) {
        if (this.state != AgentState.IDLE) {
            throw new RuntimeException("智能体正在忙，请稍后再试！当前状态: " + this.state);
        }

        this.state = AgentState.RUNNING;
        this.currentStep = 0;
        this.messageList.clear();
        this.messageList.add(new UserMessage(userPrompt));

        List<String> results = new ArrayList<>();

        try {
            log.info("🚀 [{}] 启动任务: {}", this.name, userPrompt);

            while (currentStep < maxSteps && state != AgentState.FINISHED) {
                currentStep++;
                log.info("🔄 Step {}/{}", currentStep, maxSteps);

                String stepResult = step();

                // 兼容旧的字符串检测方式
                if (stepResult != null && stepResult.contains("TERMINATE_NOW")) {
                    this.state = AgentState.FINISHED;
                    break;
                }

                results.add(String.format("步骤 %d: %s", currentStep, stepResult));
            }

            if (currentStep >= maxSteps) {
                this.state = AgentState.FINISHED;
                results.add("⚠️ 任务强制终止：已达到最大思考步数 " + maxSteps);
            }

            return String.join("\n", results);

        } catch (RuntimeException e) {
            // 🚨 专门捕获“信号弹”异常
            // 检查异常信息是否包含我们的暗号（考虑到 Spring AI 可能会包装异常）
            if (isTerminationException(e)) {
                this.state = AgentState.FINISHED;
                log.info("🛑 [BaseAgent] 捕获到终止信号，任务成功结束！");
                results.add("🏁 任务达成，CodeManus 优雅退场。");
                return String.join("\n", results);
            }

            // 真正的错误
            this.state = AgentState.ERROR;
            log.error("💥 智能体崩溃: ", e);
            return "执行出错: " + e.getMessage();
        } catch (Exception e) {
            this.state = AgentState.ERROR;
            log.error("💥 智能体未知错误: ", e);
            return "执行出错: " + e.getMessage();
        } finally {
            this.state = AgentState.IDLE;
        }
    }

    // 辅助方法：递归检查异常原因
    private boolean isTerminationException(Throwable e) {
        if (e == null) return false;
        if ("TERMINATE_AGENT".equals(e.getMessage())) return true;
        return isTerminationException(e.getCause());
    }

    public abstract String step();
}