package com.pingyu.codehubbackend.agent;

import com.pingyu.codehubbackend.agent.model.AgentState;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * 智能体基类 (The Body)
 * 职责：管理状态、记忆、执行循环 (Loop)
 * 对应文档：五、自主实现 Manus 智能体 - 1、开发基础 Agent 类
 */
@Data
@Slf4j
public abstract class BaseAgent {

    // 智能体名称
    private String name;
    // 系统设定 (人设)
    private String systemPrompt;
    // 下一步提示 (用于引导 AI 持续思考)
    private String nextStepPrompt;

    // 当前状态
    private AgentState state = AgentState.IDLE;

    // 记忆条 (上下文历史)
    private List<Message> messageList = new ArrayList<>();

    // 循环控制
    private int maxSteps = 10; // 最大防止死循环次数
    private int currentStep = 0;

    // 大脑 (Spring AI ChatClient)
    private ChatClient chatClient;

    /**
     * 启动智能体 (主入口)
     */
    public String run(String userPrompt) {
        if (this.state != AgentState.IDLE) {
            throw new RuntimeException("智能体正在忙，请稍后再试！当前状态: " + this.state);
        }

        // 1. 初始化
        this.state = AgentState.RUNNING;
        this.currentStep = 0;
        this.messageList.clear(); // 每次运行清空短期记忆

        // 2. 注入用户任务
        this.messageList.add(new UserMessage(userPrompt));

        List<String> results = new ArrayList<>();

        try {
            log.info("🚀 [{}] 启动任务: {}", this.name, userPrompt);

            // 3. 进入 Agent Loop (执行循环)
            while (currentStep < maxSteps && state != AgentState.FINISHED) {
                currentStep++;
                log.info("🔄 Step {}/{}", currentStep, maxSteps);

                // 执行单步逻辑 (由子类实现)
                String stepResult = step();

                results.add(String.format("步骤 %d: %s", currentStep, stepResult));
            }

            // 4. 检查是否超时
            if (currentStep >= maxSteps) {
                this.state = AgentState.FINISHED;
                results.add("⚠️ 任务强制终止：已达到最大思考步数 " + maxSteps);
            }

            return String.join("\n", results);

        } catch (Exception e) {
            this.state = AgentState.ERROR;
            log.error("💥 智能体崩溃: ", e);
            return "执行出错: " + e.getMessage();
        } finally {
            // 归位
            this.state = AgentState.IDLE;
        }
    }

    /**
     * 单步执行逻辑 (核心抽象方法)
     */
    public abstract String step();
}