package com.example.demoworkflow.utils.workflow.nodes.collections.queue;

import com.example.demoworkflow.utils.types.ConfigTypes;
import com.example.demoworkflow.utils.types.NodeType;
import com.example.demoworkflow.utils.workflow.dto.OutputVariableDes;
import com.example.demoworkflow.utils.workflow.misc.PoolVariableRefResolver;
import com.example.demoworkflow.utils.workflow.misc.QueueNodeHelper;
import com.example.demoworkflow.utils.workflow.nodes.NodeImpl;
import com.example.demoworkflow.utils.workflow.pool.GlobalPool;
import com.example.demoworkflow.vo.ConfigVO;

import java.util.List;
import java.util.Queue;

/**
 * 查看队首元素但不移除，结果写入节点输出 {@code output}。
 */
public class QueuePeekNode extends NodeImpl {

    public QueuePeekNode(GlobalPool globalPool) {
        super(globalPool);
        setNodeType(NodeType.QUEUE_PEEK);
    }

    public QueuePeekNode(GlobalPool globalPool, String nodeId) {
        super(globalPool, nodeId);
        setNodeType(NodeType.QUEUE_PEEK);
    }

    @Override
    public List<ConfigVO> getNodeConfigs() {
        return List.of(ConfigVO.builder()
                .name("queue")
                .des("队列变量；也可填字面量池键名")
                .type(ConfigTypes.STRING)
                .required(true)
                .build());
    }

    @Override
    public List<OutputVariableDes> getNodeOutputs() {
        return List.of(OutputVariableDes.builder()
                .name("output")
                .des("队首元素")
                .type("Object")
                .build());
    }

    @Override
    public void run() {
        String queueVar = PoolVariableRefResolver.resolvePoolKey(this, "queue");
        if (queueVar == null) {
            onNodeError("请填写有效的队列变量引用（如 {{变量名}}）或池键名");
            return;
        }
        try {
            Queue<Object> queue = QueueNodeHelper.readQueue(globalPool, token, queueVar);
            nodePool.put("output", QueueNodeHelper.peek(queue, queueVar));
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage();
            onNodeError(msg != null && !msg.isEmpty() ? msg : "队列查询失败");
        }
    }
}
