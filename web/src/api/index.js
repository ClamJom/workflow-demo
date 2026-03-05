import axios from 'axios';

// 创建axios实例
const api = axios.create({
  baseURL: '/api/v3',
  timeout: 60000,
  headers: {
    'Content-Type': 'application/json'
  }
});

// 请求拦截器
api.interceptors.request.use(
  config => {
    return config;
  },
  error => {
    return Promise.reject(error);
  }
);

// 响应拦截器
api.interceptors.response.use(
  response => {
    return response.data;
  },
  error => {
    console.error('API Error:', error);
    return Promise.reject(error);
  }
);

// ========== 工作流API ==========

/**
 * 执行工作流（SSE模式）
 */
export function executeWorkflowSSE(workflowData) {
  return api.post('/workflow/execute', workflowData, {
    responseType: 'text/event-stream'
  });
}

/**
 * 执行工作流（同步模式）
 */
export function executeWorkflowSync(workflowData) {
  return api.post('/workflow/execute-sync', workflowData);
}

/**
 * 获取工作流状态
 */
export function getWorkflowStatus(token) {
  return api.get(`/workflow/status/${token}`);
}

/**
 * 获取所有节点类型
 */
export function getNodeTypes(){
  return api.get(`/workflow/nodes`);
}

/**
 * 获取节点状态
 */
export function getNodeStatus(token, nodeId) {
  return api.get(`/workflow/node-status/${token}/${nodeId}`);
}

/**
 * 通过节点类型代码获取节点基础配置
 */
export function getNodeConfigs(nodeType){
  return api.get(`workflow/node-config/${nodeType}`);
}

/**
 * 获取全局变量池
 */
export function getVariables(token) {
  return api.get(`/workflow/variables/${token}`);
}

/**
 * 获取工作流结果
 */
export function getWorkflowResults(token) {
  return api.get(`/workflow/results/${token}`);
}

/**
 * 终止工作流
 */
export function stopWorkflow(token) {
  return api.post(`/workflow/stop/${token}`);
}

/**
 * 获取工作流列表
 */
export function listWorkflows() {
  return api.get('/workflow/list');
}

/**
 * 保存工作流（新建）
 */
export function saveWorkflow(workflowData) {
  return api.post('/workflow/save', workflowData);
}

/**
 * 更新工作流
 */
export function updateWorkflow(uuid, workflowData) {
  return api.put(`/workflow/save/${uuid}`, workflowData);
}

/**
 * 加载工作流
 */
export function loadWorkflow(uuid) {
  return api.get(`/workflow/load/${uuid}`);
}

/**
 * 删除工作流
 */
export function deleteWorkflowFile(uuid) {
  return api.delete(`/workflow/delete/${uuid}`);
}

/**
 * 获取SSE连接状态
 */
export function getSseStatus(token) {
  return api.get(`/workflow/sse-status/${token}`);
}

// ========== LLM供应商API ==========

/**
 * 获取所有启用的LLM供应商
 */
export function getLLMProviders() {
  return api.get('/llm/providers');
}

/**
 * 获取默认供应商
 */
export function getDefaultProvider() {
  return api.get('/llm/providers/default');
}

/**
 * 根据ID获取供应商
 */
export function getProviderById(id) {
  return api.get(`/llm/providers/${id}`);
}

/**
 * 创建LLM供应商
 */
export function createProvider(provider) {
  return api.post('/llm/providers', provider);
}

/**
 * 更新LLM供应商
 */
export function updateProvider(id, provider) {
  return api.put(`/llm/providers/${id}`, provider);
}

/**
 * 删除LLM供应商
 */
export function deleteProvider(id) {
  return api.delete(`/llm/providers/${id}`);
}

/**
 * 设置默认供应商
 */
export function setDefaultProvider(id) {
  return api.post(`/llm/providers/${id}/set-default`);
}

/**
 * 初始化默认供应商
 */
export function initDefaultProviders() {
  return api.post('/llm/providers/init-default');
}

// ========== 测试API ==========

/**
 * 测试接口
 */
export function testAPI(data) {
  return api.post('/test', data);
}

export default {
  // 工作流
  executeWorkflowSSE,
  executeWorkflowSync,
  getWorkflowStatus,
  getNodeStatus,
  getVariables,
  getWorkflowResults,
  stopWorkflow,
  getSseStatus,
  listWorkflows,
  saveWorkflow,
  updateWorkflow,
  loadWorkflow,
  deleteWorkflowFile,
  
  // LLM供应商
  getLLMProviders,
  getDefaultProvider,
  getProviderById,
  createProvider,
  updateProvider,
  deleteProvider,
  setDefaultProvider,
  initDefaultProviders,
  
  // 测试
  testAPI
};
