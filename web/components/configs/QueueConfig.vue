<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue';
import { Form, Button, Row, Col, Empty, FormItem, AutoComplete, Textarea, Tooltip } from 'ant-design-vue';
import { PlusOutlined, DeleteOutlined } from '@ant-design/icons-vue';

const props = defineProps({
  config: {
    type: Object,
    required: true
  },
  modelValue: {
    type: String,
    default: '[]'
  },
  pool: {
    type: Array,
    default: () => []
  },
  requestPoolRefresh: {
    type: Function,
    default: null
  }
});

const emit = defineEmits(['update:modelValue']);

const items = ref([]);

const valueTaRefs = new Map();
const valueKeydownCleanups = new Map();

const filteredOptions = computed(() => {
  if (!props.pool || props.pool.length === 0) return [];
  return props.pool.map(item => ({
    value: `{{${item.name}}}`,
    name: item.name,
    desc: item.des || item.type || ''
  }));
});

function getTextareaEl(root) {
  if (!root) return null;
  return root.resizableTextArea?.textArea || root.$el?.querySelector?.('textarea') || null;
}

function bindValueTa(itemId, el) {
  const prev = valueKeydownCleanups.get(itemId);
  prev?.();
  valueKeydownCleanups.delete(itemId);
  valueTaRefs.set(itemId, el);
  if (!el) {
    valueTaRefs.delete(itemId);
    return;
  }
  const attach = (ta) => {
    const handler = (e) => onItemSlashKeydown(e, itemId);
    ta.addEventListener('keydown', handler);
    valueKeydownCleanups.set(itemId, () => {
      ta.removeEventListener('keydown', handler);
    });
  };
  nextTick(() => {
    let ta = getTextareaEl(el);
    if (!ta) {
      requestAnimationFrame(() => {
        ta = getTextareaEl(el);
        if (ta) attach(ta);
      });
      return;
    }
    attach(ta);
  });
}

function initItems() {
  try {
    const arr = JSON.parse(props.modelValue || '[]');
    if (!Array.isArray(arr)) {
      items.value = [];
      return;
    }
    items.value = arr.map((value, index) => ({
      value: value == null ? '' : String(value),
      id: `queue-item-${index}-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
      valueVisible: false,
      valueCacheBefore: '',
      valueCacheAfter: ''
    }));
  } catch {
    items.value = [];
  }
}

function syncToModelValue() {
  const arr = [];
  items.value.forEach((item) => {
    const s = String(item.value ?? '').trim();
    if (s) {
      arr.push(item.value);
    }
  });
  emit('update:modelValue', JSON.stringify(arr));
}

onMounted(() => {
  initItems();
});

onBeforeUnmount(() => {
  valueKeydownCleanups.forEach((fn) => fn());
  valueKeydownCleanups.clear();
  valueTaRefs.clear();
});

function addItem() {
  items.value.push({
    value: '',
    id: `queue-item-${Date.now()}-${Math.random().toString(36).slice(2, 9)}`,
    valueVisible: false,
    valueCacheBefore: '',
    valueCacheAfter: ''
  });
}

function removeItem(id) {
  const idx = items.value.findIndex(item => item.id === id);
  if (idx > -1) {
    const cleanup = valueKeydownCleanups.get(id);
    cleanup?.();
    valueKeydownCleanups.delete(id);
    valueTaRefs.delete(id);
    items.value.splice(idx, 1);
    syncToModelValue();
  }
}

function onValueUpdate(val, item) {
  item.value = val == null ? '' : String(val);
  syncToModelValue();
}

async function onItemSlashKeydown(e, itemId) {
  if (e.key !== '/') return;
  const item = items.value.find(i => i.id === itemId);
  if (!item) return;
  if (props.requestPoolRefresh) {
    try {
      await props.requestPoolRefresh();
    } catch {
      /* ignore */
    }
  }
  await nextTick();
  if (filteredOptions.value.length === 0) return;
  nextTick(() => {
    const ta = getTextareaEl(valueTaRefs.get(itemId));
    if (!ta) return;
    const val = ta.value;
    const pos = ta.selectionStart;
    if (pos >= 1 && val.charAt(pos - 1) === '/') {
      item.valueCacheBefore = val.slice(0, pos - 1);
      item.valueCacheAfter = val.slice(pos);
      item.valueVisible = true;
    }
  });
}

function onItemSelect(selectedVal, item) {
  item.value = item.valueCacheBefore + selectedVal + item.valueCacheAfter;
  item.valueCacheBefore = '';
  item.valueCacheAfter = '';
  item.valueVisible = false;
  syncToModelValue();
}
</script>

<template>
  <div class="queue-config">
    <Form layout="vertical">
      <div v-if="items.length === 0" class="empty-tip">
        <Empty :description="'暂无队列项，点击下方按钮添加'" :image="Empty.PRESENTED_IMAGE_SIMPLE" />
      </div>

      <div v-for="item in items" :key="item.id" class="queue-item">
        <Row :gutter="8" type="flex" align="middle">
          <Col :span="20">
            <FormItem label="值" :label-col="{ span: 24 }">
              <Tooltip title="输入 / 选择上游变量">
                <AutoComplete
                  :value="item.value"
                  class="queue-value-input"
                  :options="filteredOptions"
                  :open="item.valueVisible"
                  :filter-option="false"
                  placeholder="输入 / 选择变量"
                  style="width: 100%"
                  @update:value="(v) => onValueUpdate(v, item)"
                  @select="(v) => onItemSelect(v, item)"
                  @dropdown-visible-change="(open) => { if (!open) item.valueVisible = false }"
                >
                  <Textarea
                    :ref="(el) => bindValueTa(item.id, el)"
                    :rows="2"
                  />
                </AutoComplete>
              </Tooltip>
            </FormItem>
          </Col>
          <Col :span="4" class="delete-btn-container">
            <Button
              type="text"
              danger
              class="delete-btn"
              @click="removeItem(item.id)"
            >
              <DeleteOutlined />
            </Button>
          </Col>
        </Row>
      </div>

      <FormItem>
        <Button type="dashed" block @click="addItem">
          <PlusOutlined />
          添加
        </Button>
      </FormItem>
    </Form>
  </div>
</template>

<style scoped>
.queue-config {
  width: 100%;
}

.queue-item {
  margin-bottom: 8px;
  padding: 8px;
  background: #fafafa;
  border-radius: 4px;
}

.empty-tip {
  padding: 16px 0;
}

.delete-btn-container {
  display: flex;
  justify-content: center;
}

.delete-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
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
