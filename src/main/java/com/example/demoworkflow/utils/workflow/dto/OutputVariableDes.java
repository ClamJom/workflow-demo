package com.example.demoworkflow.utils.workflow.dto;

import lombok.Builder;

/**
 * 变量描述，用于传递变量的名称、类型、简介等数据
 */
@Builder
public class OutputVariableDes {
    public String name;

    public String des;

    public String type;
}
