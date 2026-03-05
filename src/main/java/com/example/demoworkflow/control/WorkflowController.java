package com.example.demoworkflow.control;

import com.example.demoworkflow.pojo.CFile;
import com.example.demoworkflow.services.CFileServiceImpl;
import com.example.demoworkflow.utils.fs.workflow.WorkflowFileUtils;
import com.example.demoworkflow.utils.workflow.dto.OutputVariableDes;
import com.example.demoworkflow.utils.workflow.dto.Workflow;
import com.example.demoworkflow.utils.workflow.handler.SseResultHandler;
import com.example.demoworkflow.utils.workflow.handler.WorkflowHandler;
import com.example.demoworkflow.utils.workflow.nodes.NodeType;
import com.example.demoworkflow.utils.workflow.pool.GlobalPool;
import com.example.demoworkflow.utils.workflow.states.NodeStates;
import com.example.demoworkflow.utils.workflow.states.ResultHandlerStates;
import com.example.demoworkflow.utils.workflow.states.WorkflowStates;
import com.example.demoworkflow.vo.ConfigVO;
import com.example.demoworkflow.vo.NodeTypeVO;
import com.example.demoworkflow.vo.WorkflowVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作流控制器 - 提供工作流执行和状态查询API
 */
@RestController
@RequestMapping("/api/v3/workflow")
@Tag(name = "工作流管理", description = "工作流执行、状态查询和SSE实时推送")
@Slf4j
public class WorkflowController {

    @Resource
    private WorkflowHandler workflowHandler;

    @Resource
    private GlobalPool globalPool;

    @Resource
    private SseResultHandler sseResultHandler;

    @Resource
    private WorkflowFileUtils workflowFileUtils;

    @Resource
    private CFileServiceImpl cFileService;

    @Operation(description = "执行工作流(SSE模式)")
    @PostMapping("/execute")
    public SseEmitter executeWithSse(@RequestBody WorkflowVO workflowVO) {
        // 创建SSE发射器，设置超时时间为24小时
        SseEmitter emitter = new SseEmitter(24 * 60 * 60 * 1000L);
        
        try {
            // 解析工作流
            Workflow workflow = Workflow.castFromVO(workflowVO, globalPool);
            String token = workflow.getToken();
            
            // 注册SSE连接
            sseResultHandler.addSseEmitter(token, emitter);
            
            // 发送初始连接消息
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("{\"token\":\"" + token + "\",\"msg\":\"已连接到工作流\"}"));
            
            // 异步执行工作流（不阻塞当前线程）
            workflowHandler.handlerAsync(workflow);
            
            log.info("工作流开始执行, token: {}", token);
            
        } catch (IOException e) {
            log.error("SSE连接建立失败", e);
            emitter.completeWithError(e);
        }
        
