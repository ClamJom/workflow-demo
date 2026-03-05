<template>
  <div class="start-node" :class="{ 'is-selected': data.selected }">
    <!-- 输出连接点 -->
    <Handle type="source" :position="Position.Right" />
    
    <div class="node-content">
      <span class="node-label">{{ label }}</span>
    </div>
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

const label = computed(() => props.data?.customLabel || props.data?.label || '起点')
</script>

<style scoped>
.start-node {
  padding: 10px 15px;
  border-radius: 8px;
  background: #67C23A;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
  border: 2px solid #67C23A;
  min-width: 80px;
  position: relative;
}

.start-node:hover {
  box-shadow: 0 0 0 2px rgba(103, 194, 58, 0.3);
}

.start-node.is-selected {
  border-color: #409EFF;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.3);
}

.node-content {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
}

.node-icon {
  font-size: 16px;
}

.node-label {
  font-size: 14px;
  font-weight: 500;
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
