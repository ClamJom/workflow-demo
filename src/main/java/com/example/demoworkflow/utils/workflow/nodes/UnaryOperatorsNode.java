package com.example.demoworkflow.utils.workflow.nodes;

import com.example.demoworkflow.utils.workflow.pool.GlobalPool;

/**
 * 单目运算符，处理数字、变量的自增、自减、位操作
 */
public class UnaryOperatorsNode extends NodeImpl{
    public UnaryOperatorsNode(GlobalPool globalPool) {
        super(globalPool);
    }

    public UnaryOperatorsNode(GlobalPool globalPool, String nodeId) {
        super(globalPool, nodeId);
    }
}
