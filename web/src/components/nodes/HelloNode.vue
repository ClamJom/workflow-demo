<template>
  <div class="hello-node" :class="{ 'is-selected': data.selected }">
    <!-- 输入连接点 -->
    <Handle type="target" :position="Position.Left" />
    
    <div class="node-header">
      <span class="node-icon">👋</span>
      <span class="node-label">{{ label }}</span>
    </div>
    <div class="node-content" v-if="previewText">
      <div class="node-preview">{{ previewText }}</div>
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

const label = computed(() => props.data?.customLabel || props.data?.label || '问候')
const previewText = computed(() => props.data?.config?.message || props.data?.config?.Message || '')
</script>

<style scoped>
.hello-node {
  padding: 10px 15px;
  border-radius: 8px;
  border: 2px solid #909399;
  background: #fff;
  min-width: 120px;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
}

.hello-node:hover {
  border-color: #409EFF;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.2);
}

.hello-node.is-selected {
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
