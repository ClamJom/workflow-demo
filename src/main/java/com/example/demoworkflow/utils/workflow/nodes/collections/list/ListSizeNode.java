package com.example.demoworkflow.utils.workflow.nodes.collections.list;

import com.example.demoworkflow.utils.types.ConfigTypes;
import com.example.demoworkflow.utils.types.NodeType;
import com.example.demoworkflow.utils.workflow.dto.OutputVariableDes;
import com.example.demoworkflow.utils.workflow.misc.ListNodeHelper;
import com.example.demoworkflow.utils.workflow.misc.PoolVariableRefResolver;
import com.example.demoworkflow.utils.workflow.nodes.NodeImpl;
import com.example.demoworkflow.utils.workflow.pool.GlobalPool;
import com.example.demoworkflow.vo.ConfigVO;

import java.util.List;

/**
 * 获取列表长度，写入节点输出 {@code output}（整数）。
 */
public class ListSizeNode extends NodeImpl {

    public ListSizeNode(GlobalPool globalPool) {
        super(globalPool);
        setNodeType(NodeType.LIST_SIZE);
    }

    public ListSizeNode(GlobalPool globalPool, String nodeId) {
        super(globalPool, nodeId);
        setNodeType(NodeType.LIST_SIZE);
    }

    @Override
    public List<ConfigVO> getNodeConfigs() {
        return List.of(ConfigVO.builder()
                .name("list")
                .des("列表变量；也可填字面量池键名")
                .type(ConfigTypes.STRING)
                .required(true)
                .build());
    }

    @Override
    public List<OutputVariableDes> getNodeOutputs() {
        return List.of(OutputVariableDes.builder()
                .name("output")
                .des("列表元素个数")
                .type("Number")
                .build());
    }

    @Override
    public void run() {
        String listVar = PoolVariableRefResolver.resolvePoolKey(this, "list");
        if (listVar == null) {
            onNodeError("请填写有效的列表变量引用（如 {{变量名}}）或池键名");
            return;
        }
        try {
            List<Object> list = ListNodeHelper.readList(globalPool, token, listVar);
            nodePool.put("output", list.size());
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage();
            onNodeError(msg != null && !msg.isEmpty() ? msg : "获取列表长度失败");
        }
    }
}
