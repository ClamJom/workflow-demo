package com.example.demoworkflow.utils.workflow.handler;

import com.alibaba.fastjson2.JSON;
import com.example.demoworkflow.utils.workflow.dto.Workflow;
import com.example.demoworkflow.utils.workflow.pool.GlobalPool;
import com.example.demoworkflow.utils.workflow.result.WorkflowResult;
import com.example.demoworkflow.utils.workflow.states.NodeStates;
import com.example.demoworkflow.utils.workflow.states.WorkflowStates;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE结果处理器 - 用于支持Server-Sent Events实时推送工作流执行结果
 */
@Service
@Slf4j
public class SseResultHandler {

    /**
     * 存储所有活跃的SSE连接
     */
    private final Map<String, List<SseEmitter>> sseEmitters = new ConcurrentHashMap<>();

    /**
     * 添加SSE连接
     * @param token 工作流token
     * @param emitter SSE发射器
     */
    public void addSseEmitter(String token, SseEmitter emitter) {
        sseEmitters.computeIfAbsent(token, k -> new ArrayList<>()).add(emitter);
        
        // 处理连接完成和超时
        emitter.onCompletion(() -> {
            removeSseEmitter(token, emitter);
            log.info("SSE连接完成: {}", token);
        });
        
        emitter.onTimeout(() -> {
            removeSseEmitter(token, emitter);
            log.info("SSE连接超时: {}", token);
        });
        
        emitter.onError(e -> {
            removeSseEmitter(token, emitter);
            log.error("SSE连接错误: {}", token, e);
        });
        
        log.info("新增SSE连接: {}, 当前连接数: {}", token, sseEmitters.get(token).size());
    }

    /**
     * 移除SSE连接
     * @param token 工作流token
     * @param emitter SSE发射器
     */
    public void removeSseEmitter(String token, SseEmitter emitter) {
        List<SseEmitter> emitters = sseEmitters.get(token);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                sseEmitters.remove(token);
            }
        }
    }

    /**
     * 通过SSE发送工作流结果
     * @param token 工作流token
     * @param result 工作流结果
     */
    public void sendResult(String token, WorkflowResult result) {
        List<SseEmitter> emitters = sseEmitters.get(token);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        
        String jsonData = JSON.toJSONString(result);
        SseEmitter.SseEventBuilder eventBuilder = SseEmitter.event()
                .id(String.valueOf(System.currentTimeMillis()))
                .name("workflow-result")
                .data(jsonData);
        
        List<SseEmitter> deadEmitters = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(eventBuilder);
            } catch (IOException e) {
                deadEmitters.add(emitter);
                log.error("发送SSE消息失败: {}", token, e);
            }
        }
        
        // 清理断开的连接
        for (SseEmitter dead : deadEmitters) {
            removeSseEmitter(token, dead);
        }
    }

    /**
     * 发送工作流状态更新
     * @param token 工作流token
     * @param state 状态码
     * @param msg 状态消息
     */
    public void sendStateUpdate(String token, int state, String msg) {
        WorkflowResult result = WorkflowResult.builder()
                .token(token)
                .state(state)
                .msg(msg)
                .build();
        sendResult(token, result);
    }

    /**
     * 发送心跳保持连接
     * @param token 工作流token
     */
    public void sendHeartbeat(String token) {
        List<SseEmitter> emitters = sseEmitters.get(token);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        
        SseEmitter.SseEventBuilder eventBuilder = SseEmitter.event()
                .id(String.valueOf(System.currentTimeMillis()))
                .name("heartbeat")
                .data("{\"ping\":\"pong\"}");
        
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(eventBuilder);
            } catch (IOException e) {
                removeSseEmitter(token, emitter);
            }
        }
    }

    /**
     * 获取指定token的SSE连接数
     * @param token 工作流token
     * @return 连接数
     */
    public int getSseEmitterCount(String token) {
        List<SseEmitter> emitters = sseEmitters.get(token);
        return emitters != null ? emitters.size() : 0;
    }

    /**
     * 关闭指定token的所有SSE连接
     * @param token 工作流token
     */
    public void closeAllSseEmitters(String token) {
        List<SseEmitter> emitters = sseEmitters.remove(token);
        if (emitters != null) {
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.complete();
                } catch (Exception e) {
                    log.error("关闭SSE连接失败: {}", token, e);
                }
            }
        }
    }

    /**
     * 判断指定token是否有活跃的SSE连接
     * @param token 工作流token
     * @return 是否有活跃连接
     */
    public boolean hasActiveConnection(String token) {
        List<SseEmitter> emitters = sseEmitters.get(token);
        return emitters != null && !emitters.isEmpty();
    }
}
