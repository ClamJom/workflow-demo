<template>
  <div class="workflow-editor">
    <!-- 顶部工具栏 -->
    <div class="toolbar">
      <el-button @click="goBack" icon="ArrowLeft">返回</el-button>
      <el-input
        v-model="workflowName"
        placeholder="工作流名称"
        style="width: 200px;"
        size="default"
      />
      <el-button type="primary" @click="saveWorkflow" :loading="saving">保存工作流</el-button>
      <el-button type="success" @click="runWorkflow" :loading="running">运行工作流</el-button>
      
      <el-divider direction="vertical" />
      
      <!-- 节点类型选择 -->
      <el-dropdown @command="handleAddNode">
        <el-button type="info">
          添加节点 <el-icon class="el-icon--right"><ArrowDown /></el-icon>
        </el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item v-for="nodeType in nodeTypes" :key="nodeType.type" :command="nodeType.type">
              <span :style="{ color: nodeType.color }">●</span> {{ nodeType.label }}
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <div class="editor-container">
      <!-- 工作流画布 -->
      <div class="canvas-wrapper">
        <VueFlow
          v-model="nodes"
          v-model:edges="edges"
          :connection-line-style="{ stroke: '#666' }"
          :default-edge-options="{ 
            type: 'default',
            animated: false,
            style: { stroke: '#999', strokeWidth: 2, strokeDasharray: '5,5' }
          }"
          @connect="onConnectHandler"
          @node-click="onNodeClick"
          @edge-click="onEdgeClick"
          @selection-change="onSelectionChange"
        >

          <!-- 起点节点 -->
          <template #node-start="props">
            <StartNode :id="props.id" :data="props.data" />
          </template>
          
          <!-- 终点节点 -->
          <template #node-end="props">
            <EndNode :id="props.id" :data="props.data" />
          </template>
          
          <!-- 问候节点 -->
          <template #node-hello="props">
            <HelloNode :id="props.id" :data="props.data" />
          </template>
          
          <!-- HTTP请求节点 -->
          <template #node-http="props">
            <HttpNode :id="props.id" :data="props.data" />
          </template>
          
          <!-- 条件节点 -->
          <template #node-condition="props">
            <ConditionNode :id="props.id" :data="props.data" />
          </template>
          
          <!-- LLM节点 -->
          <template #node-llm="props">
            <LLMNode :id="props.id" :data="props.data" />
          </template>
          
          <!-- 控件 -->
          <Controls>
            <ControlButton title="Log `toObject`" @click="logToObject">
              <Icon name="log" />
            </ControlButton>
          </Controls>
          <Background variant="dots" />
          <MiniMap />
        </VueFlow>
      </div>

      <!-- 右侧配置面板 -->
      <div class="config-panel" v-if="selectedNode || selectedEdge">
        <div style="position: absolute; right: 5px; top: 5px; color: red; font-size: 12px; cursor: pointer;"
             @click="()=>{
               if(selectedEdge)
                selectedEdge = null;
               if(selectedNode)
                selectedNode = null;
             }">关闭</div>
        <!-- 节点配置 -->
        <div v-if="selectedNode" class="node-config">
          <h3>节点配置</h3>

          <!-- 可用变量列表 -->
          <div v-if="getUpstreamVariables().length > 0" class="upstream-variables">
            <h4>可用变量</h4>
            <div class="variable-tags">
              <el-tag 
                v-for="v in getUpstreamVariables()" 
                :key="v.variableName" 
                size="small"
                @click="insertVariable({ value: `{{${v.variableName}}}` })"
                class="variable-tag"
              >
                {{ v.label }}
              </el-tag>
            </div>
            <p class="variable-hint">点击标签或输入/插入变量</p>
          </div>
          
          <el-form label-width="100px">
            <el-form-item label="节点ID">
              <el-input v-model="selectedNode.data.label" disabled />
            </el-form-item>
            <el-form-item label="节点类型">
              <el-input :value="getNodeTypeName(selectedNode.type)" disabled />
            </el-form-item>
            <el-form-item label="节点名称">
              <el-input v-model="selectedNode.data.customLabel" placeholder="自定义显示名称" />
            </el-form-item>
            
            <!-- 根据节点类型显示不同配置 -->
            <template v-if="selectedNode.type === 'http'">
              <el-form-item label="URL">
                <el-input 
                  v-model="selectedNode.data.config.Url" 
                  placeholder="https://api.example.com / 输入变量"
                  @keydown="handleInputKeydown($event, 'config.Url')"
                  data-field="config.Url"
                />
              </el-form-item>
              <el-form-item label="Method">
                <el-select v-model="selectedNode.data.config.Method">
                  <el-option label="GET" value="GET" />
                  <el-option label="POST" value="POST" />
                  <el-option label="PUT" value="PUT" />
                  <el-option label="DELETE" value="DELETE" />
                </el-select>
              </el-form-item>
              <el-form-item label="Headers">
                <el-table :data="getHeadersData()" size="small" stripe>
                  <el-table-column prop="key" label="Key" min-width="120">
                    <template #default="{ row, $index }">
                      <el-input v-model="row.key" placeholder="Key" @change="updateHeadersData" />
                    </template>
                  </el-table-column>
                  <el-table-column prop="value" label="Value">
                    <template #default="{ row }">
                      <el-input v-model="row.value" placeholder="Value" @change="updateHeadersData" />
                    </template>
                  </el-table-column>
                  <el-table-column width="60">
                    <template #default="{ $index }">
                      <el-button type="danger" link @click="deleteHeadersRow($index)">删除</el-button>
                    </template>
                  </el-table-column>
                </el-table>
                <el-button size="small" @click="addHeadersRow" style="margin-top: 8px">+ 添加</el-button>
              </el-form-item>
              <el-form-item label="Data">
                <el-table :data="getDataData()" size="small" stripe>
                  <el-table-column prop="key" label="Key" min-width="120">
                    <template #default="{ row }">
                      <el-input v-model="row.key" placeholder="Key" @change="updateDataData" />
                    </template>
                  </el-table-column>
                  <el-table-column prop="value" label="Value">
                    <template #default="{ row }">
                      <el-input 
                        v-model="row.value" 
                        placeholder="Value / 输入变量"
                        @change="updateDataData"
                        @keydown="handleInputKeydown($event, 'data-row-' + row._uuid + '-value')"
                        :data-field="'data-row-' + row._uuid + '-value'"
                      />
                    </template>
                  </el-table-column>
                  <el-table-column width="60">
                    <template #default="{ $index }">
                      <el-button type="danger" link @click="deleteDataRow($index)">删除</el-button>
                    </template>
                  </el-table-column>
                </el-table>
                <el-button size="small" @click="addDataRow" style="margin-top: 8px">+ 添加</el-button>
              </el-form-item>
            </template>
            
            <template v-if="selectedNode.type === 'llm'">
              <el-form-item label="供应商">
                <el-select v-model="selectedNode.data.config.ProviderName" placeholder="选择供应商">
                  <el-option v-for="p in llmProviders" :key="p.id" :label="p.name" :value="p.name" />
                </el-select>
              </el-form-item>
              <el-form-item label="模型">
                <el-input v-model="selectedNode.data.config.Model" placeholder="gpt-3.5-turbo" />
              </el-form-item>
              <el-form-item label="系统提示">
                <el-input v-model="selectedNode.data.config.SystemPrompt" type="textarea" :rows="3" />
              </el-form-item>
              <el-form-item label="用户提示">
                <el-input 
                  v-model="selectedNode.data.config.UserPrompt" 
                  type="textarea" 
                  :rows="3" 
                  placeholder="输入提示词，可使用变量 / 选择上游节点变量"
                  @keydown="handleInputKeydown($event, 'config.UserPrompt')"
                  data-field="config.UserPrompt"
                />
              </el-form-item>
              <el-form-item label="温度">
                <el-slider v-model="selectedNode.data.config.Temperature" :min="0" :max="2" :step="0.1" show-stops />
              </el-form-item>
            </template>
            
            <template v-if="selectedNode.type === 'condition'">
              <el-form-item label="条件">
                <el-table :data="getConditionData()" size="small" stripe>
                  <el-table-column prop="left" label="左值" min-width="120">
                    <template #default="{ row }">
                      <el-input v-model="row.left" placeholder="变量名 / 输入/选择变量" @change="updateConditionData" @keydown="handleInputKeydown($event, 'condition-row-' + row._uuid + '-left')" :data-field="'condition-row-' + row._uuid + '-left'" />
                    </template>
                  </el-table-column>
                  <el-table-column prop="operator" label="运算符" width="100">
                    <template #default="{ row }">
                      <el-select v-model="row.operator" placeholder="选择" @change="updateConditionData">
                        <el-option label="==" value="==" />
                        <el-option label="!=" value="!=" />
                        <el-option label="<" value="<" />
                        <el-option label=">" value=">" />
                        <el-option label="<=" value="<=" />
                        <el-option label=">=" value=">=" />
                      </el-select>
                    </template>
                  </el-table-column>
                  <el-table-column prop="right" label="右值" min-width="120">
                    <template #default="{ row }">
                      <el-input v-model="row.right" placeholder="比较值 / 输入/选择变量" @change="updateConditionData" @keydown="handleInputKeydown($event, 'condition-row-' + row._uuid + '-right')" :data-field="'condition-row-' + row._uuid + '-right'" />
                    </template>
                  </el-table-column>
                  <el-table-column width="60">
                    <template #default="{ $index }">
                      <el-button type="danger" link @click="deleteConditionRow($index)">删除</el-button>
                    </template>
                  </el-table-column>
                </el-table>
                <el-button size="small" @click="addConditionRow" style="margin-top: 8px">+ 添加条件</el-button>
              </el-form-item>
            </template>
            
            <template v-if="selectedNode.type === 'hello'">
              <el-form-item label="消息">
                <el-input
                  v-model="selectedNode.data.config.message"
                  type="textarea"
                  :rows="3"
                  placeholder="输入问候消息，可使用变量"
                  @keydown="handleInputKeydown($event, 'config.message')"
                  data-field="config.message"
                />
              </el-form-item>
            </template>
            
            <el-form-item>
              <el-button type="danger" @click="deleteSelectedNode">删除节点</el-button>
            </el-form-item>
          </el-form>
        </div>
        
        <!-- 自动补全下拉框 -->
        <div 
          v-if="showAutocomplete" 
          class="autocomplete-dropdown"
          :style="{ top: autocompletePosition.top + 'px', left: autocompletePosition.left + 'px' }"
        >
          <div 
            v-for="(item, index) in autocompleteSuggestions" 
            :key="index"
            class="autocomplete-item"
            @click="insertVariable(item)"
          >
            <span class="autocomplete-value">{{ item.value }}</span>
            <span class="autocomplete-label">{{ item.label }}</span>
          </div>
          <div v-if="autocompleteSuggestions.length === 0" class="autocomplete-empty">
            无可用变量
          </div>
        </div>
        
        <!-- 边配置 -->
        <div v-else-if="selectedEdge" class="edge-config">
          <h3>边配置</h3>
          <el-form label-width="100px">
            <el-form-item label="源节点">
              <el-input :value="selectedEdge.source" disabled />
            </el-form-item>
            <el-form-item label="目标节点">
              <el-input :value="selectedEdge.target" disabled />
            </el-form-item>
            <el-form-item label="标签">
              <el-input v-model="selectedEdge.label" placeholder="条件标签" />
            </el-form-item>
            <el-form-item>
              <el-button type="danger" @click="deleteSelectedEdge">删除边</el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>
      
      <!-- 运行结果面板 -->
      <div class="result-panel" v-if="(running || results.length > 0) && !selectedEdge && !selectedNode">
        <h3>运行结果</h3>
        <el-button v-if="running" type="danger" @click="stopWorkflow">停止</el-button>
        
        <div class="results-list">
          <el-card v-for="(result, index) in results" :key="index" class="result-item">
            <template #header>
              <span>{{ result.msg }}</span>
            </template>
