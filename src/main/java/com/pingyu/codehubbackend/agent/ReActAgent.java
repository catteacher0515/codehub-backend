package com.pingyu.codehubbackend.agent;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * ReAct 模式代理 (The Brain Logic)
 * 职责：强制 "思考-行动" 循环
 * 对应文档：五、自主实现 Manus 智能体 - 2、开发 ReActAgent 类
 */
@EqualsAndHashCode(callSuper = true)
@Data
public abstract class ReActAgent extends BaseAgent {

    /**
     * 思考阶段 (Reasoning)
     * @return true=需要行动, false=思考结束/无需行动
     */
    public abstract boolean think();

    /**
     * 行动阶段 (Acting)
     * @return 行动结果描述
     */
    public abstract String act();

    @Override
    public String step() {
        // 1. 先思考
        boolean shouldAct = think();

        if (!shouldAct) {
            // AI 认为不需要行动了（可能已经得出答案，或者需要等待用户）
            return "🤔 思考完毕，无需额外物理行动。";
        }

        // 2. 后行动
        return act();
    }
}