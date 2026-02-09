package com.pingyu.codehubbackend.agent;

import com.pingyu.codehubbackend.agent.model.AgentEvent;
import com.pingyu.codehubbackend.agent.model.AgentState;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

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

    // 默认的空监听器 (防止空指针)
    private Consumer<AgentEvent> eventListener = event -> {};

    /**
     * 启动智能体 (带监听器版本) - Controller 用这个
     */
    public void run(String userPrompt, Consumer<AgentEvent> listener) {
        this.eventListener = listener; // 注入监听器
        run(userPrompt);
    }

    /**
     * 启动智能体 (兼容旧版本) - Runner 用这个
     */
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
            // 推送开始事件
            notify(AgentEvent.thinking("🚀 任务启动: " + userPrompt));

            while (currentStep < maxSteps && state != AgentState.FINISHED) {
                currentStep++;
                log.info("🔄 Step {}/{}", currentStep, maxSteps);
                notify(AgentEvent.thinking("🔄 进入第 " + currentStep + " 步思考..."));

                // 执行单步逻辑 (传入监听器)
                String stepResult = step();

                if (state == AgentState.FINISHED || (stepResult != null && stepResult.contains("TERMINATE_NOW"))) {
                    log.info("🛑 [BaseAgent] 检测到任务完成信号。");
                    this.state = AgentState.FINISHED;
                    // 这里的 stepResult 可能是 terminate 的原因，作为最终答案推送
                    results.add("🏁 任务达成。");
                    break;
                }
                results.add(String.format("步骤 %d: %s", currentStep, stepResult));
            }

            if (currentStep >= maxSteps) {
                this.state = AgentState.FINISHED;
                String msg = "⚠️ 任务强制终止：已达到最大思考步数 " + maxSteps;
                results.add(msg);
                notify(AgentEvent.error(msg));
            }

            return String.join("\n", results);

        } catch (RuntimeException e) {
            if (isTerminationException(e)) {
                this.state = AgentState.FINISHED;
                log.info("🛑 [BaseAgent] 捕获到终止异常，任务结束。");
                return String.join("\n", results);
            }
            this.state = AgentState.ERROR;
            log.error("💥 智能体崩溃: ", e);
            notify(AgentEvent.error("执行出错: " + e.getMessage()));
            return "执行出错: " + e.getMessage();
        } catch (Exception e) {
            this.state = AgentState.ERROR;
            log.error("💥 未知错误: ", e);
            notify(AgentEvent.error("未知错误: " + e.getMessage()));
            return "执行出错: " + e.getMessage();
        } finally {
            this.state = AgentState.IDLE;
        }
    }

    // 辅助方法：发送通知
    protected void notify(AgentEvent event) {
        if (eventListener != null) {
            try {
                eventListener.accept(event);
            } catch (Exception e) {
                log.warn("发送事件失败: {}", e.getMessage());
            }
        }
    }

    private boolean isTerminationException(Throwable e) {
        if (e == null) return false;
        if (e.getMessage() != null && e.getMessage().contains("TERMINATE_AGENT")) return true;
        return isTerminationException(e.getCause());
    }

    public abstract String step();
}