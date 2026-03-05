<template>
  <div class="condition-node" :class="{ 'is-selected': data.selected }">
    <!-- 输入连接点 -->
    <Handle type="target" :position="Position.Left" />
    
    <div class="node-header">
      <span class="node-icon">?</span>
      <span class="node-label">{{ label }}</span>
    </div>
    <div class="node-content">
      <div v-if="conditions.length === 0" class="node-preview no-condition">
        未配置条件
      </div>
      <div v-else class="condition-list">
        <div 
          v-for="(cond, index) in conditions" 
          :key="cond._uuid || index"
          class="condition-item"
        >
          <span class="condition-index">{{ index + 1 }}</span>
          <span class="condition-text">{{ cond.left }} {{ cond.operator }} {{ cond.right }}</span>
        </div>
      </div>
    </div>
    
    <!-- 条件节点的动态输出连接点，每个条件对应一个 Handle -->
    <template v-if="conditions.length > 0">
      <Handle
        v-for="(cond, index) in conditions"
        :key="cond._uuid || index"
        type="source"
        :position="Position.Right"
        :id="cond._uuid || `condition-${index}`"
        :style="getHandleStyle(index, conditions.length)"
      />
    </template>
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

const label = computed(() => props.data?.customLabel || props.data?.label || '条件')
const config = computed(() => props.data?.config || {})

// 解析条件数组
const conditions = computed(() => {
  // 优先使用内存中的 _conditions 数组（已解析的格式）
  if (config.value._conditions && Array.isArray(config.value._conditions)) {
    return config.value._conditions
  }
  // 否则尝试解析 Condition 字符串
  try {
    const parsed = JSON.parse(config.value.Condition || '[]')
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
})

// 计算每个 Handle 的垂直位置
function getHandleStyle(index, total) {
  // if (total === 1) {
  //   return { top: '50%', transform: 'translateY(-50%)' }
  // }
  // // 均匀分布，留出上下边距
  // const spacing = 80 / (total - 1)
  // const topPercent = 10 + spacing * index
  // return { top: `${topPercent}%`, transform: 'translateY(-50%)' }
  var topPx = 10 + 24 + 9 + 12 + 24 * index + 5;
  return {top: `${topPx}px`}
}
</script>

<style scoped>
.condition-node {
  padding: 10px 15px;
  border-radius: 8px;
  border: 2px solid #E6A23C;
  background: #fff;
  min-width: 160px;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
  /* 右侧留出 Handle 的空间 */
  padding-right: 20px;
}

.condition-node:hover {
  border-color: #409EFF;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.2);
}

.condition-node.is-selected {
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
  font-weight: bold;
  color: #E6A23C;
}

.node-content {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid #eee;
  font-size: 12px;
  color: #666;
}

.no-condition {
  color: #999;
  font-style: italic;
}

.condition-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.condition-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 2px 4px;
  background: #f5f7fa;
  border-radius: 4px;
  font-size: 11px;
}

.condition-index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  background: #E6A23C;
  color: #fff;
  border-radius: 50%;
  font-size: 10px;
  font-weight: bold;
  flex-shrink: 0;
}

.condition-text {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 120px;
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
