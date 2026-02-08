package com.pingyu.codehubbackend.agent.model;

/**
 * 智能体生命周期状态枚举
 * 对应文档：五、自主实现 Manus 智能体 - 定义数据模型
 */
public enum AgentState {
    /** 空闲中 */
    IDLE,
    /** 正在思考或行动 */
    RUNNING,
    /** 任务圆满完成 */
    FINISHED,
    /** 发生异常 */
    ERROR
}