package com.example.demoworkflow.utils.workflow.nodes;

import com.alibaba.fastjson2.JSON;
import com.example.demoworkflow.utils.workflow.dto.OutputVariableDes;
import com.example.demoworkflow.utils.workflow.pool.GlobalPool;
import com.example.demoworkflow.utils.workflow.result.WorkflowResult;
import com.example.demoworkflow.vo.ConfigVO;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 网络请求节点
 */
public class HTTPRequestNode extends NodeImpl{

    private WebClient.RequestBodySpec spec;

    HTTPRequestNode(GlobalPool globalPool) {
        super(globalPool);
        setNodeType(NodeType.HTTP);
    }

    HTTPRequestNode(GlobalPool globalPool, String nodeId) {
        super(globalPool, nodeId);
        setNodeType(NodeType.HTTP);
    }

    @Override
    public List<ConfigVO> getNodeConfigs(){
        List<ConfigVO> defaultConfigs = new ArrayList<>();
        defaultConfigs.add(ConfigVO.builder()
                .name("Url")
                .type("String")
                .des("网络请求基础地址")
                .value("https://api.example.com")
                .required(true)
                .build());
        defaultConfigs.add(ConfigVO.builder()
                .name("Uri")
                .type("String")
                .des("资源地址")
                .required(false)
                .value("")
                .build());
        defaultConfigs.add(ConfigVO.builder()
                .name("Method")
                .type("String")
                .des("网络请求模式")
                .value("GET")
                .required(true)
                .build());
        defaultConfigs.add(ConfigVO.builder()
                .name("Headers")
                .type("Map")
                .des("请求头 (JSON格式)")
                .value("{}")
                .required(false)
                .build());
        defaultConfigs.add(ConfigVO.builder()
                .name("Data")
                .type("Map")
                .des("请求数据 (JSON格式)")
                .value("{}")
                .required(false)
                .build());
        defaultConfigs.add(ConfigVO.builder()
                .name("Timeout")
                .type("Number")
                .des("超时时间（毫秒）")
                .value("30000")
                .required(false)
                .build());
        return defaultConfigs;
    }

    @Override
    public List<OutputVariableDes> getNodeOutputs(){
        List<OutputVariableDes> defaultOutputs = new ArrayList<>();
        defaultOutputs.add(OutputVariableDes.builder()
                        .name("output")
                        .type("Object")
                        .des("请求结果")
                .build());
        defaultOutputs.add(OutputVariableDes.builder()
                        .name("statusCode")
                        .type("Integer")
                        .des("状态码")
                .build());
        return defaultOutputs;
    }

    private boolean hasRequestBody(String method){
        return "POST".equals(method) || "DELETE".equals(method) || "PUT".equals(method) || "PATCH".equals(method);
    }

    @Override
    public void before(){
        String url = (String) configs.get("Url");
        String uri = (String) configs.get("Uri");
        String method = (String) configs.get("Method");
        Map<String, String> headers = (Map<String, String>) configs.computeIfAbsent("Headers", k -> new HashMap<>());
        Map<String, String> data = (Map<String, String>) configs.computeIfAbsent("Data", k -> new HashMap<>());
        // 超时时间默认30秒
        int timeout = configs.get("Timeout") != null ? (int) configs.get("Timeout") : 30000;

        // 处理URL
        if(url == null || url.trim().isEmpty()){
            onNodeError("URL不允许为空");
            return;
        }
        url = url.trim();
        if(url.endsWith("/")) url = url.substring(0, url.length() - 1);
        
        // 处理URI
        if(uri != null && !uri.trim().isEmpty()) {
            String trimmedUri = uri.trim();
            if (trimmedUri.startsWith("/")) trimmedUri = trimmedUri.substring(1);
            url += "/" + trimmedUri;
        }
        
        // 处理请求方法
        if(method == null || method.trim().isEmpty()){
            onNodeError("请求模式不允许为空");
            return;
        }
        method = method.toUpperCase().trim();
        
        // 处理请求数据 - 解析全局变量
        Map<String, String> processedData = new HashMap<>();
        data.forEach((key, value) -> {
            if (value != null) {
                processedData.put(key, globalPool.parseConfig(value, token));
            }
        });
        
        // 处理请求头 - 解析全局变量
        Map<String, String> processedHeaders = new HashMap<>();
        headers.forEach((key, value) -> {
            if (value != null) {
                processedHeaders.put(key, globalPool.parseConfig(value, token));
            }
        });
        
        // 配置超时
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeout)
                .responseTimeout(Duration.ofMillis(timeout))
                .doOnConnected(conn -> conn.addHandlerLast(new ReadTimeoutHandler(timeout, TimeUnit.MILLISECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(timeout, TimeUnit.MILLISECONDS)));
        
        // 配置请求客户端
        WebClient.Builder webClientBuilder = WebClient.builder()
                .baseUrl(url)
                .defaultHeader(HttpHeaders.USER_AGENT,"Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko)")
                .clientConnector(new ReactorClientHttpConnector(httpClient));
        
        // 添加自定义请求头
        for(Map.Entry<String, String> entry : processedHeaders.entrySet()){
            webClientBuilder.defaultHeader(entry.getKey(), entry.getValue());
        }
        
        WebClient webClient = webClientBuilder.build();
        WebClient.RequestBodySpec spec = webClient.method(HttpMethod.valueOf(method));
        
        // 添加请求头
        for(String k : processedHeaders.keySet()){
            spec = spec.header(k, processedHeaders.get(k));
        }
        
        // 设置请求体
        if(hasRequestBody(method) && !processedData.isEmpty()){
            spec.contentType(MediaType.APPLICATION_JSON).bodyValue(JSON.toJSONString(processedData));
        }
        
        this.spec = spec;
    }

    @Override
    public void run(){
        try {
            Object result;
            // 处理响应
            WebClient.ResponseSpec responseSpec = spec.retrieve()
                    .onStatus(httpStatus -> httpStatus.is4xxClientError() || httpStatus.is5xxServerError(),
                            clientResponse -> {
                                return clientResponse.bodyToMono(String.class)
                                        .flatMap(errorBody -> Mono.error(new RuntimeException("HTTP错误: " + 
                                                clientResponse.statusCode() + " - " + errorBody)));
                            });

            result = responseSpec.bodyToMono(String.class).block();

            result = JSON.parseObject((String) result, Object.class);
            
            // 将响应状态码和响应体存入节点变量池
            nodePool.put("output", result);
            nodePool.put("statusCode", 200);
            
            putWorkflowResult(WorkflowResult.builder()
                    .token(token)
                    .nodeId(nodeId)
                    .msg("HTTP请求成功")
                    .extData(result)
                    .build());
                    
        } catch (Exception e) {
            // 处理请求错误
            String errorMsg = e.getMessage();

            // 调用错误处理，停止当前分支
            onNodeError("HTTP请求失败: " + errorMsg);
        }
    }
}