<!--            <pre>{{ JSON.stringify(result.extData, null, 2) }}</pre>-->
          </el-card>
        </div>
        
        <div v-if="workflowStatus" class="status-info">
          <el-tag :type="getStatusType(workflowStatus.workflowStateName)">
            状态: {{ workflowStatus.workflowStateName }}
          </el-tag>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, h } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { VueFlow, useVueFlow } from '@vue-flow/core';
import { Controls, ControlButton} from '@vue-flow/controls';
import { Background} from "@vue-flow/background";
import { MiniMap } from "@vue-flow/minimap";
import '@vue-flow/core/dist/style.css';
import '@vue-flow/controls/dist/style.css';
import '@vue-flow/minimap/dist/style.css';
import { ElMessage } from 'element-plus';
import { ArrowDown } from '@element-plus/icons-vue';
import { fetchEventSource } from '@microsoft/fetch-event-source';
import { stopWorkflow as stopWorkflowAPI, loadWorkflow, saveWorkflow as saveWorkflowAPI, updateWorkflow } from '../api';
import { getLLMProviders } from '../api';
import Icon from "@/components/Icon.vue";

// 导入自定义节点组件
import StartNode from '../components/nodes/StartNode.vue';
import EndNode from '../components/nodes/EndNode.vue';
import HelloNode from '../components/nodes/HelloNode.vue';
import HttpNode from '../components/nodes/HttpNode.vue';
import ConditionNode from '../components/nodes/ConditionNode.vue';
import LLMNode from '../components/nodes/LLMNode.vue';

