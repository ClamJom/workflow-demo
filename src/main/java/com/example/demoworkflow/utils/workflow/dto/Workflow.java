package com.example.demoworkflow.utils.workflow.dto;

import com.example.demoworkflow.mapper.ConfigMapper;
import com.example.demoworkflow.utils.workflow.nodes.NodeImpl;
import com.example.demoworkflow.utils.types.NodeType;
import com.example.demoworkflow.utils.workflow.pool.GlobalPool;
import com.example.demoworkflow.utils.workflow.states.WorkflowStates;
import com.example.demoworkflow.vo.EdgeVO;
import com.example.demoworkflow.vo.WorkflowVO;
import lombok.Data;

import java.util.*;

@Data
public class Workflow {
    private String token;

    private GlobalPool globalPool;

    private List<NodeImpl> nodes;

    private NodeImpl startNode;

    public Workflow(GlobalPool globalPool){
        token = UUID.randomUUID().toString();
        this.globalPool = globalPool;
        nodes = new ArrayList<>();

        globalPool.initWorkflow(token);
    }

    public boolean isRunning(){
        return (globalPool.getWorkflowState(token) & WorkflowStates.RUNNING) != 0;
    }

    public boolean isEnded(){
        return (globalPool.getWorkflowState(token) & WorkflowStates.DONE) != 0 ||
                (globalPool.getWorkflowState(token) & WorkflowStates.ERROR) != 0;
    }

    /**
     * 拓扑排序判环
     * @param edges 有向边
     * @return  是否存在环
     */
    private static boolean hasCircle(List<EdgeVO> edges){
        Map<String, Set<String>> graph = new HashMap<>();
        Map<String, Integer> inDegree = new HashMap<>();
        for(EdgeVO edge: edges){
            Set<String> outEdgeSet = graph.computeIfAbsent(edge.from, k -> new HashSet<>());
            graph.computeIfAbsent(edge.to, k -> new HashSet<>());
            inDegree.computeIfAbsent(edge.from, k -> 0);
            int inCount = inDegree.computeIfAbsent(edge.to, k -> 0);
            outEdgeSet.add(edge.to);
            inDegree.put(edge.to, inCount + 1);
        }
        Queue<String> zeroInDegree = new ArrayDeque<>();
        for(String key: inDegree.keySet()){
            if(inDegree.get(key) == 0) zeroInDegree.add(key);
        }
        int processNodes = 0;
        while(!zeroInDegree.isEmpty()){
            String node = zeroInDegree.poll();
            processNodes++;
            Set<String> neighborNodes = graph.get(node);
            if(neighborNodes == null) continue;
            for(String targetNode: neighborNodes){
                int inDegreeCount = inDegree.get(targetNode);
                inDegree.put(targetNode, inDegreeCount - 1);
                if(inDegreeCount - 1 == 0) zeroInDegree.add(targetNode);
            }
        }
        return processNodes != inDegree.size();
    }

    public static Workflow castFromVO(WorkflowVO vo, GlobalPool globalPool){
        Workflow workflow = new Workflow(globalPool);
        List<NodeImpl> nodes = new ArrayList<>();
        Map<String, NodeImpl> nodeMap = new HashMap<>();
        vo.nodes.forEach(node->{
           NodeImpl nodeInstance = NodeType.createNodeInstanceWithNodeIdByCode(node.type, globalPool, node.id);
           if(nodeInstance == null){
               throw new NullPointerException("从JSON构造节点对象失败！");
           }
           nodeInstance.setToken(workflow.getToken());
           // 配置项的初始化应当在节点执行前进行
           nodeInstance.configList = ConfigMapper.INSTANCE.listConfigVOToListConfig(node.configs);
           nodes.add(nodeInstance);
           nodeMap.put(node.id, nodeInstance);
           if(nodeInstance.getNodeType() == NodeType.START){
               workflow.startNode = nodeInstance;
           }
        });
        NodeImpl startNode = workflow.startNode;
        if(startNode == null) throw new RuntimeException("流程不存在起始节点！");
        if(hasCircle(vo.edges)) throw new RuntimeException("不允许存在环结构");
        vo.edges.forEach(edge -> {
            if(edge.from.equals(edge.to)) throw new RuntimeException("不允许自循环！");
            nodeMap.get(edge.to).relatedNodes.add(edge.from);
            // 注意自循环！
            nodeMap.get(edge.from).nextNodes.add(nodeMap.get(edge.to));
        });
        workflow.getNodes().addAll(nodes);
        return workflow;
    }
}
