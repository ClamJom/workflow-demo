package com.example.demoworkflow.vo;

import lombok.Data;

@Data
public class EdgeVO {
    public String from;

    public String to;

    /**
     * 源节点的 Handle ID（用于条件节点等多输出节点）
     */
    public String sourceHandle;

    /**
     * 目标节点的 Handle ID
     */
    public String targetHandle;
}