const route = useRoute();
const router = useRouter();
const { onConnect, addEdges, addNodes, removeNodes, removeEdges, getNodes, getEdges, toObject } = useVueFlow();

// 状态
const saving = ref(false);
const running = ref(false);
const nodes = ref([]);
const edges = ref([]);
const selectedNode = ref(null);
const selectedEdge = ref(null);
const results = ref([]);
const workflowStatus = ref(null);
const currentToken = ref(null);
const llmProviders = ref([]);
const workflowUuid = ref(null);  // 当前工作流的 UUID（编辑模式）
const workflowName = ref('未命名工作流');
// SSE 控制器，用于中断 SSE 连接
let sseAbortController = null;

// 自动补全相关状态
const showAutocomplete = ref(false);
const autocompletePosition = ref({ top: 0, left: 0 });
const autocompleteSuggestions = ref([]);
const activeInputField = ref('');

// 节点类型映射（前端type -> 后端code）
const nodeTypeMap = {
  'start': { label: '起点', color: '#67C23A', code: 0x00001 },
  'end': { label: '终点', color: '#E6A23C', code: 0x000002 },
  'hello': { label: '问候', color: '#909399', code: 0x000003 },
  'http': { label: 'HTTP请求', color: '#409EFF', code: 0x000005 },
  'condition': { label: '条件', color: '#E6A23C', code: 0x000004 },
  'llm': { label: '大模型', color: '#9C27B0', code: 0x000006 }
};

// 节点类型列表（用于下拉选择）
const nodeTypes = [
  { type: 'start', label: '起点', color: '#67C23A' },
  { type: 'end', label: '终点', color: '#E6A23C' },
  { type: 'hello', label: '问候', color: '#909399' },
  { type: 'http', label: 'HTTP请求', color: '#409EFF' },
  { type: 'condition', label: '条件', color: '#E6A23C' },
  { type: 'llm', label: '大模型', color: '#9C27B0' }
];

// 生命周期
onMounted(async () => {
  // 加载LLM供应商列表
  try {
    llmProviders.value = await getLLMProviders();
  } catch (e) {
    console.error('加载供应商失败:', e);
  }
  
  // 如果是编辑模式，从后端加载工作流
  const routeId = route.params.id;
  if (routeId && routeId !== 'new') {
    workflowUuid.value = routeId;
    await loadWorkflowFromServer(routeId);
  } else {
    // 新建模式：初始化默认节点
    if (nodes.value.length === 0) {
      initDefaultNodes();
    }
  }
});

