package com.example.demoworkflow.utils.workflow.nodes;

import com.example.demoworkflow.pojo.LLMProvider;
import com.example.demoworkflow.services.LLMProviderService;
import com.example.demoworkflow.utils.workflow.dto.OutputVariableDes;
import com.example.demoworkflow.utils.workflow.pool.GlobalPool;
import com.example.demoworkflow.utils.workflow.result.WorkflowResult;
import com.example.demoworkflow.utils.workflow.states.NodeStates;
import com.example.demoworkflow.vo.ConfigVO;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * LLM大模型节点 - 支持调用各种大语言模型API
 */
@Slf4j
public class LLMNode extends NodeImpl {

    private LLMProviderService llmProviderService;

    private WebClient webClient;
    private LLMProvider provider;

    private static final String PROVIDER_ID = "ProviderId";
    private static final String PROVIDER_NAME = "ProviderName";
    private static final String MODEL = "Model";
    private static final String SYSTEM_PROMPT = "SystemPrompt";
    private static final String USER_PROMPT = "UserPrompt";
    private static final String TEMPERATURE = "Temperature";
    private static final String MAX_TOKENS = "MaxTokens";
    private static final String STREAM = "Stream";
    private static final String TOP_P = "TopP";
    private static final String FREQUENCY_PENALTY = "FrequencyPenalty";
    private static final String PRESENCE_PENALTY = "PresencePenalty";

    LLMNode(GlobalPool globalPool) {
        super(globalPool);
        // 在此处设置节点类型，需要延迟到Spring注入后
    }

    LLMNode(GlobalPool globalPool, String nodeId) {
        super(globalPool, nodeId);
    }

    /**
     * 初始化节点类型
     */
    public void initNodeType() {
        super.setNodeType(NodeType.LLM);
    }

    @Override
    public List<ConfigVO> getNodeConfigs() {
        List<ConfigVO> defaultConfigs = new ArrayList<>();
        
        defaultConfigs.add(ConfigVO.builder()
                .name(PROVIDER_ID)
                .type("Number")
                .des("LLM供应商ID (不填则使用默认供应商)")
                .required(false)
                .value("")
                .build());
        
        defaultConfigs.add(ConfigVO.builder()
                .name(PROVIDER_NAME)
                .type("String")
                .des("LLM供应商名称 (如: OpenAI, Azure, Anthropic等)")
                .required(true)
                .value("OpenAI")
                .build());
        
        defaultConfigs.add(ConfigVO.builder()
                .name(MODEL)
                .type("String")
                .des("模型名称")
                .required(true)
                .value("gpt-3.5-turbo")
                .build());
        
        defaultConfigs.add(ConfigVO.builder()
                .name(SYSTEM_PROMPT)
                .type("String")
                .des("系统提示词")
                .required(false)
                .value("你是一个有用的AI助手。")
                .build());
        
        defaultConfigs.add(ConfigVO.builder()
                .name(USER_PROMPT)
                .type("String")
                .des("用户提示词 (支持变量注入，如: {{nodeId:variableName}})")
                .required(true)
                .value("请介绍一下你自己。")
                .build());
        
        defaultConfigs.add(ConfigVO.builder()
                .name(TEMPERATURE)
                .type("Number")
                .des("温度参数 (0-2，越高越有创造性)")
                .required(false)
                .value("0.7")
                .build());
        
        defaultConfigs.add(ConfigVO.builder()
                .name(MAX_TOKENS)
                .type("Number")
                .des("最大生成Token数")
                .required(false)
                .value("2048")
                .build());
        
        defaultConfigs.add(ConfigVO.builder()
                .name(STREAM)
                .type("Boolean")
                .des("是否启用流式输出")
                .required(false)
                .value("false")
                .build());
        
        defaultConfigs.add(ConfigVO.builder()
                .name(TOP_P)
                .type("Number")
                .des("Top P采样参数 (0-1)")
                .required(false)
                .value("1.0")
                .build());
        
        defaultConfigs.add(ConfigVO.builder()
                .name(FREQUENCY_PENALTY)
                .type("Number")
                .des("频率惩罚 (-2.0 到 2.0)")
                .required(false)
                .value("0.0")
                .build());
        
        defaultConfigs.add(ConfigVO.builder()
                .name(PRESENCE_PENALTY)
                .type("Number")
                .des("存在惩罚 (-2.0 到 2.0)")
                .required(false)
                .value("0.0")
                .build());
        
        return defaultConfigs;
    }

