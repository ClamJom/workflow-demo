import { createPinia, defineStore } from 'pinia';
import { getLLMProviders, createProvider, updateProvider, deleteProvider, setDefaultProvider } from '../api';

const pinia = createPinia();

export default pinia;

// 工作流Store
export const useWorkflowStore = defineStore('workflow', {
  state: () => ({
    // 当前工作流数据
    currentWorkflow: null,
    // 节点列表
    nodes: [],
    // 边列表
    edges: [],
    // 当前选中的节点
    selectedNode: null,
    // 运行状态
    isRunning: false,
    // 当前token
    currentToken: null,
    // 运行结果
    results: [],
    // 变量池
    variables: {},
    // 工作流状态
    workflowStatus: null,
    // SSE事件源
    eventSource: null
  }),
  
  getters: {
    // 获取节点类型列表
    nodeTypes: () => [
      { type: 'start', label: '起点', color: '#67C23A' },
      { type: 'end', label: '终点', color: '#E6A23C' },
      { type: 'hello', label: '问候', color: '#909399' },
      { type: 'http', label: 'HTTP请求', color: '#409EFF' },
      { type: 'condition', label: '条件', color: '#E6A23C' },
      { type: 'llm', label: '大模型', color: '#9C27B0' }
    ]
  },
  
  actions: {
    // 添加节点
    addNode(node) {
      this.nodes.push(node);
    },
    
    // 更新节点
    updateNode(nodeId, data) {
      const index = this.nodes.findIndex(n => n.id === nodeId);
      if (index !== -1) {
        this.nodes[index] = { ...this.nodes[index], ...data };
      }
    },
    
    // 删除节点
    removeNode(nodeId) {
      this.nodes = this.nodes.filter(n => n.id !== nodeId);
      // 同时删除相关的边
      this.edges = this.edges.filter(e => e.source !== nodeId && e.target !== nodeId);
    },
    
    // 添加边
    addEdge(edge) {
      this.edges.push(edge);
    },
    
    // 删除边
    removeEdge(edgeId) {
      this.edges = this.edges.filter(e => e.id !== edgeId);
    },
    
    // 设置选中的节点
    setSelectedNode(node) {
      this.selectedNode = node;
    },
    
    // 加载工作流
    loadWorkflow(workflow) {
      this.currentWorkflow = workflow;
      this.nodes = workflow.nodes || [];
      this.edges = workflow.edges || [];
    },
    
    // 清空工作流
    clearWorkflow() {
      this.currentWorkflow = null;
      this.nodes = [];
      this.edges = [];
      this.selectedNode = null;
      this.isRunning = false;
      this.currentToken = null;
      this.results = [];
      this.variables = {};
      this.workflowStatus = null;
    },
    
    // 添加结果
    addResult(result) {
      this.results.push(result);
    },
    
    // 更新变量
    updateVariables(newVariables) {
      this.variables = { ...this.variables, ...newVariables };
    },
    
    // 设置运行状态
    setRunning(running, token = null) {
      this.isRunning = running;
      this.currentToken = token;
    },
    
    // 设置工作流状态
    setWorkflowStatus(status) {
      this.workflowStatus = status;
    },
    
    // 连接到SSE
    connectSSE(token) {
      this.disconnectSSE();
      this.currentToken = token;
      
      // 暂时使用轮询方式
      this.isRunning = true;
    },
    
    // 断开SSE
    disconnectSSE() {
      if (this.eventSource) {
        this.eventSource.close();
        this.eventSource = null;
      }
      this.isRunning = false;
    }
  }
});

// LLM供应商Store
export const useLLMStore = defineStore('llm', {
  state: () => ({
    providers: [],
    defaultProvider: null,
    loading: false
  }),
  
  actions: {
    async fetchProviders() {
      this.loading = true;
      try {
        const data = await getLLMProviders();
        this.providers = data;
      } catch (error) {
        console.error('获取供应商失败:', error);
      } finally {
        this.loading = false;
      }
    },
    
    async fetchDefaultProvider() {
      try {
        const data = await getDefaultProvider();
        if (data.success) {
          this.defaultProvider = data.data;
        }
      } catch (error) {
        console.error('获取默认供应商失败:', error);
      }
    },
    
    async createProviderAction(provider) {
      try {
        const result = await createProvider(provider);
        if (result.success) {
          this.providers.push(result.data);
          return result;
        }
        return result;
      } catch (error) {
        console.error('创建供应商失败:', error);
        throw error;
      }
    },
    
    async updateProviderAction(id, provider) {
      try {
        const result = await updateProvider(id, provider);
        if (result.success) {
          const index = this.providers.findIndex(p => p.id === id);
          if (index !== -1) {
            this.providers[index] = result.data;
          }
        }
        return result;
      } catch (error) {
        console.error('更新供应商失败:', error);
        throw error;
      }
    },
    
    async deleteProviderAction(id) {
      try {
        const result = await deleteProvider(id);
        if (result.success) {
          this.providers = this.providers.filter(p => p.id !== id);
        }
        return result;
      } catch (error) {
        console.error('删除供应商失败:', error);
        throw error;
      }
    },
    
    async setDefaultProviderAction(id) {
      try {
        const result = await setDefaultProvider(id);
        if (result.success) {
          this.providers.forEach(p => {
            p.isDefault = p.id === id;
          });
        }
        return result;
      } catch (error) {
        console.error('设置默认供应商失败:', error);
        throw error;
      }
    }
  }
});