// 从服务器加载工作流
async function loadWorkflowFromServer(uuid) {
  try {
    const result = await loadWorkflow(uuid);
    if (result.success && result.data) {
      const wf = result.data;
      workflowName.value = wf.name || '未命名工作流';
      
      // 将后端格式转换为 Vue Flow 格式
      const loadedNodes = (wf.nodes || []).map(n => {
        const frontendType = getNodeTypeFrontend(n.type);
        const config = buildConfigFromVO(n.configs || [], frontendType);
        return {
          id: n.id,
          type: frontendType,
          position: n.position || { x: 250, y: 200 },
          data: {
            label: n.id,
            customLabel: n.label || frontendType,
            config
          }
        };
      });
      
      const loadedEdges = (wf.edges || []).map((e, index) => ({
        id: `edge-${index}`,
        source: e.from,
        target: e.to,
        sourceHandle: e.sourceHandle || null,
        targetHandle: e.targetHandle || null
      }));
      
      addNodes(loadedNodes);
      addEdges(loadedEdges);
    } else {
      ElMessage.error(result.msg || '加载工作流失败');
      initDefaultNodes();
    }
  } catch (e) {
    ElMessage.error('加载工作流失败: ' + e.message);
    initDefaultNodes();
  }
}

// 将后端节点类型代码转换为前端类型字符串
function getNodeTypeFrontend(typeCode) {
  for (const [key, val] of Object.entries(nodeTypeMap)) {
    if (val.code === typeCode) return key;
  }
  return 'hello';
}

// 将 ConfigVO 列表转换为前端 config 对象
function buildConfigFromVO(configs, nodeType) {
  const config = {};
  
  if (nodeType === 'condition') {
    // 条件节点：将多个 Condition 类型的 ConfigVO 转换为 _conditions 数组
    const conditions = configs
      .filter(c => c.type === 'Condition')
      .map(c => {
        try {
          const parsed = JSON.parse(c.value || '{}');
          return {
            _uuid: Date.now() + Math.random().toString(36).substr(2, 9),
            left: parsed.a || '',
            operator: parsed.operator || '==',
            right: parsed.b || '',
            _nextNodes: parsed.nextNodes || []
          };
        } catch {
          return { _uuid: Date.now() + Math.random().toString(36).substr(2, 9), left: '', operator: '==', right: '' };
        }
      });
    config._conditions = conditions;
    config.Condition = JSON.stringify(conditions);
    return config;
  }
  
  // 其他节点类型：将 ConfigVO 列表转换为 key-value 对象
  configs.forEach(c => {
    if (c.type === 'Number') {
      config[c.name] = c.k && c.k > 1 ? parseInt(c.value) / c.k : parseInt(c.value);
    } else {
      config[c.name] = c.value || '';
    }
  });
  
  return config;
}

// 初始化默认节点
function initDefaultNodes() {
  // 添加起点节点
  addNodes({
    id: 'start-1',
    type: 'start',
    position: { x: 250, y: 50 },
    data: { label: 'start-1', customLabel: '开始', config: {} }
  });
}

// 添加节点
function handleAddNode(nodeType) {
  const id = `${nodeType.toLowerCase()}-${Date.now()}`;
  const typeInfo = nodeTypes.find(t => t.type === nodeType);
  
  let position = { x: 250, y: 200 };
  // 安全地获取最后一个节点的位置
  const currentNodes = nodes.value || [];
  if (currentNodes.length > 0) {
    const lastNode = currentNodes[currentNodes.length - 1];
    if (lastNode && lastNode.position) {
      position = { 
        x: (lastNode.position.x || 250) + 100,
        y: (lastNode.position.y || 200)
      };
    }
  }
  const newNode = {
    id,
    type: nodeType,
    position,
    data: { 
      label: id, 
      customLabel: typeInfo?.label || nodeType,
      config: getDefaultConfig(nodeType)
    }
  };
  addNodes(newNode);
}

// 获取节点默认配置
function getDefaultConfig(nodeType) {
  switch (nodeType) {
    case 'http':
      return { Url: '', Method: 'GET', Headers: '{}', Data: '{}' };
    case 'llm':
      return { ProviderName: '', Model: 'gpt-3.5-turbo', SystemPrompt: '', UserPrompt: '', Temperature: 0.7 };
    case 'condition':
      return { Condition: '' };
    case 'hello':
      return { message: 'Hello, world' };
    default:
      return {};
  }
}

// 连接处理
function onConnectHandler(params) {
  addEdges([params]);
}

// 节点点击
function onNodeClick(event) {
  selectedNode.value = event.node;
  selectedEdge.value = null;
  running.value = false;
}

// 边点击
function onEdgeClick(event) {
  selectedEdge.value = event.edge;
  selectedNode.value = null;
}

// 选择变更
function onSelectionChange(event) {
  if (!event || event.length === 0) {
    selectedNode.value = null;
    selectedEdge.value = null;
  }
}

// 获取节点类型名称
function getNodeTypeName(type) {
  const typeInfo = nodeTypes.find(t => t.type === type);
  return typeInfo?.label || type;
}

// 获取节点类型代码（用于后端）
function getNodeTypeCode(type) {
  const info = nodeTypeMap[type];
  return info ? info.code : null;
}

