<script setup>
import { ref, computed, onMounted } from 'vue';
import { Form, Slider, Row, Col, Tooltip, InputNumber } from 'ant-design-vue';

const props = defineProps({
  config: {
    type: Object,
    required: true
  },
  modelValue: {
    type: String,
    default: ''
  }
});

const emit = defineEmits(['update:modelValue']);

// 内部维护的显示值
const displayValue = ref(0);

// 计算 min 和 max（默认为 0-100）
const min = computed(() => props.config.min !== undefined ? props.config.min : 0);
const max = computed(() => props.config.max !== undefined ? props.config.max : 100);

// 计算实际值（考虑 k 和 quantize）
const actualValue = computed(() => {
  const k = props.config.k || 1;
  const quantize = props.config.quantize || 0;
  
  if (k === 1) {
    return parseInt(displayValue.value) || 0;
  }
  const v = parseFloat(displayValue.value) / k;
  return quantize > 0 ? parseFloat(v.toFixed(quantize)) : parseInt(v);
});

// 初始化 displayValue
function initDisplayValue() {
  try {
    const v = parseFloat(props.modelValue) || 0;
    const k = props.config.k || 1;
    // 如果 k > 1，需要将存储的值转换为显示值
    displayValue.value = k > 1 ? v * k : v;
  } catch {
    displayValue.value = 0;
  }
}

// 组件挂载时初始化
onMounted(() => {
  initDisplayValue();
});

// 值变化时同步到 modelValue
function onChange(value) {
  emit('update:modelValue', JSON.stringify(actualValue.value));
}
</script>

<template>
  <div class="slider-config">
    <Form layout="vertical">
      <Row :gutter="8">
        <Col :span="24">
          <Form.Item :label="config.des || config.name">
            <Tooltip :title="`范围: ${min} - ${max}`">
              <Slider
                v-model:value="displayValue"
                :min="min"
                :max="max"
                @change="onChange"
              />
            </Tooltip>
          </Form.Item>
        </Col>
      </Row>
      <Row :gutter="8" v-if="config.k && config.k > 1">
        <Col :span="8">
          <span class="info-text">除数 (k): {{ config.k }}</span>
        </Col>
        <Col :span="8">
          <span class="info-text">精度: {{ config.quantize || 0 }} 位小数</span>
        </Col>
        <Col :span="8">
          <span class="info-text">实际值: {{ actualValue }}</span>
        </Col>
      </Row>
    </Form>
  </div>
</template>

<style scoped>
.slider-config {
  width: 100%;
  overflow: hidden;
}

.info-text {
  font-size: 12px;
  color: #999;
}

:deep(.ant-form-item) {
  margin-bottom: 0;
}

:deep(.ant-form-item-label) {
  padding-bottom: 4px;
}

:deep(.ant-form-item-label > label) {
  font-size: 12px;
  color: #999;
}
</style>
