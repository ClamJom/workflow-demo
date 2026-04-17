<script setup>
import { ref, computed, watch, nextTick } from 'vue';
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
  },
  /** 刷新上游变量池（输入 `/` 前调用，与当前画布边一致） */
  requestPoolRefresh: {
    type: Function,
    default: null
  }
});

const emit = defineEmits(['update:modelValue']);

const inputValue = ref('');
const textareaRef = ref(null);

const cacheBefore = ref('');
const cacheAfter = ref('');

const filteredOptions = computed(() => {
  if (!props.pool || props.pool.length === 0) return [];
  return props.pool.map(item => ({
    value: `{{${item.name}}}`,
    name: item.name,
    desc: item.des || item.type || ''
  }));
});

const visible = ref(false);

function getTextareaEl() {
  const root = textareaRef.value;
  if (!root) return null;
  return root.resizableTextArea?.textArea || root.$el?.querySelector?.('textarea') || null;
}

watch(() => props.modelValue, (v) => {
  const s = v ?? '';
  if (s !== inputValue.value) {
    inputValue.value = s;
  }
}, { immediate: true });

watch(inputValue, (val) => {
  emit('update:modelValue', val);
});

async function onTextareaKeydown(e) {
  if (e.key !== '/') return;
  if (props.requestPoolRefresh) {
    try {
      await props.requestPoolRefresh();
    } catch {
      /* ignore */
    }
  }
  await nextTick();
  if (filteredOptions.value.length === 0) return;
  // 需在下一帧读 DOM：此时 `/` 已写入 textarea，且 AutoComplete 默认会按输入过滤选项，必须关闭过滤才能显示变量列表
  nextTick(() => {
    const ta = getTextareaEl();
    if (!ta) return;
    const val = ta.value;
    const pos = ta.selectionStart;
    if (pos >= 1 && val.charAt(pos - 1) === '/') {
      cacheBefore.value = val.slice(0, pos - 1);
      cacheAfter.value = val.slice(pos);
      visible.value = true;
    }
  });
}

function onDropdownVisibleChange(open) {
  if (!open) {
    visible.value = false;
  }
}

function onSelect(value) {
  inputValue.value = cacheBefore.value + value + cacheAfter.value;
  cacheBefore.value = '';
  cacheAfter.value = '';
  visible.value = false;
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
            :filter-option="false"
            placeholder="输入 // 选择变量或直接输入内容"
            style="width: 100%"
            @select="onSelect"
            @dropdown-visible-change="onDropdownVisibleChange"
          >
            <Textarea
              ref="textareaRef"
              :rows="3"
              @keydown="onTextareaKeydown"
            />
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
