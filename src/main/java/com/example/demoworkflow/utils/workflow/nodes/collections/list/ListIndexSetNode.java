package com.example.demoworkflow.utils.workflow.nodes.collections.list;

import com.example.demoworkflow.utils.types.NodeType;
import com.example.demoworkflow.utils.workflow.misc.ListNodeHelper;
import com.example.demoworkflow.utils.workflow.misc.PoolVariableRefResolver;
import com.example.demoworkflow.utils.workflow.nodes.NodeImpl;
import com.example.demoworkflow.utils.workflow.pool.GlobalPool;
import com.example.demoworkflow.vo.ConfigVO;

import java.util.List;

/**
 * 按下标更新列表元素，更新后的列表写回变量池。
 */
public class ListIndexSetNode extends NodeImpl {

    public ListIndexSetNode(GlobalPool globalPool) {
        super(globalPool);
        setNodeType(NodeType.LIST_INDEX_SET);
    }

    public ListIndexSetNode(GlobalPool globalPool, String nodeId) {
        super(globalPool, nodeId);
        setNodeType(NodeType.LIST_INDEX_SET);
    }

    @Override
    public List<ConfigVO> getNodeConfigs() {
        return List.of(
                ConfigVO.builder()
                        .name("list")
                        .des("列表变量：推荐 {{变量名}}（与快捷插入一致）；也可填字面量池键名")
                        .type("String")
                        .required(true)
                        .build(),
                ConfigVO.builder()
                        .name("index")
                        .des("下标（从 0 开始）")
                        .type("Number")
                        .k(1)
                        .quantize(0)
                        .required(true)
                        .build(),
                ConfigVO.builder()
                        .name("value")
                        .des("新值（可与其它节点输出或模板配合）")
                        .type("String")
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
            List<Object> current = ListNodeHelper.readList(globalPool, token, listVar);
            int index = ListNodeHelper.parseIndex(configs.get("index"));
            ListNodeHelper.requireIndexInRange(index, current.size(), listVar);
            List<Object> next = ListNodeHelper.mutableCopy(current);
            next.set(index, ListNodeHelper.normalizeConfigValue(configs.get("value")));
            globalPool.put(token, listVar, next);
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage();
            onNodeError(msg != null && !msg.isEmpty() ? msg : "列表下标更新失败");
        }
    }
}
