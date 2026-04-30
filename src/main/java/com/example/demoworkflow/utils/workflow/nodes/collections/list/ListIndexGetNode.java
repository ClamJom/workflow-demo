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
 * 按下标读取列表元素，结果写入节点输出 {@code output}。
 */
public class ListIndexGetNode extends NodeImpl {

    public ListIndexGetNode(GlobalPool globalPool) {
        super(globalPool);
        setNodeType(NodeType.LIST_INDEX_GET);
    }

    public ListIndexGetNode(GlobalPool globalPool, String nodeId) {
        super(globalPool, nodeId);
        setNodeType(NodeType.LIST_INDEX_GET);
    }

    @Override
    public List<ConfigVO> getNodeConfigs() {
        return List.of(
                ConfigVO.builder()
                        .name("list")
                        .des("列表变量；也可填字面量池键名")
                        .type(ConfigTypes.STRING)
                        .required(true)
                        .build(),
                ConfigVO.builder()
                        .name("index")
                        .des("下标（从 0 开始）")
                        .type(ConfigTypes.NUMBER)
                        .k(1)
                        .quantize(0)
                        .required(true)
                        .build());
    }

    @Override
    public List<OutputVariableDes> getNodeOutputs() {
        return List.of(OutputVariableDes.builder()
                .name("output")
                .des("该下标处的元素")
                .type("Object")
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
            int index = ListNodeHelper.parseIndex(configs.get("index"));
            ListNodeHelper.requireIndexInRange(index, list.size(), listVar);
            nodePool.put("output", list.get(index));
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage();
            onNodeError(msg != null && !msg.isEmpty() ? msg : "列表下标访问失败");
        }
    }
}