// 获取当前节点的上游节点输出变量
function getUpstreamVariables() {
  if (!selectedNode.value) return [];
  
  const nodeId = selectedNode.value.id;
  const upstreamNodes = [];
  
  // 找出所有连接到当前节点的源节点
  edges.value.forEach(edge => {
    if (edge.target === nodeId) {
      const sourceNode = nodes.value.find(n => n.id === edge.source);
      if (sourceNode) {
        upstreamNodes.push(sourceNode);
      }
    }
  });
  
  // 为每个上游节点生成输出变量
  const variables = [];
  upstreamNodes.forEach(node => {
    // 根据节点类型添加不同的输出变量
    const outputs = getNodeOutputs(node);
    outputs.forEach(output => {
      variables.push({
        nodeId: node.id,
        nodeLabel: node.data?.label || node.id,
        variableName: `${node.id}:${output.key}`,
        label: `${node.data?.label || node.id} - ${output.label}`
      });
    });
  });
  
  return variables;
}

// 获取节点的输出变量定义
function getNodeOutputs(node) {
  const outputs = [];
  
  switch (node.type) {
    case 'start':
      outputs.push({ key: 'output', label: '输出' });
      break;
    case 'hello':
      outputs.push({ key: 'output', label: '消息' });
      break;
    case 'http':
      outputs.push({ key: 'output', label: '响应内容' });
      outputs.push({ key: 'statusCode', label: '状态码' });
      outputs.push({ key: 'success', label: '是否成功' });
      break;
    case 'llm':
      outputs.push({ key: 'output', label: '回复内容' });
      outputs.push({ key: 'model', label: '使用模型' });
      outputs.push({ key: 'provider', label: '供应商' });
      break;
    case 'condition':
      outputs.push({ key: 'result', label: '判断结果' });
      break;
  }
  
  return outputs;
}

// 获取变量建议列表（用于自动补全）
function getVariableSuggestions() {
  return getUpstreamVariables().map(v => ({
    value: `{{${v.variableName}}}`,
    label: v.label
  }));
}

// 处理输入框按键事件（检测/触发自动补全）
function handleInputKeydown(event, fieldName) {
  const input = event.target;
  const cursorPos = input.selectionStart;
  const textBeforeCursor = input.value.substring(0, cursorPos);
  
  // 检测是否输入了/
  if (event.key === '/') {
    const textAfterSlash = textBeforeCursor.substring(textBeforeCursor.lastIndexOf('/') + 1);
    // 只有当/是最近的一个/时才触发
    if (!textAfterSlash.includes('{{')) {
      showAutocomplete.value = true;
      autocompleteSuggestions.value = getVariableSuggestions();
      activeInputField.value = fieldName;
      
      // 计算下拉框位置
      const rect = input.getBoundingClientRect();
      autocompletePosition.value = {
        top: rect.bottom + window.scrollY + 5,
        left: rect.left + window.scrollX
      };
    }
  } else if (showAutocomplete.value) {
    // 如果自动补全已显示，按Escape关闭
    if (event.key === 'Escape') {
      showAutocomplete.value = false;
    }
  }
}

// 插入选中的变量
function insertVariable(suggestion) {
  if (!selectedNode.value) return;
  
  // 如果有活跃的输入字段，直接插入
  if (activeInputField.value) {
    insertVariableToField(activeInputField.value, suggestion.value);
  } else {
    // 如果没有活跃字段，尝试找到第一个文本输入字段
    const textField = findFirstTextField();
    if (textField) {
      insertVariableToField(textField, suggestion.value);
    }
  }
  
  showAutocomplete.value = false;
}

// 插入变量到指定字段
function insertVariableToField(fieldName, variableValue) {
  if (!selectedNode.value) return;
  
  // 通过DOM找到输入框
  const input = document.querySelector(`[data-field="${fieldName}"]`);
  if (!input) {
    // 如果找不到DOM，直接通过data更新
    updateNodeDataField(fieldName, variableValue);
    return;
  }
  
  const cursorPos = input.selectionStart || input.value.length;
  const textBeforeCursor = input.value.substring(0, cursorPos);
  const textAfterCursor = input.value.substring(cursorPos);
  
  // 找到/的位置，用变量替换（如果没有/，则追加到末尾）
  const lastSlashPos = textBeforeCursor.lastIndexOf('/');
  let newValue;
  if (lastSlashPos >= 0) {
    newValue = textBeforeCursor.substring(0, lastSlashPos) + variableValue + textAfterCursor;
  } else {
    newValue = textBeforeCursor + variableValue + textAfterCursor;
  }
  
  // 更新字段
  updateNodeDataField(fieldName, newValue);
}

// 通过字段路径更新节点数据
function updateNodeDataField(fieldPath, value) {
  if (!selectedNode.value) return;
  
  if (fieldPath.startsWith('config.')) {
    const path = fieldPath.substring(7);
    const parts = path.split('.');
    let obj = selectedNode.value.data;
    for (let i = 0; i < parts.length - 1; i++) {
      if (!obj[parts[i]]) obj[parts[i]] = {};
      obj = obj[parts[i]];
    }
    obj[parts[parts.length - 1]] = value;
  } else if (fieldPath.startsWith('data-row-')) {
    // 处理Table行的值 - 格式: data-row-{uuid}-value
    const match = fieldPath.match(/data-row-(\w+)-(\w+)/);
    if (match) {
      const [, uuid, field] = match;
      // 找到对应的行
      const tableData = getDataData();
      const row = tableData.find(r => r._uuid === uuid);
      if (row) {
        row[field] = value;
        updateDataData();
      }
    }
  } else if (fieldPath.startsWith('condition-row-')) {
    // 处理Condition表格行的值 - 格式: condition-row-{uuid}-{field}
    const match = fieldPath.match(/condition-row-(\w+)-(\w+)/);
    if (match) {
      const [, uuid, field] = match;
      const tableData = getConditionData();
      const row = tableData.find(r => r._uuid === uuid);
      if (row) {
        row[field] = value;
        updateConditionData();
      }
    }
  }
}

