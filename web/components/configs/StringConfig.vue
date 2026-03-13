<script setup>
import { ref, computed, onMounted } from 'vue';
import { Form, Textarea, AutoComplete, Tooltip } from 'ant-design-vue';

const props = defineProps({
  config: {
    type: Object,
    required: true
  },
  modelValue: {
    type: String,
    default: ''
  },
  pool: {
    type: Array,
    default: () => []
  }
});

const emit = defineEmits(['update:modelValue']);

// 内部维护的值
const inputValue = ref('');

const cacheBefore = ref('');
const cacheAfter = ref('');

// 搜索过滤后的选项
const filteredOptions = computed(() => {
  if (!props.pool || props.pool.length === 0) return [];
  return props.pool.map(item => ({
    value: `{{${item.name}}}`,
    name: item.name,
    desc: item.des || item.type || ''
  }));
});

// 是否显示下拉框
const visible = ref(false);

// 初始化值
function initValue() {
  inputValue.value = props.modelValue || '';
}

// 组件挂载时初始化
onMounted(() => {
  initValue();
});

// 输入变化时处理
function onInputChange(e) {
  // 检查是否需要显示下拉框
  if(e.data !== "/" || filteredOptions.value.length === 0) return;
  // 当输入包含 / 字符时显示选项
  visible.value = true;
  const input = document.querySelector('.string-config textarea');
  if (!input) return;
  // 无论如何，只要输入了 / 字符，就先将前后部分缓存，方便后续组装
  const cursorPos = input.selectionStart;
  const currentValue = inputValue.value;
  cacheBefore.value = currentValue.substring(0, cursorPos - 1);
  cacheAfter.value = currentValue.substring(cursorPos);
}

// 选择选项时处理
function onSelect(value) {
  inputValue.value = cacheBefore.value + value + cacheAfter.value;
  // 清理缓存并关闭下拉选项
  cacheBefore.value = "";
  cacheAfter.value = "";
  visible.value = false;

  emit('update:modelValue', inputValue.value);
}
</script>

<template>
  <div class="string-config">
    <Form layout="vertical">
      <Form.Item :label="config.des || config.name">
        <Tooltip :title="config.des">
          <AutoComplete
            v-model:value="inputValue"
            :options="filteredOptions"
            :open="visible"
            placeholder="输入 / 选择变量或直接输入内容"
            style="width: 100%"
            @input="onInputChange"
            @select="onSelect"
          >
            <Textarea :rows="1" />
          </AutoComplete>
        </Tooltip>
      </Form.Item>
    </Form>
  </div>
</template>

<style scoped>
.string-config {
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
