package com.example.demoworkflow.utils.workflow.nodes;

import com.alibaba.fastjson2.JSON;
import com.example.demoworkflow.utils.types.ConfigTypes;
import com.example.demoworkflow.utils.types.NodeType;
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
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

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

    public HTTPRequestNode(GlobalPool globalPool) {
        super(globalPool);
        setNodeType(NodeType.HTTP);
    }

    public HTTPRequestNode(GlobalPool globalPool, String nodeId) {
        super(globalPool, nodeId);
        setNodeType(NodeType.HTTP);
    }

    @Override
    public List<ConfigVO> getNodeConfigs(){
        List<ConfigVO> defaultConfigs = new ArrayList<>();
        defaultConfigs.add(ConfigVO.builder()
                .name("Url")
                .type(ConfigTypes.STRING)
                .des("网络请求基础地址")
                .value("https://www.example.com")
                .required(true)
                .build());
        defaultConfigs.add(ConfigVO.builder()
                .name("Uri")
                .type(ConfigTypes.STRING)
                .des("资源地址")
                .required(false)
                .value("")
                .build());
        defaultConfigs.add(ConfigVO.builder()
                .name("Method")
                .type(ConfigTypes.SELECT)
                .options(JSON.toJSONString(List.of("GET", "POST", "PUT", "DELETE", "PATCH")))
                .des("网络请求模式")
                .value("GET")
                .required(true)
                .build());
        defaultConfigs.add(ConfigVO.builder()
                .name("Headers")
                .type(ConfigTypes.MAP)
                .des("请求头")
                .value("{}")
                .required(false)
                .build());
        defaultConfigs.add(ConfigVO.builder()
                .name("Data")
                .type(ConfigTypes.MAP)
                .des("请求数据")
                .value("{}")
                .required(false)
                .build());
        defaultConfigs.add(ConfigVO.builder()
                .name("Timeout")
                .type(ConfigTypes.NUMBER)
                .des("超时时间（秒）")
                .value("5000")
                .required(false)
                .build());
        defaultConfigs.add(ConfigVO.builder()
                .name("Retry")
                .type(ConfigTypes.BOOLEAN)
                .des("是否重试")
                .value("true")
                .required(false)
                .build());
        defaultConfigs.add(ConfigVO.builder()
                .name("MaxRetriedTimes")
                .type(ConfigTypes.NUMBER)
                .des("最大重试次数")
                .value("5")
                .build());
        return defaultConfigs;
    }

    @Override
    public List<OutputVariableDes> getNodeOutputs() {
        return List.of(OutputVariableDes.builder()
                        .name("output")
                        .type("String")
                        .des("请求结果")
                .build());
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
        int timeout = (int) configs.get("Timeout");
        boolean retry = (boolean) configs.get("Retry");
        int maxRetriedTimes = (int) configs.get("MaxRetriedTimes");
        // 处理URL
        if(url == null){
            onNodeError("URL不允许为空");
            return;
        }
        if(url.endsWith("/")) url = url.substring(0, url.length() - 1);
        // 处理URI
        if(uri != null) {
            if (uri.startsWith("/")) uri = uri.substring(1);
            url += uri;
        }
        // 处理请求方法
        if(method == null){
            onNodeError("请求模式不允许为空");
            return;
        }
        method = method.toUpperCase();
        // 处理请求数据
        data.forEach((key, value) -> {
            data.put(key, globalPool.parseConfig(value, token));
        });
        // 配置超时
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeout)
                .responseTimeout(Duration.ofMillis(timeout))
                .disableRetry(!retry)
                .doOnConnected(conn -> conn.addHandlerLast(new ReadTimeoutHandler(timeout, TimeUnit.MILLISECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(timeout, TimeUnit.MILLISECONDS)));
        // 配置重试
        ExchangeFilterFunction retryFilter = (request, next) -> next.exchange(request)
                .retryWhen(Retry.fixedDelay(maxRetriedTimes, Duration.ofMillis(500)));
        // 配置请求客户端
        WebClient webClient = WebClient.builder()
                .baseUrl(url)
                .defaultHeader(HttpHeaders.USER_AGENT,"Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko)")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .filter(retryFilter)
                .build();
        WebClient.RequestBodySpec spec =  webClient.method(HttpMethod.valueOf(method));
        for(String k : headers.keySet()){
            spec = spec.header(k, headers.get(k));
        }
        if(hasRequestBody(method) && !data.isEmpty()){
            spec.contentType(MediaType.APPLICATION_JSON).bodyValue(data);
        }
        this.spec = spec;
    }

    @Override
    public void run(){
        try {
            Mono<Object> rsp = spec.retrieve().bodyToMono(Object.class);
            Object result = rsp.block();
            nodePool.put("output", result);
            putWorkflowResult(WorkflowResult.builder()
                    .token(token)
                    .nodeId(nodeId)
                    .msg("请求结果")
                    .extData(result)
                    .build());
        }catch (Exception e){
            onNodeError(e.getMessage());
        }
    }
}
