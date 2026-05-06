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
 * 弹出队首元素并写回变量池，弹出的元素写入节点输出 {@code output}。
 */
public class QueuePopNode extends NodeImpl {

    public QueuePopNode(GlobalPool globalPool) {
        super(globalPool);
        setNodeType(NodeType.QUEUE_POP);
    }

    public QueuePopNode(GlobalPool globalPool, String nodeId) {
        super(globalPool, nodeId);
        setNodeType(NodeType.QUEUE_POP);
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
                .des("弹出的队首元素")
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
            Queue<Object> current = QueueNodeHelper.readQueue(globalPool, token, queueVar);
            Queue<Object> next = QueueNodeHelper.mutableCopy(current);
            Object head = QueueNodeHelper.pop(next, queueVar);
            globalPool.put(token, queueVar, next);
            nodePool.put("output", head);
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage();
            onNodeError(msg != null && !msg.isEmpty() ? msg : "队列弹出失败");
        }
    }
}
