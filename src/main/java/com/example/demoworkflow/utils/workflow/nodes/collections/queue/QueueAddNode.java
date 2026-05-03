package com.example.demoworkflow.utils.workflow.nodes.collections.queue;

import com.example.demoworkflow.utils.types.ConfigTypes;
import com.example.demoworkflow.utils.types.NodeType;
import com.example.demoworkflow.utils.workflow.misc.PoolVariableRefResolver;
import com.example.demoworkflow.utils.workflow.misc.QueueNodeHelper;
import com.example.demoworkflow.utils.workflow.nodes.NodeImpl;
import com.example.demoworkflow.utils.workflow.pool.GlobalPool;
import com.example.demoworkflow.vo.ConfigVO;

import java.util.List;
import java.util.Queue;

public class QueueAddNode extends NodeImpl {
    public QueueAddNode(GlobalPool globalPool) {
        super(globalPool);
        setNodeType(NodeType.QUEUE_PUSH);
    }

    public QueueAddNode(GlobalPool globalPool, String nodeId) {
        super(globalPool, nodeId);
        setNodeType(NodeType.QUEUE_PUSH);
    }
    @Override
    public List<ConfigVO> getNodeConfigs() {
        return List.of(
                ConfigVO.builder()
                        .name("queue")
                        .des("队列变量；也可填字面量池键名")
                        .type(ConfigTypes.STRING)
                        .required(true)
                        .build(),
                ConfigVO.builder()
                        .name("value")
                        .des("要添加的元素")
                        .type(ConfigTypes.STRING)
                        .required(true)
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
            Queue<Object> next = QueueNodeHelper.readQueue(globalPool, token, queueVar);
            next.add(QueueNodeHelper.normalizeConfigValue(configs.get("value")));
            globalPool.put(token, queueVar, next);
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage();
            onNodeError(msg != null && !msg.isEmpty() ? msg : "队列添加失败");
        }
    }
}