        return emitter;
    }

    @Operation(description = "执行工作流(同步模式)")
    @PostMapping("/execute-sync")
    public Map<String, Object> executeSync(@RequestBody WorkflowVO workflowVO) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 解析工作流
            Workflow workflow = Workflow.castFromVO(workflowVO, globalPool);
            String token = workflow.getToken();
            
            // 执行工作流
            workflowHandler.handler(workflow, null);
            
            result.put("success", true);
            result.put("token", token);
            result.put("msg", "工作流已启动");
            
        } catch (Exception e) {
            log.error("工作流执行失败", e);
            result.put("success", false);
            result.put("msg", e.getMessage());
        }
        return result;
    }

    @Operation(description = "获取工作流状态")
    @GetMapping("/status/{token}")
    public Map<String, Object> getWorkflowStatus(@PathVariable String token) {
        Map<String, Object> result = new HashMap<>();
        
        int state = globalPool.getWorkflowState(token);
        int rhState = globalPool.getResultHandlerState(token);
        
        result.put("token", token);
        result.put("workflowState", state);
        result.put("workflowStateName", getWorkflowStateName(state));
        result.put("resultHandlerState", rhState);
        result.put("resultHandlerStateName", getResultHandlerStateName(rhState));
        
        return result;
    }

    @Operation(description = "获取节点状态")
    @GetMapping("/node-status/{token}/{nodeId}")
    public Map<String, Object> getNodeStatus(@PathVariable String token, @PathVariable String nodeId) {
        Map<String, Object> result = new HashMap<>();
        
        int state = globalPool.getNodeState(token, nodeId);
        
        result.put("token", token);
        result.put("nodeId", nodeId);
        result.put("state", state);
        result.put("stateName", getNodeStateName(state));
        
        return result;
    }

    @Operation(description = "获取全部节点类型")
    @GetMapping("/nodes")
    public List<NodeTypeVO> getNodeTypes(){
        List<NodeTypeVO> list = new ArrayList<>();
        for(NodeType nodeType : NodeType.values()){
            // 不允许创建空节点
            if(nodeType == NodeType.EMPTY_NODE) continue;
            list.add(NodeTypeVO.builder()
                    .code(nodeType.getCode())
                    .type(nodeType.toString())
                    .name(nodeType.getName())
                    .build());
        }
        return list;
    }

    @Operation(description = "获取节点基础配置")
    @GetMapping("/node-config/{nodeCode}")
    public List<ConfigVO> getNodeConfigsByCode(@PathVariable int nodeCode){
        return NodeType.getNodeDefaultConfigsByCode(nodeCode);
    }

    @Operation(description = "获取节点基础配置")
    @GetMapping("/node-output/{nodeCode}")
    public List<OutputVariableDes> getNodeOutputsByCode(@PathVariable int nodeCode){
        return NodeType.getNodeOutputsByCode(nodeCode);
    }

    @Operation(description = "获取全局变量池")
    @GetMapping("/variables/{token}")
    public Map<String, Object> getVariables(@PathVariable String token) {
        Map<Object, Object> variables = globalPool.getAll(token);
        Map<String, Object> result = new HashMap<>();
        
        // 过滤掉系统变量
        for (Map.Entry<Object, Object> entry : variables.entrySet()) {
            String key = entry.getKey().toString();
            if (!key.startsWith("<|") && !key.startsWith("node_state:")) {
                result.put(key, entry.getValue());
            }
        }
        
        return result;
    }

    @Operation(description = "获取工作流结果队列")
    @GetMapping("/results/{token}")
    public Map<String, Object> getResults(@PathVariable String token) {
        Map<String, Object> result = new HashMap<>();
        
        // 注意：这里只能获取已经处理过的结果，
        // 实时结果需要通过SSE获取
        int state = globalPool.getWorkflowState(token);
        
        result.put("token", token);
        result.put("workflowState", state);
        result.put("workflowEnded", (state & WorkflowStates.DONE) != 0 || (state & WorkflowStates.ERROR) != 0);
        
        return result;
    }

    @Operation(description = "终止工作流")
    @PostMapping("/stop/{token}")
    public Map<String, Object> stopWorkflow(@PathVariable String token) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            workflowHandler.stopWorkflow(token);
            sseResultHandler.closeAllSseEmitters(token);
            
            result.put("success", true);
            result.put("msg", "工作流已终止");
            
        } catch (Exception e) {
            log.error("终止工作流失败: {}", token, e);
            result.put("success", false);
            result.put("msg", e.getMessage());
        }
        
        return result;
    }

    @Operation(description = "获取SSE连接状态")
    @GetMapping("/sse-status/{token}")
    public Map<String, Object> getSseStatus(@PathVariable String token) {
        Map<String, Object> result = new HashMap<>();
        
        result.put("token", token);
        result.put("hasConnection", sseResultHandler.hasActiveConnection(token));
        result.put("connectionCount", sseResultHandler.getSseEmitterCount(token));
        
        return result;
    }

    @Operation(description = "获取工作流列表")
    @GetMapping("/list")
    public Map<String, Object> listWorkflows() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<CFile> cFiles = cFileService.getAllCFiles();
            List<Map<String, Object>> workflows = new ArrayList<>();
            for (CFile cFile : cFiles) {
                if ("workflow".equals(cFile.getWorkspace())) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", cFile.getId());
                    item.put("uuid", cFile.getUuid());
                    item.put("name", cFile.getName());
                    item.put("createTime", cFile.getCreated());
                    workflows.add(item);
                }
            }
            result.put("success", true);
            result.put("data", workflows);
        } catch (Exception e) {
            log.error("获取工作流列表失败", e);
            result.put("success", false);
            result.put("msg", e.getMessage());
        }
        return result;
    }

    @Operation(description = "保存工作流")
    @PostMapping("/save")
    public Map<String, Object> saveWorkflow(@RequestBody WorkflowVO workflowVO) {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean saved = workflowFileUtils.saveWorkflow(workflowVO);
            if (saved) {
                result.put("success", true);
                result.put("msg", "工作流已保存");
            } else {
                result.put("success", false);
                result.put("msg", "保存失败");
            }
        } catch (Exception e) {
            log.error("保存工作流失败", e);
            result.put("success", false);
            result.put("msg", e.getMessage());
        }
        return result;
    }

    @Operation(description = "更新工作流")
    @PutMapping("/save/{uuid}")
    public Map<String, Object> updateWorkflow(@PathVariable String uuid, @RequestBody WorkflowVO workflowVO) {
        Map<String, Object> result = new HashMap<>();
        try {
            workflowFileUtils.write(uuid, workflowVO);
            // 更新文件名称
            if (workflowVO.name != null && !workflowVO.name.isEmpty()) {
                cFileService.updateCFileName("workflow", uuid, workflowVO.name);
            }
            result.put("success", true);
            result.put("msg", "工作流已更新");
        } catch (Exception e) {
            log.error("更新工作流失败", e);
            result.put("success", false);
            result.put("msg", e.getMessage());
        }
        return result;
    }

    @Operation(description = "加载工作流")
    @GetMapping("/load/{uuid}")
    public Map<String, Object> loadWorkflow(@PathVariable String uuid) {
        Map<String, Object> result = new HashMap<>();
        try {
            WorkflowVO workflow = workflowFileUtils.loadWorkflow(uuid);
            if (workflow != null) {
                result.put("success", true);
                result.put("data", workflow);
            } else {
                result.put("success", false);
                result.put("msg", "工作流不存在");
            }
        } catch (Exception e) {
            log.error("加载工作流失败", e);
            result.put("success", false);
            result.put("msg", e.getMessage());
        }
        return result;
    }

    @Operation(description = "删除工作流")
    @DeleteMapping("/delete/{uuid}")
    public Map<String, Object> deleteWorkflow(@PathVariable String uuid) {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean deleted = workflowFileUtils.deleteWorkflow(uuid);
            if (deleted) {
                result.put("success", true);
                result.put("msg", "工作流已删除");
            } else {
                result.put("success", false);
                result.put("msg", "删除失败");
            }
        } catch (Exception e) {
            log.error("删除工作流失败", e);
            result.put("success", false);
            result.put("msg", e.getMessage());
        }
        return result;
    }

    private String getWorkflowStateName(int state) {
        if ((state & WorkflowStates.NULL) != 0) return "NULL";
        if ((state & WorkflowStates.ERROR) != 0) return "ERROR";
        if ((state & WorkflowStates.STAND_BY) != 0) return "STAND_BY";
        if ((state & WorkflowStates.RUNNING) != 0) return "RUNNING";
        if ((state & WorkflowStates.DONE) != 0) return "DONE";
        if ((state & WorkflowStates.ABORT) != 0) return "ABORT";
        return "UNKNOWN";
    }

    private String getResultHandlerStateName(int state) {
        if ((state & ResultHandlerStates.NULL) != 0) return "NULL";
        if ((state & ResultHandlerStates.ERROR) != 0) return "ERROR";
        if ((state & ResultHandlerStates.STAND_BY) != 0) return "STAND_BY";
        if ((state & ResultHandlerStates.RUNNING) != 0) return "RUNNING";
        if ((state & ResultHandlerStates.DONE) != 0) return "DONE";
        return "UNKNOWN";
    }

    private String getNodeStateName(int state) {
        if ((state & NodeStates.NULL) != 0) return "NULL";
        if ((state & NodeStates.ERROR) != 0) return "ERROR";
        if ((state & NodeStates.STAND_BY) != 0) return "STAND_BY";
        if ((state & NodeStates.RUNNING) != 0) return "RUNNING";
        if ((state & NodeStates.DONE) != 0) return "DONE";
        if ((state & NodeStates.DISABLED) != 0) return "DISABLED";
        return "UNKNOWN";
    }
}
