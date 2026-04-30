package com.example.demoworkflow.utils.workflow.nodes.base;

import com.example.demoworkflow.utils.types.ConfigTypes;
import com.example.demoworkflow.utils.types.NodeType;
import com.example.demoworkflow.utils.workflow.nodes.NodeImpl;
import com.example.demoworkflow.utils.workflow.pool.GlobalPool;
import com.example.demoworkflow.vo.ConfigVO;

import java.util.List;

/**
 * 变量声明节点，向变量池声明没有节点ID标识的变量
 */
public class VariableAssignNode extends NodeImpl {
    public VariableAssignNode(GlobalPool globalPool) {
        super(globalPool);
        setNodeType(NodeType.VARIABLE_ASSIGN);
    }

    public VariableAssignNode(GlobalPool globalPool, String nodeId) {
        super(globalPool, nodeId);
        setNodeType(NodeType.VARIABLE_ASSIGN);
    }

    @Override
    public List<ConfigVO> getNodeConfigs(){
        return List.of(ConfigVO.builder()
                .name("name")
                .des("变量名")
                .type(ConfigTypes.STRING)
                .required(true)
                .build(),
        ConfigVO.builder()
                .name("value")
                .des("初始值")
                .value("0")
                .type(ConfigTypes.STRING)
                .build());
    }

    @Override
    public void run(){
        String variableName = (String) configs.get("name");
        assert variableName != null;
        Object value = configs.get("value");
        // 正常来说，节点不应该直接向globalPool插入值，但此处需要注册变量，因此破格使用这个方法
        globalPool.put(token, variableName, value);
    }
}
