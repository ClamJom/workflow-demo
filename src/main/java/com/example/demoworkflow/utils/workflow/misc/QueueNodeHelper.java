package com.example.demoworkflow.utils.workflow.misc;

import com.alibaba.fastjson2.JSON;
import com.example.demoworkflow.utils.workflow.pool.GlobalPool;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;

/**
 * 工作流队列节点：从变量池读取队列、规范化与可变副本。
 */
public class QueueNodeHelper {

    private QueueNodeHelper() {}

    /**
     * 将配置中的队列名称转为字符串并校验。
     */
    public static String normalizeQueueName(Object queueNameObj) {
        if (queueNameObj == null) {
            throw new IllegalArgumentException("队列名称不能为 null");
        }
        if (queueNameObj instanceof String s) {
            if (s.isBlank()) {
                throw new IllegalArgumentException("队列名称不能为空");
            }
            return s.trim();
        }
        return String.valueOf(queueNameObj).trim();
    }

    @SuppressWarnings("unchecked")
    public static Queue<Object> readQueue(GlobalPool globalPool, String token, String queueVarName) {
        Object raw = globalPool.get(token, queueVarName);
        if (raw == null) {
            throw new IllegalArgumentException("队列变量不存在: " + queueVarName);
        }
        if (raw instanceof Queue) {
            return (Queue<Object>) raw;
        }
        if (raw instanceof Collection<?> collection) {
            return new LinkedList<>(collection);
        }
        if (raw instanceof String s) {
            return parseStringQueue(s, queueVarName);
        }
        throw new IllegalArgumentException("变量不是队列类型: " + queueVarName);
    }

    /**
     * 返回可变副本，写回变量池时应使用 {@link GlobalPool#put(String, String, Object)} 覆盖原变量。
     */
    public static Queue<Object> mutableCopy(Queue<?> source) {
        return new LinkedList<>(source);
    }

    /**
     * 读取用于写入：变量不存在时得到空队列；存在但非 Queue 则抛错。
     */
    public static Queue<Object> readQueueForWrite(GlobalPool globalPool, String token, String queueVarName) {
        Object raw = globalPool.get(token, queueVarName);
        if (raw == null) {
            return new LinkedList<>();
        }
        if (raw instanceof Queue<?> queue) {
            return mutableCopy(queue);
        }
        if (raw instanceof Collection<?> collection) {
            return new LinkedList<>(collection);
        }
        if (raw instanceof String s) {
            return parseStringQueue(s, queueVarName);
        }
        throw new IllegalArgumentException("变量不是队列类型: " + queueVarName);
    }

    private static Queue<Object> parseStringQueue(String raw, String queueVarName) {
        String value = raw == null ? "" : raw.trim();
        if (!value.startsWith("[") || !value.endsWith("]")) {
            throw new IllegalArgumentException("变量不是队列类型: " + queueVarName);
        }
        try {
            return new LinkedList<>(JSON.parseArray(value, Object.class));
        } catch (Exception e) {
            throw new IllegalArgumentException("变量不是队列类型: " + queueVarName);
        }
    }

    /**
     * 读取队首元素但不移除；队列为空时抛错。
     */
    public static Object peek(Queue<Object> queue, String queueVarName) {
        Object head = queue.peek();
        if (head == null) {
            throw new IllegalArgumentException("队列为空: " + queueVarName);
        }
        return head;
    }

    /**
     * 弹出队首元素；队列为空时抛错。
     */
    public static Object pop(Queue<Object> queue, String queueVarName) {
        Object head = queue.poll();
        if (head == null) {
            throw new IllegalArgumentException("队列为空: " + queueVarName);
        }
        return head;
    }

    /**
     * 将节点配置中的标量（多为 String）尽量还原为 JSON 数字/布尔/对象/数组，便于写入队列；
     * 无法解析时保留原字符串。
     */
    public static Object normalizeConfigValue(Object raw) {
        return ListNodeHelper.normalizeConfigValue(raw);
    }
}
