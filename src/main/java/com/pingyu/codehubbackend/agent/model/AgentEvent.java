package com.pingyu.codehubbackend.agent.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AgentEvent {
    // 事件类型: "THINKING"(思考), "ACTION"(行动), "RESULT"(结果/观察), "ANSWER"(最终答案/终止), "ERROR"(错误)
    private String type;

    // 事件内容
    private String content;

    // 可选：元数据 (例如耗时、工具名等)
    private String meta;

    public static AgentEvent thinking(String text) {
        return new AgentEvent("THINKING", text, null);
    }

    public static AgentEvent action(String toolName, String args) {
        return new AgentEvent("ACTION", args, toolName);
    }

    public static AgentEvent result(String output) {
        return new AgentEvent("RESULT", output, null);
    }

    public static AgentEvent answer(String text) {
        return new AgentEvent("ANSWER", text, null);
    }

    public static AgentEvent error(String msg) {
        return new AgentEvent("ERROR", msg, null);
    }
}