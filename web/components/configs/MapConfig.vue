<script setup>
import { ref, computed, onMounted, nextTick } from 'vue';
import { Form, AutoComplete, Textarea, Button, Row, Col, Empty, Tooltip } from 'ant-design-vue';
import { PlusOutlined, DeleteOutlined } from '@ant-design/icons-vue';

const props = defineProps({
  config: {
    type: Object,
    required: true
  },
  modelValue: {
    type: String,
    default: '{}'
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

/** @type {import('vue').Ref<Array<{
 *   id: string,
 *   key: string,
 *   value: string,
 *   keyCacheBefore: string,
 *   keyCacheAfter: string,
 *   valueCacheBefore: string,
 *   valueCacheAfter: string,
 *   keyVisible: boolean,
 *   valueVisible: boolean
 * }>>} */
const entries = ref([]);

/** 每行 Textarea 组件实例，用于定位光标与「/」补全 */
const keyTextareaRefs = new Map();
const valueTextareaRefs = new Map();

function bindKeyTaRef(entryId, el) {
  if (el) keyTextareaRefs.set(entryId, el);
  else keyTextareaRefs.delete(entryId);
}

function bindValueTaRef(entryId, el) {
  if (el) valueTextareaRefs.set(entryId, el);
  else valueTextareaRefs.delete(entryId);
}

function getTextareaEl(refMap, entryId) {
  const root = refMap.get(entryId);
  if (!root) return null;
  return root.resizableTextArea?.textArea || root.$el?.querySelector?.('textarea') || null;
}

const filteredOptions = computed(() => {
  if (!props.pool || props.pool.length === 0) return [];
  return props.pool.map(item => ({
    value: `{{${item.name}}}`,
    name: item.name,
    desc: item.des || item.type || ''
  }));
});

function normalizeValueForDisplay(v) {
  if (v == null) return '';
  if (typeof v === 'object') {
    try {
      return JSON.stringify(v);
    } catch {
      return String(v);
    }
  }
  return String(v);
}

function initEntries() {
  try {
    const obj = JSON.parse(props.modelValue || '{}');
    if (!obj || typeof obj !== 'object' || Array.isArray(obj)) {
      entries.value = [];
      return;
    }
    entries.value = Object.entries(obj).map(([key, value], index) => ({
      id: `map-row-${index}-${String(key)}`,
      key: key == null ? '' : String(key),
      value: normalizeValueForDisplay(value),
      keyCacheBefore: '',
      keyCacheAfter: '',
      valueCacheBefore: '',
      valueCacheAfter: '',
      keyVisible: false,
      valueVisible: false
    }));
  } catch {
    entries.value = [];
  }
}

/**
 * 从当前行构建对象（仅非空键），用于与 props 比较，避免无意义重渲染
 */
function buildObjectFromEntries() {
  const obj = {};
  entries.value.forEach((item) => {
    const k = typeof item.key === 'string' ? item.key.trim() : String(item.key ?? '').trim();
    if (!k) return;
    obj[k] = item.value;
  });
  return obj;
}

function syncToModelValue() {
  try {
    const obj = buildObjectFromEntries();
    emit('update:modelValue', JSON.stringify(obj));
  } catch (err) {
    console.warn('[MapConfig] syncToModelValue failed', err);
  }
}

function addEntry() {
  entries.value.push({
    key: '',
    value: '',
    id: `map-row-${Date.now()}-${Math.random().toString(36).slice(2, 9)}`,
    keyCacheBefore: '',
    keyCacheAfter: '',
    valueCacheBefore: '',
    valueCacheAfter: '',
    keyVisible: false,
    valueVisible: false
  });
}

function removeEntry(id) {
  const index = entries.value.findIndex(item => item.id === id);
  if (index > -1) {
    entries.value.splice(index, 1);
    syncToModelValue();
  }
}

/**
 * AutoComplete 的 @update:value 直接得到字符串，不要使用 @change 里假定 e.target
 */
function onKeyValueUpdate(val, entry) {
  entry.key = val == null ? '' : String(val);
  syncToModelValue();
}

function onValueUpdate(val, entry) {
  entry.value = val == null ? '' : String(val);
  syncToModelValue();
}

async function onKeySlashKeydown(e, entry) {
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
  nextTick(() => {
    const ta = getTextareaEl(keyTextareaRefs, entry.id);
    if (!ta) return;
    const pos = ta.selectionStart;
    const val = ta.value;
    if (pos >= 1 && val.charAt(pos - 1) === '/') {
      entry.keyCacheBefore = val.slice(0, pos - 1);
      entry.keyCacheAfter = val.slice(pos);
      entry.keyVisible = true;
    }
  });
}

async function onValueSlashKeydown(e, entry) {
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
  nextTick(() => {
    const ta = getTextareaEl(valueTextareaRefs, entry.id);
    if (!ta) return;
    const pos = ta.selectionStart;
    const val = ta.value;
    if (pos >= 1 && val.charAt(pos - 1) === '/') {
      entry.valueCacheBefore = val.slice(0, pos - 1);
      entry.valueCacheAfter = val.slice(pos);
      entry.valueVisible = true;
    }
  });
}

function onKeySelect(selectedVal, entry) {
  entry.key = entry.keyCacheBefore + selectedVal + entry.keyCacheAfter;
  entry.keyCacheBefore = '';
  entry.keyCacheAfter = '';
  entry.keyVisible = false;
  syncToModelValue();
}

function onValueSelect(selectedVal, entry) {
  entry.value = entry.valueCacheBefore + selectedVal + entry.valueCacheAfter;
  entry.valueCacheBefore = '';
  entry.valueCacheAfter = '';
  entry.valueVisible = false;
  syncToModelValue();
}

onMounted(() => {
  initEntries();
});
</script>

<template>
  <div class="map-config">
    <Form layout="vertical">
      <div v-if="entries.length === 0" class="empty-tip">
        <Empty :description="'暂无数据，点击下方按钮添加'" :image="Empty.PRESENTED_IMAGE_SIMPLE" />
      </div>

      <div v-for="entry in entries" :key="entry.id" class="map-entry">
        <Row :gutter="8" align="middle">
          <Col :span="10">
            <Form.Item label="键">
              <Tooltip title="输入 / 选择变量">
                <AutoComplete
                  :value="entry.key"
                  class="key-input"
                  :options="filteredOptions"
                  :open="entry.keyVisible"
                  :filter-option="false"
                  placeholder="请输入键名"
                  style="width: 100%"
                  @update:value="(val) => onKeyValueUpdate(val, entry)"
                  @select="(val) => onKeySelect(val, entry)"
                  @dropdown-visible-change="(open) => { if (!open) entry.keyVisible = false }"
                >
                  <Textarea
                    :ref="(el) => bindKeyTaRef(entry.id, el)"
                    :rows="1"
                    @keydown="(e) => onKeySlashKeydown(e, entry)"
                  />
                </AutoComplete>
              </Tooltip>
            </Form.Item>
          </Col>
          <Col :span="12">
            <Form.Item label="值">
              <Tooltip title="输入 / 选择变量">
                <AutoComplete
                  :value="entry.value"
                  class="value-input"
                  :options="filteredOptions"
                  :open="entry.valueVisible"
                  :filter-option="false"
                  placeholder="请输入值"
                  style="width: 100%"
                  @update:value="(val) => onValueUpdate(val, entry)"
                  @select="(val) => onValueSelect(val, entry)"
                  @dropdown-visible-change="(open) => { if (!open) entry.valueVisible = false }"
                >
                  <Textarea
                    :ref="(el) => bindValueTaRef(entry.id, el)"
                    :rows="1"
                    @keydown="(e) => onValueSlashKeydown(e, entry)"
                  />
                </AutoComplete>
              </Tooltip>
            </Form.Item>
          </Col>
          <Col :span="2">
            <Button
              type="text"
              danger
              class="delete-btn"
              @click="removeEntry(entry.id)"
            >
              <DeleteOutlined />
            </Button>
          </Col>
        </Row>
      </div>

      <Form.Item>
        <Button type="dashed" block @click="addEntry">
          <PlusOutlined />
          添加
        </Button>
      </Form.Item>
    </Form>
  </div>
</template>

<style scoped>
.map-config {
  width: 100%;
}

.map-entry {
  margin-bottom: 8px;
  padding: 8px;
  background: #fafafa;
  border-radius: 4px;
}

.empty-tip {
  padding: 16px 0;
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
