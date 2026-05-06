package com.example.demoworkflow.control;

import com.alibaba.fastjson2.JSON;
import com.example.demoworkflow.mapper.CFileMapper;
import com.example.demoworkflow.pojo.CFile;
import com.example.demoworkflow.services.CFileServiceImpl;
import com.example.demoworkflow.utils.fs.workflow.WorkflowFileUtils;
import com.example.demoworkflow.utils.types.NodeType;
import com.example.demoworkflow.utils.workflow.dto.OutputVariableDes;
import com.example.demoworkflow.utils.workflow.dto.Workflow;
import com.example.demoworkflow.utils.workflow.handler.SseHandler;
import com.example.demoworkflow.utils.workflow.handler.WorkflowHandler;
import com.example.demoworkflow.utils.workflow.pool.GlobalPool;
import com.example.demoworkflow.vo.CFileVO;
import com.example.demoworkflow.vo.ConfigVO;
import com.example.demoworkflow.vo.NodeTypeVO;
import com.example.demoworkflow.vo.WorkflowVO;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v3")
public class WorkflowController {

    @Resource
    private WorkflowHandler workflowHandler;

    @Resource
    private GlobalPool globalPool;

    @Resource
    private SseHandler sseHandler;

    @Resource
    private CFileServiceImpl cFileService;

    @Resource
    private WorkflowFileUtils workflowFS;

    @Resource
    private CFileMapper cFileMapper;

    @Operation(description = "获取所有节点类型")
    @RequestMapping(path="/node/types", method=RequestMethod.GET)
    public List<NodeTypeVO> getNodeTypes(){
        List<NodeTypeVO> nodeTypes = new ArrayList<>();
        for(NodeType nodeType: NodeType.values()) {
            if(nodeType == NodeType.EMPTY_NODE) continue;
            nodeTypes.add(NodeTypeVO.builder()
                    .name(nodeType.getName())
                    .code(nodeType.getCode())
                    .type(nodeType.toString())
                    .build());
        }
        return nodeTypes;
    }

    @Operation(description = "通过节点代码获取节点配置信息")
    @RequestMapping(path="/node/{nodeCode}/config", method=RequestMethod.GET)
    public List<ConfigVO> getNodeConfigsByCode(@PathVariable int nodeCode){
        return NodeType.getNodeConfigsByCode(nodeCode);
    }

    @Operation(description = "通过节点代码获取节点输出变量描述")
    @RequestMapping(path="/node/{nodeCode}/output", method=RequestMethod.GET)
    public List<OutputVariableDes> getNodeOutputsByCode(@PathVariable int nodeCode){
        return NodeType.getNodeOutputsByCode(nodeCode);
    }

    @Operation(description = "运行工作流")
    @RequestMapping(path="/workflow/run", method=RequestMethod.POST)
    public SseEmitter runWorkflow(@RequestBody WorkflowVO workflowVO){
        Workflow workflow = Workflow.castFromVO(workflowVO, globalPool);
        String token = workflow.getToken();
        workflowHandler.handler(workflow);
        return sseHandler.connect(token);
    }

    @Operation(description = "保存工作流")
    @RequestMapping(path="/workflow", method=RequestMethod.POST)
    public boolean saveWorkflow(@RequestBody WorkflowVO workflowVO){
        return workflowFS.saveWorkflow(workflowVO);
    }

    @Operation(description = "通过UUID获取工作流")
    @RequestMapping(path="/workflow", method=RequestMethod.GET)
    public WorkflowVO getWorkflowByUUID(@RequestParam String uuid){
        return workflowFS.loadWorkflow(uuid);
    }

    @Operation(description = "获取所有保存的工作流")
    @RequestMapping(path="/workflow/all", method=RequestMethod.GET)
    public List<CFileVO> getAllSavedWorkflows(){
        List<CFile> cFiles = cFileService.getAllCFilesByWorkspace("workflow");
        return cFileMapper.cFileListToCFileVoList(cFiles);
    }

    @Operation(description = "更新工作流")
    @RequestMapping(path="/workflow", method=RequestMethod.PUT)
    public void updateWorkflow(@RequestBody WorkflowVO workflowVO, @RequestParam String uuid) {
        workflowFS.deleteWorkflow(uuid);
        workflowFS.saveWorkflow(workflowVO, uuid);
    }

    @Operation(description = "删除工作流")
    @RequestMapping(path="/workflow", method=RequestMethod.DELETE)
    public void deleteWorkflow(@RequestParam String uuid) {
        cFileService.deleteCFile("workflow", uuid);
        workflowFS.deleteWorkflow(uuid);
    }

    @Operation(description = "上传工作流")
    @RequestMapping(path="/workflow/upload", method=RequestMethod.POST)
    public String uploadWorkflow(@RequestBody MultipartFile file) {
        try {
            InputStream fis = file.getInputStream();
            byte[] bytes = fis.readAllBytes();
            WorkflowVO workflowVO = JSON.parseObject(bytes, WorkflowVO.class);
            workflowFS.saveWorkflow(workflowVO);
        }catch(Exception e){
            log.error("Upload file error", e);
            return "{\"state\":\"error\",\"msg\":\""+e.getMessage()+"\"}";
        }
        return "{\"state\":\"ok\",\"msg\":\"\"}";
    }

    @Operation(description = "下载工作流")
    @RequestMapping(path="/workflow/download", method=RequestMethod.GET)
    public void downloadWorkflow(@RequestParam String uuid, HttpServletResponse response) throws IOException {
        CFile cFile = cFileService.getCFileByWorkspaceAndUuid("workflow", uuid);
        Path path = workflowFS.pathFactory(cFile.getUuid());
        File file = new File(path.toUri());
        FileInputStream fis = new FileInputStream(file);
        response.setHeader("content-disposition","attachment;fileName="+ URLEncoder.encode(uuid, StandardCharsets.UTF_8));
        response.setHeader("Content-Length", String.valueOf(file.length()));
        response.setContentType("application/json");
        FileCopyUtils.copy(fis, response.getOutputStream());
        fis.close();
    }
}
