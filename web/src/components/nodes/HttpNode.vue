<template>
  <div class="http-node" :class="{ 'is-selected': data.selected }">
    <!-- 输入连接点 -->
    <Handle type="target" :position="Position.Left" />
    
    <div class="node-header">
      <span class="node-icon">🌐</span>
      <span class="node-label">{{ label }}</span>
    </div>
    <div class="node-content">
      <div class="node-preview">
        <span class="method-badge" :class="methodClass">{{ method }}</span>
        <span class="url-text">{{ urlText }}</span>
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

const label = computed(() => props.data?.customLabel || props.data?.label || 'HTTP请求')
const config = computed(() => props.data?.config || {})
const method = computed(() => config.value.Method || 'GET')
const url = computed(() => config.value.Url || '')

const methodClass = computed(() => {
  const m = method.value.toLowerCase()
  return {
    'get': m === 'get',
    'post': m === 'post',
    'put': m === 'put',
    'delete': m === 'delete'
  }
})

const urlText = computed(() => {
  const u = url.value
  if (!u) return '未配置URL'
  // 简化显示
  try {
    const urlObj = new URL(u)
    return urlObj.pathname
  } catch {
    return u.substring(0, 20) + (u.length > 20 ? '...' : '')
  }
})
</script>

<style scoped>
.http-node {
  padding: 10px 15px;
  border-radius: 8px;
  border: 2px solid #409EFF;
  background: #fff;
  min-width: 150px;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
}

.http-node:hover {
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.2);
}

.http-node.is-selected {
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
}

.node-preview {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #666;
}

.method-badge {
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 10px;
  font-weight: 600;
  color: #fff;
}

.method-badge.get {
  background: #67C23A;
}

.method-badge.post {
  background: #409EFF;
}

.method-badge.put {
  background: #E6A23C;
}

.method-badge.delete {
  background: #F56C6C;
}

.url-text {
  flex: 1;
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