// 找到第一个文本输入字段
function findFirstTextField() {
  if (!selectedNode.value) return null;
  const type = selectedNode.value.type;
  
  if (type === 'http') return 'config.Url';
  if (type === 'llm') return 'config.UserPrompt';
  if (type === 'hello') return 'config.message';
  
  return null;
}

// Headers表格数据处理
function getHeadersData() {
  if (!selectedNode.value) return [];
  const config = selectedNode.value.data.config;
  if (!config._headers) {
    // 从旧的Headers字段解析
    try {
      const old = JSON.parse(config.Headers || '{}');
      config._headers = Object.entries(old).map(([key, value]) => ({ key, value: String(value) }));
    } catch {
      config._headers = [];
    }
  }
  return config._headers;
}

function updateHeadersData() {
  const data = getHeadersData();
  const obj = {};
  data.forEach(item => {
    if (item.key) obj[item.key] = item.value;
  });
  selectedNode.value.data.config.Headers = JSON.stringify(obj);
}

function addHeadersRow() {
  const data = getHeadersData();
  data.push({ _uuid: Date.now() + Math.random().toString(36).substr(2, 9), key: '', value: '' });
  updateHeadersData();
}

function deleteHeadersRow(index) {
  const data = getHeadersData();
  data.splice(index, 1);
  updateHeadersData();
}

// Data表格数据处理
function getDataData() {
  if (!selectedNode.value) return [];
  const config = selectedNode.value.data.config;
  if (!config._data) {
    try {
      const old = JSON.parse(config.Data || '{}');
      config._data = Object.entries(old).map(([key, value]) => ({ key, value: String(value) }));
    } catch {
      config._data = [];
    }
  }
  return config._data;
}

function updateDataData() {
  const data = getDataData();
  const obj = {};
  data.forEach(item => {
    if (item.key) obj[item.key] = item.value;
  });
  selectedNode.value.data.config.Data = JSON.stringify(obj);
}

function addDataRow() {
  const data = getDataData();
  data.push({ _uuid: Date.now() + Math.random().toString(36).substr(2, 9), key: '', value: '' });
  updateDataData();
}

function deleteDataRow(index) {
  const data = getDataData();
  data.splice(index, 1);
  updateDataData();
}

// Condition表格数据处理
function getConditionData() {
  if (!selectedNode.value) return [];
  const config = selectedNode.value.data.config;
  if (!config._conditions) {
    try {
      config._conditions = JSON.parse(config.Condition || '[]');
    } catch {
      config._conditions = [];
    }
  }
  return config._conditions;
}

function updateConditionData() {
  const data = getConditionData();
  selectedNode.value.data.config.Condition = JSON.stringify(data);
}

function addConditionRow() {
  const data = getConditionData();
  data.push({ _uuid: Date.now() + Math.random().toString(36).substr(2, 9), left: '', operator: '==', right: '' });
  updateConditionData();
}

function deleteConditionRow(index) {
  const data = getConditionData();
  data.splice(index, 1);
  updateConditionData();
}

// 删除选中节点
function deleteSelectedNode() {
  if (selectedNode.value) {
    removeNodes([selectedNode.value.id]);
    selectedNode.value = null;
  }
}

// 删除选中边
function deleteSelectedEdge() {
  if (selectedEdge.value) {
    removeEdges([selectedEdge.value.id]);
    selectedEdge.value = null;
  }
}

// 返回
function goBack() {
  router.push('/workflows');
}

// 将节点配置转换为后端期望的 ConfigVO 列表格式
function buildNodeConfigs(node) {
  const config = node.data.config || {};
  
  if (node.type === 'condition') {
    // 条件节点：将 _conditions 数组转换为多个独立的 ConfigVO（type=Condition）
    const conditions = config._conditions || [];
    return conditions.map((cond, index) => {
      // 根据 edge.sourceHandle 确定该条件对应的 nextNodes
      const handleId = cond._uuid || `condition-${index}`;
      const nextNodes = edges.value
        .filter(e => e.source === node.id && e.sourceHandle === handleId)
        .map(e => e.target);
      
      return {
        name: `condition-${index}`,
        type: 'Condition',
        value: JSON.stringify({
          a: cond.left,
          operator: cond.operator,
          b: cond.right,
          nextNodes
        })
      };
    });
  }
  
  if (node.type === 'http') {
    return [
      { name: 'Url', type: 'String', value: config.Url || '' },
      { name: 'Method', type: 'String', value: config.Method || 'GET' },
      { name: 'Headers', type: 'Map', value: config.Headers || '{}' },
      { name: 'Data', type: 'Map', value: config.Data || '{}' }
    ];
  }
  
  if (node.type === 'llm') {
    return [
      { name: 'ProviderName', type: 'String', value: config.ProviderName || '' },
      { name: 'Model', type: 'String', value: config.Model || '' },
      { name: 'SystemPrompt', type: 'String', value: config.SystemPrompt || '' },
      { name: 'UserPrompt', type: 'String', value: config.UserPrompt || '' },
      { name: 'Temperature', type: 'Number', value: String(Math.round((config.Temperature || 0.7) * 10)), k: 10, quantize: 1 }
    ];
  }
  
  if (node.type === 'hello') {
    return [
      { name: 'message', type: 'String', value: config.message || '' }
    ];
  }
  
  // 其他节点类型：将 config 对象的每个字段转换为 ConfigVO
  return Object.entries(config)
    .filter(([key]) => !key.startsWith('_'))
    .map(([key, value]) => ({
      name: key,
      type: 'String',
      value: typeof value === 'string' ? value : JSON.stringify(value)
    }));
}

