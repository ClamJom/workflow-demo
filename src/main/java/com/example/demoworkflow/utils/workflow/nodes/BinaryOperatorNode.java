package com.example.demoworkflow.utils.workflow.nodes;

import com.example.demoworkflow.utils.workflow.pool.GlobalPool;

/**
 * 双目运算符，处理两个数值、变量之间的数学运算
 */
public class BinaryOperatorNode extends NodeImpl{
    public BinaryOperatorNode(GlobalPool globalPool) {
        super(globalPool);
    }

    public BinaryOperatorNode(GlobalPool globalPool, String nodeId) {
        super(globalPool, nodeId);
    }
}
