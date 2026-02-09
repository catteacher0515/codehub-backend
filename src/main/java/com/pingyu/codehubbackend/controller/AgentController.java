package com.pingyu.codehubbackend.controller;

import com.pingyu.codehubbackend.agent.BaseAgent;
import com.pingyu.codehubbackend.agent.model.AgentEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api") // 注意：为了统一管理，建议把前缀改为 /api
@Slf4j
public class AgentController {

    private final BaseAgent codeManus;
    private final ChatClient simpleChatClient;

    // 创建一个线程池来执行耗时的 Agent 任务
    private final ExecutorService executor = Executors.newCachedThreadPool();

    // 构造注入：区分复杂的 Agent 和 简单的 ChatClient
    public AgentController(BaseAgent codeManus,
                           @Qualifier("simpleChatClient") ChatClient simpleChatClient) {
        this.codeManus = codeManus;
        this.simpleChatClient = simpleChatClient;
    }

    // ==========================================
    // 页面 1 接口: 智码助手 (轻量级对话)
    // ==========================================
    /**
     * 普通流式对话接口
     * 请求：GET /api/assistant/chat?prompt=xxx
     * 响应：text/event-stream (直接返回 Flux<String>)
     */
    @GetMapping(value = "/assistant/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> simpleChat(@RequestParam String prompt) {
        log.info("💬 [智码助手] 收到消息: {}", prompt);
        return simpleChatClient.prompt()
                .user(prompt)
                .stream()
                .content(); // 直接返回内容流，Spring Boot 会自动处理 SSE
    }

    // ==========================================
    // 页面 2 接口: CodeManus (超级智能体)
    // ==========================================
    /**
     * Agent 专用流式接口 (包含思考过程、工具调用等复杂事件)
     * 请求：GET /api/agent/chat?prompt=xxx
     * 响应：text/event-stream (返回 SseEmitter)
     */
    @GetMapping(value = "/agent/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter agentChat(@RequestParam String prompt) {
        // 设置超时时间为 5 分钟
        SseEmitter emitter = new SseEmitter(300000L);

        executor.execute(() -> {
            try {
                // 启动 CodeManus
                codeManus.run(prompt, event -> {
                    try {
                        // 发送自定义事件 (THINKING, ACTION, RESULT, ANSWER)
                        emitter.send(SseEmitter.event()
                                .name(event.getType())
                                .data(event));
                    } catch (Exception e) {
                        log.error("SSE 发送失败", e);
                        emitter.completeWithError(e);
                    }
                });
                emitter.complete();
            } catch (Exception e) {
                log.error("Agent 执行异常", e);
                try {
                    emitter.send(SseEmitter.event().name("ERROR").data("Server Error: " + e.getMessage()));
                } catch (Exception ignored) {}
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }
}