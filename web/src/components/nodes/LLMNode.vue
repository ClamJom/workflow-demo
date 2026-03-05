<template>
  <div class="llm-node" :class="{ 'is-selected': data.selected }">
    <!-- 输入连接点 -->
    <Handle type="target" :position="Position.Left" />
    
    <div class="node-header">
      <span class="node-icon">🤖</span>
      <span class="node-label">{{ label }}</span>
    </div>
    <div class="node-content">
      <div class="node-preview">
        <span class="provider-badge">{{ provider }}</span>
        <span class="model-text">{{ model }}</span>
      </div>
      <div class="prompt-preview" v-if="promptText">
        {{ promptText }}
      </div>
    </div>
    <!-- 输出连接点 -->
    <Handle type="source" :position="Position.Right" />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { Handle, Position } from '@vue-flow/core'

const props = defineProps({
  id: {
    type: String,
    required: true
  },
  data: {
    type: Object,
    required: true
  }
})

const label = computed(() => props.data?.customLabel || props.data?.label || '大模型')
const config = computed(() => props.data?.config || {})
const provider = computed(() => config.value.ProviderName || '未选择')
const model = computed(() => config.value.Model || 'gpt-3.5-turbo')
const promptText = computed(() => {
  const prompt = config.value.UserPrompt || ''
  return prompt.length > 30 ? prompt.substring(0, 30) + '...' : prompt
})
</script>

<style scoped>
.llm-node {
  padding: 10px 15px;
  border-radius: 8px;
  border: 2px solid #9C27B0;
  background: #fff;
  min-width: 150px;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
}

.llm-node:hover {
  box-shadow: 0 2px 8px rgba(156, 39, 176, 0.2);
}

.llm-node.is-selected {
  border-color: #409EFF;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.3);
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

.node-label {
  color: #9C27B0;
}

.node-content {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid #eee;
  font-size: 12px;
}

.node-preview {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #666;
}

.provider-badge {
  padding: 2px 6px;
  border-radius: 3px;
  background: #9C27B0;
  color: #fff;
  font-size: 10px;
}

.model-text {
  font-weight: 500;
}

.prompt-preview {
  margin-top: 4px;
  color: #999;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* Vue Flow Handle 样式 */
:deep(.vue-flow__handle) {
  width: 10px;
  height: 10px;
  background: #409EFF;
  border: 2px solid #fff;
}

:deep(.vue-flow__handle-connected) {
  background: #67C23A;
}
</style>
