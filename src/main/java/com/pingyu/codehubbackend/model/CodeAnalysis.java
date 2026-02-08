package com.pingyu.codehubbackend.model;

import java.util.List;

/**
 * 智码 - 代码诊断报告 (结构化数据)
 * 作用：强制 AI 按照这个格式交卷，方便后续处理
 */
public record CodeAnalysis(
        String title,          // 诊断标题 (例如：空指针异常分析)
        int score,             // 代码健康度评分 (0-100)
        List<String> issues,   // 发现的问题列表
        String analysis,       // 详细侦探分析 (Root Cause)
        String improvedCode    // 修复后的完整代码
) {}