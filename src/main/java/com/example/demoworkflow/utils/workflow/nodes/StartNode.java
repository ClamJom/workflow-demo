package com.example.demoworkflow.utils.workflow.nodes;

import com.example.demoworkflow.utils.workflow.dto.OutputVariableDes;
import com.example.demoworkflow.utils.workflow.pool.GlobalPool;
import com.example.demoworkflow.vo.ConfigVO;

import java.util.List;

/**
 * 起始节点
 * 绝对不要在单个流程图中添加多个起始节点，这将导致程序无法判断流程图入口，可能从随机入口执行流程图
 */
public class StartNode extends NodeImpl {
    StartNode(GlobalPool globalPool){
        super(globalPool);
        this.setNodeType(NodeType.START);
    }

    StartNode(GlobalPool globalPool, String uuid){
        super(globalPool, uuid);
        this.setNodeType(NodeType.START);
    }

    @Override
    public List<ConfigVO> getNodeConfigs(){
        return List.of(ConfigVO.builder()
                .name("input")
                .type("Map")
                .value("{}")
                .des("输入变量")
                .required(false)
                .build());
    }

    @Override
    public List<OutputVariableDes> getNodeOutputs() {
        return List.of(OutputVariableDes.builder()
                        .name("output")
                        .type("Map")
                        .des("输出")
                .build());
    }

    @Override
    public void run(){
        configs.keySet().forEach(key->{
            nodePool.put(key, configs.get(key));
        });
    }
}
