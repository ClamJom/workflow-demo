package com.example.demoworkflow.utils.workflow.nodes.collections.list;

import com.example.demoworkflow.utils.types.ConfigTypes;
import com.example.demoworkflow.utils.types.NodeType;
import com.example.demoworkflow.utils.workflow.misc.ListNodeHelper;
import com.example.demoworkflow.utils.workflow.misc.PoolVariableRefResolver;
import com.example.demoworkflow.utils.workflow.nodes.NodeImpl;
import com.example.demoworkflow.utils.workflow.pool.GlobalPool;
import com.example.demoworkflow.vo.ConfigVO;

import java.util.ArrayList;
import java.util.List;

/**
 * 清空列表（置为空列表），写回变量池。
 */
public class ListClearNode extends NodeImpl {

    public ListClearNode(GlobalPool globalPool) {
        super(globalPool);
        setNodeType(NodeType.LIST_CLEAR);
    }

    public ListClearNode(GlobalPool globalPool, String nodeId) {
        super(globalPool, nodeId);
        setNodeType(NodeType.LIST_CLEAR);
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
    public void run() {
        String listVar = PoolVariableRefResolver.resolvePoolKey(this, "list");
        if (listVar == null) {
            onNodeError("请填写有效的列表变量引用（如 {{变量名}}）或池键名");
            return;
        }
        try {
            ListNodeHelper.readList(globalPool, token, listVar);
            globalPool.put(token, listVar, new ArrayList<>());
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage();
            onNodeError(msg != null && !msg.isEmpty() ? msg : "列表清空失败");
        }
    }
}
