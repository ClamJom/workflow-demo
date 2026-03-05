package com.example.demoworkflow.pojo;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * LLM模型供应商配置
 * 支持OpenAI规范的各种大模型API
 */
@Entity
@Table(name = "llm_provider")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LLMProvider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 供应商名称
     */
    @Column(nullable = false)
    private String name;

    /**
     * 供应商类型: openai, azure, anthropic, google, local, custom等
     */
    @Column(nullable = false)
    private String type;

    /**
     * API基础URL
     */
    @Column(columnDefinition = "TEXT")
    private String baseUrl;

    /**
     * API密钥
     */
    @Column(columnDefinition = "TEXT")
    private String apiKey;

    /**
     * 模型名称
     */
    @Column
    private String model;

    /**
     * 默认超时时间（毫秒）
     */
    @Column
    private Integer timeout;

    /**
     * 最大Token数
     */
    @Column
    private Integer maxTokens;

    /**
     * 温度参数（0-2）
     */
    @Column
    private Float temperature;

    /**
     * 顶级P参数
     */
    @Column
    private Float topP;

    /**
     * 频率惩罚
     */
    @Column
    private Float frequencyPenalty;

    /**
     * 存在惩罚
     */
    @Column
    private Float presencePenalty;

    /**
     * 是否启用流式输出
     */
    @Column
    private Boolean stream;

    /**
     * 额外请求头（JSON格式）
     */
    @Column(columnDefinition = "TEXT")
    private String extraHeaders;

    /**
     * 额外请求参数（JSON格式）
     */
    @Column(columnDefinition = "TEXT")
    private String extraParams;

    /**
     * 供应商描述
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * 是否为默认供应商
     */
    @Column
    private Boolean isDefault;

    /**
     * 供应商状态: active, inactive
     */
    @Column
    private String status;

    /**
     * 创建时间
     */
    @Column
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Column
    private LocalDateTime updateTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
        if (status == null) {
            status = "active";
        }
        if (stream == null) {
            stream = false;
        }
        if (isDefault == null) {
            isDefault = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}
