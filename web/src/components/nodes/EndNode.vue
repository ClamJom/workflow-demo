<template>
  <div class="end-node" :class="{ 'is-selected': data.selected }">
    <!-- 输入连接点 -->
    <Handle type="target" :position="Position.Left" />
    
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

const label = computed(() => props.data?.customLabel || props.data?.label || '终点')
</script>

<style scoped>
.end-node {
  padding: 10px 15px;
  border-radius: 8px;
  background: #E6A23C;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
  border: 2px solid #E6A23C;
  min-width: 80px;
  position: relative;
}

.end-node:hover {
  box-shadow: 0 0 0 2px rgba(230, 162, 60, 0.3);
}

.end-node.is-selected {
  border-color: #409EFF;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.3);
}

.node-content {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
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