// 保存工作流
async function saveWorkflow() {
  saving.value = true;
  try {
    const workflowData = buildWorkflowData();
    workflowData.name = workflowName.value;
    
    let result;
    if (workflowUuid.value) {
      // 编辑模式：更新已有工作流
      result = await updateWorkflow(workflowUuid.value, workflowData);
    } else {
      // 新建模式：创建新工作流
      result = await saveWorkflowAPI(workflowData);
    }
    
    if (result.success) {
      ElMessage.success('工作流已保存');
    } else {
      ElMessage.error(result.msg || '保存失败');
    }
  } catch (e) {
    ElMessage.error('保存失败: ' + e.message);
  } finally {
    saving.value = false;
  }
}

// 构建后端期望的工作流数据格式
function buildWorkflowData() {
  nodes.value = nodes.value.filter(node => node.type !== "default");
  return {
    nodes: nodes.value.map(n => ({
      id: n.id,
      type: getNodeTypeCode(n.type),  // 转换为后端code
      configs: buildNodeConfigs(n),
      position: n.position,
      label: n.data?.customLabel || n.data?.label || n.id
    })),
    edges: edges.value.map(e => ({
      from: e.source,
      to: e.target,
      sourceHandle: e.sourceHandle || null,
      targetHandle: e.targetHandle || null
    }))
  };
}

// 工作流状态码常量（与后端保持一致）
const WorkflowStates = {
  NULL: -1,
  STAND_BY: 0x00,
  ERROR: 0x01,
  RUNNING: 0x02,
  DONE: 0x10,
  ABORT: 0x11
};

const NodeStates = {
  NULL: -1,
  STAND_BY: 0x000,
  ERROR: 0x001,
  RUNNING: 0x002,
  DONE: 0x100,
  DISABLED: 0x200
};

// 运行工作流（使用 FetchEventSource 订阅 SSE）
async function runWorkflow() {
  if (nodes.value.length === 0) {
    ElMessage.warning('请先添加节点');
    return;
  }
  
  // 如果已有 SSE 连接，先关闭
  closeSseConnection();
  
  running.value = true;
  results.value = [];
  workflowStatus.value = null;
  
  // 重置所有边状态为pending
  edges.value = edges.value.map(e => ({
    ...e,
    data: { ...e.data, state: 'pending' },
    animated: false,
    style: { stroke: '#999', strokeWidth: 2, strokeDasharray: '5,5' }
  }));
  
  const workflowData = buildWorkflowData();
  
  // 创建 AbortController 用于中断 SSE 连接
  sseAbortController = new AbortController();
  
  try {
    await fetchEventSource('/api/v3/workflow/execute', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(workflowData),
      signal: sseAbortController.signal,
      
      // 连接建立时
      onopen(response) {
        if (response.ok) {
          ElMessage.success('工作流已开始运行');
        } else {
          throw new Error(`连接失败: ${response.status}`);
        }
      },
      
      // 收到消息时
      onmessage(event) {
        if (event.event === 'connected') {
          // 初始连接消息，提取 token
          try {
            const data = JSON.parse(event.data);
            if (data.token) {
              currentToken.value = data.token;
            }
          } catch (e) {
            console.error('解析连接消息失败:', e);
          }
          return;
        }
        
        if (event.event === 'heartbeat') {
          return;
        }
        
        if (event.event === 'workflow-result') {
          try {
            const result = JSON.parse(event.data);
            handleWorkflowResult(result);
          } catch (e) {
            console.error('解析工作流结果失败:', e);
          }
        }
      },
      
      // 连接关闭时
      onclose() {
        running.value = false;
        console.log('SSE 连接已关闭');
      },
      
      // 连接出错时
      onerror(err) {
        if (err.name === 'AbortError') {
          // 主动中断，不报错
          return;
        }
        console.error('SSE 连接错误:', err);
        ElMessage.error('工作流连接出错');
        running.value = false;
        throw err; // 抛出错误以停止重试
      }
    });
  } catch (e) {
    if (e.name !== 'AbortError') {
      ElMessage.error('运行失败: ' + e.message);
    }
    running.value = false;
  }
}

// 处理工作流结果事件
function handleWorkflowResult(result) {
  // 添加到结果列表
  results.value.push(result);
  
  // 更新工作流状态
  const state = result.state;
  
  // 判断工作流是否结束
  if ((state & WorkflowStates.DONE) !== 0 || (state & WorkflowStates.ERROR) !== 0 || (state & WorkflowStates.ABORT) !== 0) {
    workflowStatus.value = {
      workflowState: state,
      workflowStateName: getWorkflowStateName(state),
      workflowEnded: true
    };
    running.value = false;
    closeSseConnection();
  } else if ((state & WorkflowStates.RUNNING) !== 0) {
    workflowStatus.value = {
      workflowState: state,
      workflowStateName: 'RUNNING',
      workflowEnded: false
    };
  }
  
  // 更新边的状态（节点完成时）
  if (result.nodeId && (state & NodeStates.DONE) !== 0) {
    updateEdgeStateForNode(result.nodeId);
  }
}

