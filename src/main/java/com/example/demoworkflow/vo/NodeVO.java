package com.example.demoworkflow.vo;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class NodeVO {
    @NotBlank
    @NotNull
    @Size(max=255, min=1, message="节点ID不允许为空")
    public String id;

    @Min(value=0, message="节点类型不允许为空")
    public int type;

    public List<ConfigVO> configs;

    /**
     * 节点在画布上的位置，格式：{"x": 100, "y": 200}
     */
    public Map<String, Object> position;

    /**
     * 节点的显示标签
     */
    public String label;
}