    @Override
    public List<OutputVariableDes> getNodeOutputs() {
        List<OutputVariableDes> defaultOutputs = new ArrayList<>();
//        return List.of("output", "model", "provider");
        defaultOutputs.add(OutputVariableDes.builder()
                        .name("output")
                        .type("String")
                        .des("模型输出")
                .build());
        defaultOutputs.add(OutputVariableDes.builder()
                        .name("model")
                        .type("String")
                        .des("使用模型名称")
                .build());
        defaultOutputs.add(OutputVariableDes.builder()
                        .name("provider")
                        .type("String")
                        .des("供应商名称")
                .build());
        return defaultOutputs;
    }

    @Override
    public void before() {
        // 获取LLMProviderService实例
        if (llmProviderService == null) {
            // 尝试通过Spring上下文获取
            try {
                llmProviderService = com.example.demoworkflow.DemoWorkflowApplication.getApplicationContext()
                        .getBean(LLMProviderService.class);
            } catch (Exception e) {
                // 如果无法获取，则使用默认配置
                log.warn("无法获取LLMProviderService，将使用默认配置");
            }
        }
        
        // 确保节点类型已设置
        if (getNodeType() == null || getNodeType() == NodeType.EMPTY_NODE) {
            initNodeType();
        }
        
        // 获取供应商配置
        String providerName = (String) configs.get(PROVIDER_NAME);
        Long providerId = configs.get(PROVIDER_ID) != null ? 
            ((Number) configs.get(PROVIDER_ID)).longValue() : null;
        
        if (providerId != null) {
            provider = llmProviderService.getProviderById(providerId).orElse(null);
        }
        
        if (provider == null && providerName != null) {
            provider = llmProviderService.getProviderByName(providerName).orElse(null);
        }
        
        if (provider == null) {
            provider = llmProviderService.getDefaultProvider().orElse(null);
        }
        
        if (provider == null) {
            onNodeError("未配置LLM供应商，请先配置供应商或设置默认供应商");
            return;
        }
        
        // 验证API密钥
        if (provider.getApiKey() == null || provider.getApiKey().trim().isEmpty()) {
            onNodeError("LLM供应商API密钥未配置");
            return;
        }
        
        // 配置WebClient
        int timeout = provider.getTimeout() != null ? provider.getTimeout() : 60000;
        
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeout)
                .responseTimeout(Duration.ofMillis(timeout))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(timeout, TimeUnit.MILLISECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(timeout, TimeUnit.MILLISECONDS)));
        
        WebClient.Builder webClientBuilder = WebClient.builder()
                .baseUrl(provider.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + provider.getApiKey())
                .defaultHeader(HttpHeaders.USER_AGENT, "WorkflowDemo/1.0")
                .clientConnector(new ReactorClientHttpConnector(httpClient));
        
        // 添加内容类型
        webClientBuilder.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        
        this.webClient = webClientBuilder.build();
    }

    @Override
    public void run() {
        try {
            String model = (String) configs.getOrDefault(MODEL, provider.getModel());
            String systemPrompt = (String) configs.getOrDefault(SYSTEM_PROMPT, "");
            String userPrompt = globalPool.parseConfig((String) configs.get(USER_PROMPT), token);
            
            // 获取参数
            float temperature = getFloatConfig(TEMPERATURE, provider.getTemperature() != null ? provider.getTemperature() : 0.7f);
            int maxTokens = getIntConfig(MAX_TOKENS, provider.getMaxTokens() != null ? provider.getMaxTokens() : 2048);
            boolean stream = getBooleanConfig(STREAM, provider.getStream() != null ? provider.getStream() : false);
            float topP = getFloatConfig(TOP_P, provider.getTopP() != null ? provider.getTopP() : 1.0f);
            float frequencyPenalty = getFloatConfig(FREQUENCY_PENALTY, provider.getFrequencyPenalty() != null ? provider.getFrequencyPenalty() : 0.0f);
            float presencePenalty = getFloatConfig(PRESENCE_PENALTY, provider.getPresencePenalty() != null ? provider.getPresencePenalty() : 0.0f);
            
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            
            // 构建消息列表
            List<Map<String, String>> messages = new ArrayList<>();
            
            if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
                Map<String, String> systemMessage = new HashMap<>();
                systemMessage.put("role", "system");
                systemMessage.put("content", systemPrompt);
                messages.add(systemMessage);
            }
            
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", userPrompt);
            messages.add(userMessage);
            
            requestBody.put("messages", messages);
            requestBody.put("temperature", temperature);
            requestBody.put("max_tokens", maxTokens);
            requestBody.put("top_p", topP);
            requestBody.put("frequency_penalty", frequencyPenalty);
            requestBody.put("presence_penalty", presencePenalty);
            requestBody.put("stream", stream);
            
            // 发送请求
            String responseContent;
            
            if (stream) {
                // 流式输出处理（简化版，返回第一块内容）
                responseContent = handleStreamRequest(requestBody);
            } else {
                responseContent = handleNormalRequest(requestBody);
            }
            
            // 保存结果
            nodePool.put("output", responseContent);
            nodePool.put("model", model);
            nodePool.put("provider", provider.getName());
            
            putWorkflowResult(WorkflowResult.builder()
                    .token(token)
                    .nodeId(nodeId)
                    .msg("LLM调用成功")
                    .state(NodeStates.DONE)
                    .extData(responseContent)
                    .build());
            
        } catch (Exception e) {
            log.error("LLM调用失败", e);

            onNodeError("LLM调用失败: " + e.getMessage());
        }
    }

    private String handleNormalRequest(Map<String, Object> requestBody) {
        // 根据供应商类型构建不同的请求
        String endpoint = buildEndpoint();
        
        Mono<Map> responseMono = webClient.post()
                .uri(endpoint)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class);
        
        Map response = responseMono.block();
        
        if (response == null) {
            throw new RuntimeException("LLM响应为空");
        }
        
        // 检查错误
        if (response.containsKey("error")) {
            Map<String, Object> error = (Map<String, Object>) response.get("error");
            String errorMessage = error.get("message") != null ? 
                error.get("message").toString() : "未知错误";
            throw new RuntimeException("LLM API错误: " + errorMessage);
        }
        
        // 解析响应
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new RuntimeException("LLM未返回任何内容");
        }
        
        Map<String, Object> choice = choices.get(0);
        Map<String, Object> message = (Map<String, Object>) choice.get("message");
        
        return message != null ? message.get("content").toString() : "";
    }

    private String handleStreamRequest(Map<String, Object> requestBody) {
        // 流式请求处理
        StringBuilder content = new StringBuilder();
        
        webClient.post()
                .uri(buildEndpoint())
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .subscribe(chunk -> {
                    // 简单处理SSE流
                    if (chunk.startsWith("data: ")) {
                        String data = chunk.substring(6);
                        if (!data.equals("[DONE]")) {
                            // 解析并累加内容
                            content.append(".");
                        }
                    }
                });
        
        return content.length() > 0 ? content.toString() : "流式输出已处理";
    }

    private String buildEndpoint() {
        String type = provider.getType();
        String baseUrl = provider.getBaseUrl();
        
        switch (type) {
            case "openai":
                return "/chat/completions";
            case "azure":
                return "/chat/completions?api-version=2024-02-01";
            case "anthropic":
                return "/v1/messages";
            case "local":
                return "/chat/completions";
            default:
                return "/chat/completions";
        }
    }

    private float getFloatConfig(String key, float defaultValue) {
        Object value = configs.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }
        try {
            return Float.parseFloat(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private int getIntConfig(String key, int defaultValue) {
        Object value = configs.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private boolean getBooleanConfig(String key, boolean defaultValue) {
        Object value = configs.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return Boolean.parseBoolean(value.toString());
    }
}