// 更新指定节点相关边的状态
function updateEdgeStateForNode(nodeId) {
  edges.value = edges.value.map(e => {
    if (e.source === nodeId) {
      return {
        ...e,
        data: { ...e.data, state: 'done' },
        animated: false,
        style: { stroke: '#67C23A', strokeWidth: 2, strokeDasharray: 'none' }
      };
    }
    return e;
  });
}

// 关闭 SSE 连接
function closeSseConnection() {
  if (sseAbortController) {
    sseAbortController.abort();
    sseAbortController = null;
  }
}

// 停止工作流
async function stopWorkflow() {
  if (currentToken.value) {
    try {
      await stopWorkflowAPI(currentToken.value);
      ElMessage.success('工作流已停止');
    } catch (e) {
      ElMessage.error('停止失败: ' + e.message);
    }
  }
  running.value = false;
  closeSseConnection();
}

// 获取工作流状态名称
function getWorkflowStateName(state) {
  if ((state & WorkflowStates.ERROR) !== 0) return 'ERROR';
  if ((state & WorkflowStates.DONE) !== 0) return 'DONE';
  if ((state & WorkflowStates.ABORT) !== 0) return 'ABORT';
  if ((state & WorkflowStates.RUNNING) !== 0) return 'RUNNING';
  if ((state & WorkflowStates.STAND_BY) !== 0) return 'STAND_BY';
  return 'UNKNOWN';
}

// 获取状态类型
function getStatusType(statusName) {
  switch (statusName) {
    case 'RUNNING': return 'success';
    case 'DONE': return 'info';
    case 'ERROR': return 'danger';
    case 'ABORT': return 'warning';
    default: return '';
  }
}

function logToObject(){
  console.log(toObject());
}
</script>

<style scoped>
.workflow-editor {
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.toolbar {
  padding: 10px;
  background: #fff;
  border-bottom: 1px solid #ddd;
  display: flex;
  align-items: center;
  gap: 10px;
}

.editor-container {
  flex: 1;
  display: flex;
  position: relative;
}

.canvas-wrapper {
  flex: 1;
  height: 100%;
}

.config-panel {
  width: 350px;
  background: #fff;
  border-left: 1px solid #ddd;
  padding: 20px;
  overflow-y: auto;
  position: relative;
}

.result-panel {
  position: absolute;
  right: 0;
  top: 0;
  bottom: 0;
  width: 400px;
  background: #fff;
  border-left: 1px solid #ddd;
  padding: 20px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

.results-list {
  flex: 1;
  overflow-y: auto;
  margin-top: 10px;
}

.result-item {
  margin-bottom: 10px;
}

.result-item pre {
  max-height: 200px;
  overflow: auto;
}

.status-info {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid #eee;
}

h3 {
  margin-top: 0;
  margin-bottom: 20px;
}

/* 边样式 - 未运行 */
:deep(.vue-flow__edge) {
  stroke: #999;
  stroke-width: 2;
}

:deep(.vue-flow__edge.pending) .vue-flow__edge-path {
  stroke: #999;
  stroke-dasharray: 5, 5;
}

/* 边样式 - 运行中 */
:deep(.vue-flow__edge.running) .vue-flow__edge-path {
  stroke: #E6A23C;
  stroke-dasharray: 5, 5;
  animation: dash 0.5s linear infinite;
}

/* 边样式 - 已完成 */
:deep(.vue-flow__edge.done) .vue-flow__edge-path {
  stroke: #67C23A;
  stroke-dasharray: none;
}

@keyframes dash {
  to {
    stroke-dashoffset: -10;
  }
}

/* Vue Flow Handle 默认样式 */
:deep(.vue-flow__handle) {
  width: 10px;
  height: 10px;
  background: #409EFF;
  border: 2px solid #fff;
}

:deep(.vue-flow__handle-connecting) {
  background: #67C23A;
}

:deep(.vue-flow__handle-connected) {
  background: #67C23A;
}

/* 上游变量显示区域 */
.upstream-variables {
  background: #f5f7fa;
  border-radius: 4px;
  padding: 12px;
  margin-bottom: 16px;
}

.upstream-variables h4 {
  margin: 0 0 8px 0;
  font-size: 14px;
  color: #303133;
}

.variable-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.variable-tag {
  cursor: pointer;
}

.variable-hint {
  margin: 8px 0 0 0;
  font-size: 12px;
  color: #909399;
}

/* 自动补全下拉框 */
.autocomplete-dropdown {
  position: fixed;
  z-index: 9999;
  background: #fff;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  max-height: 200px;
  overflow-y: auto;
  min-width: 250px;
}

.autocomplete-item {
  padding: 8px 12px;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #f0f0f0;
}

.autocomplete-item:last-child {
  border-bottom: none;
}

.autocomplete-item:hover {
  background: #f5f7fa;
}

.autocomplete-value {
  font-family: monospace;
  color: #409EFF;
  font-weight: bold;
}

.autocomplete-label {
  color: #909399;
  font-size: 12px;
}

.autocomplete-empty {
  padding: 12px;
  text-align: center;
  color: #909399;
}
</style>
