<script setup>
import { ref, onMounted } from 'vue';
import { Form, Switch, Tooltip } from 'ant-design-vue';

const props = defineProps({
  config: {
    type: Object,
    required: true
  },
  modelValue: {
    type: String,
    default: 'false'
  }
});

const emit = defineEmits(['update:modelValue']);

// 内部维护的值
const switchValue = ref(false);

// 初始化值
function initValue() {
  switchValue.value = props.modelValue.toLowerCase() === 'true';
}

// 组件挂载时初始化
onMounted(() => {
  initValue();
});

// 值变化时处理
function onChange(checked) {
  switchValue.value = checked;
  emit('update:modelValue', checked ? 'true' : 'false');
}
</script>

<template>
  <div class="boolean-config">
    <Form layout="vertical">
      <Form.Item :label="config.des || config.name">
        <Tooltip :title="config.des">
          <Switch
            v-model:checked="switchValue"
            checked-children="On"
            un-checked-children="Off"
            @change="onChange"
          />
        </Tooltip>
      </Form.Item>
    </Form>
  </div>
</template>

<style scoped>
.boolean-config {
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
