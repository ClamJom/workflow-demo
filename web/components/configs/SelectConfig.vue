<script setup>
import { ref, computed, onMounted } from 'vue';
import { Form, Select, Tooltip } from 'ant-design-vue';

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

// 内部维护的值
const selectedValue = ref('');

// 解析 options 字符串为数组
const options = computed(() => {
  if (!props.config.options) return [];
  try {
    return JSON.parse(props.config.options);
  } catch {
    return [];
  }
});

// 转换为 Select 组件需要的格式
const selectOptions = computed(() => {
  return options.value.map(opt => ({
    value: opt,
    label: opt
  }));
});

// 初始化值
function initValue() {
  selectedValue.value = props.modelValue || '';
}

// 组件挂载时初始化
onMounted(() => {
  initValue();
});

// 值变化时处理
function onChange(value) {
  selectedValue.value = value;
  emit('update:modelValue', value);
}
</script>

<template>
  <div class="select-config">
    <Form layout="vertical">
      <Form.Item :label="config.des || config.name">
        <Tooltip :title="config.des">
          <Select
            v-model:value="selectedValue"
            :options="selectOptions"
            placeholder="请选择"
            style="width: 100%"
            @change="onChange"
          />
        </Tooltip>
      </Form.Item>
    </Form>
  </div>
</template>

<style scoped>
.select-config {
  width: 100%;
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
