<template>
  <div class="workflow-node" :class="nodeClass">
    <div class="node-header">
      <span class="node-icon">{{ nodeIcon }}</span>
      <span class="node-label">{{ nodeLabel }}</span>
    </div>
    <div class="node-content" v-if="showContent">
      <div class="node-preview">{{ previewText }}</div>
    </div>
    <div class="node-handles">
      <!-- 条件节点有多个输出Handle -->
      <template v-if="node.type === 'CONDITION'">
        <div class="handle handle-true" data-handleid="true">
          <span>True</span>
        </div>
        <div class="handle handle-false" data-handleid="false">
          <span>False</span>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  node: {
    type: Object,
    required: true
  }
});

const node = props.node;

// 节点类型配置
const nodeTypeConfig = {
  START: { icon: '●', color: '#67C23A', label: '起点' },
  END: { icon: '●', color: '#E6A23C', label: '终点' },
  HELLO: { icon: '👋', color: '#909399', label: '问候' },
  HTTP: { icon: '🌐', color: '#409EFF', label: 'HTTP' },
  CONDITION: { icon: '?', color: '#E6A23C', label: '条件' },
  LLM: { icon: '🤖', color: '#9C27B0', label: 'LLM' },
  EMPTY_NODE: { icon: '□', color: '#909399', label: '空白' }
};

const config = computed(() => nodeTypeConfig[node.type] || nodeTypeConfig.EMPTY_NODE);

const nodeIcon = computed(() => config.value.icon);
const nodeLabel = computed(() => node.data?.customLabel || node.data?.label || config.value.label);

const nodeClass = computed(() => [
  'node-type-' + node.type.toLowerCase(),
  { 'is-selected': node.selected }
]);

const showContent = computed(() => {
  return ['HTTP', 'LLM', 'CONDITION'].includes(node.type);
});

const previewText = computed(() => {
  const data = node.data?.config || {};
  if (node.type === 'HTTP') {
    return data.Url || data.Method || 'HTTP请求';
  }
  if (node.type === 'LLM') {
    return data.Model || data.ProviderName || 'LLM';
  }
  if (node.type === 'CONDITION') {
    return '条件判断';
  }
  return '';
});
</script>

<style scoped>
.workflow-node {
  padding: 10px 15px;
  border-radius: 8px;
  border: 2px solid #ddd;
  background: #fff;
  min-width: 120px;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
}

.workflow-node:hover {
  border-color: #409EFF;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.2);
}

.workflow-node.is-selected {
  border-color: #409EFF;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.3);
}

/* 起点节点 */
.node-type-start {
  border-radius: 50%;
  width: 80px;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #67C23A;
  color: #fff;
}

/* 终点节点 */
.node-type-end {
  border-radius: 50%;
  width: 80px;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #E6A23C;
  color: #fff;
}

/* 普通节点 */
.node-type-hello,
.node-type-http,
.node-type-condition,
.node-type-llm {
  border-radius: 8px;
}

.node-type-start .node-header,
.node-type-end .node-header {
  flex-direction: column;
}

.node-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
}

.node-icon {
  font-size: 16px;
}

.node-content {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid #eee;
  font-size: 12px;
  color: #666;
}

.node-preview {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.node-handles {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  right: -10px;
  display: flex;
  gap: 5px;
}

.handle {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #409EFF;
  border: 2px solid #fff;
  cursor: crosshair;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  color: #fff;
}
</style>
