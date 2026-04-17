package com.example.demoworkflow.vo;

import lombok.Data;

@Data
public class EdgeVO {
    public String from;

    public String to;

    /** 与前端 Vue Flow 的 sourceHandle 对应，多输出节点（如条件）按句柄区分分支；旧数据可为 null */
    public String fromHandle;
}
