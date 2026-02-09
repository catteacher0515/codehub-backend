package com.pingyu.codehubbackend.controller;

import com.pingyu.codehubbackend.agent.BaseAgent;
import com.pingyu.codehubbackend.agent.model.AgentEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/agent")
@Slf4j
public class AgentController {

    private final BaseAgent codeManus;
    // 创建一个线程池来执行耗时的 Agent 任务，避免阻塞 HTTP 线程
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public AgentController(BaseAgent codeManus) {
        this.codeManus = codeManus;
    }

    /**
     * SSE 流式聊天接口
     * 请求：GET /api/agent/chat?prompt=xxx
     * 响应：text/event-stream
     */
    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestParam String prompt) {
        // 设置超时时间为 5 分钟 (300000ms)，因为 Agent 思考和联网可能很慢
        SseEmitter emitter = new SseEmitter(300000L);

        // 在后台线程执行 Agent，防止阻塞 Servlet 线程
        executor.execute(() -> {
            try {
                // 启动 CodeManus，并传入监听器
                codeManus.run(prompt, event -> {
                    try {
                        // 将事件发送给前端
                        // id: 自动生成, name: 事件类型, data: 事件内容
                        emitter.send(SseEmitter.event()
                                .name(event.getType())
                                .data(event));
                    } catch (Exception e) {
                        log.error("SSE 发送失败", e);
                        emitter.completeWithError(e);
                    }
                });

                // 任务自然结束
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