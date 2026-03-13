<script setup>
import { ref, computed, onMounted } from 'vue';
import { Form, AutoComplete, Textarea, Button, Row, Col, Empty, FormItem, Tooltip } from 'ant-design-vue';
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
  }
});

const emit = defineEmits(['update:modelValue']);

// 内部维护的键值对列表（包含空键的条目）
const entries = ref([]);

// 搜索过滤后的选项
const filteredOptions = computed(() => {
  if (!props.pool || props.pool.length === 0) return [];
  return props.pool.map(item => ({
    value: `{{${item.name}}}`,
    name: item.name,
    desc: item.des || item.type || ''
  }));
});

// 初始化 entries
function initEntries() {
  try {
    const obj = JSON.parse(props.modelValue || '{}');
    entries.value = Object.entries(obj).map(([key, value], index) => ({
      key,
      value,
      id: index,
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

// 组件挂载时初始化
onMounted(() => {
  initEntries();
});

// 将有效键值对转换为 JSON 字符串并 emit
function syncToModelValue() {
  const obj = {};
  entries.value.forEach(item => {
    if (item.key && item.key.trim()) {
      obj[item.key] = item.value;
    }
  });
  emit('update:modelValue', JSON.stringify(obj));
}

// 添加新的键值对
function addEntry() {
  entries.value.push({ 
    key: '', 
    value: '', 
    id: Date.now(),
    keyCacheBefore: '',
    keyCacheAfter: '',
    valueCacheBefore: '',
    valueCacheAfter: '',
    keyVisible: false,
    valueVisible: false
  });
}

// 删除键值对
function removeEntry(id) {
  const index = entries.value.findIndex(item => item.id === id);
  if (index > -1) {
    entries.value.splice(index, 1);
    syncToModelValue();
  }
}

// 键输入变化时处理
function onKeyInputChange(e, entry) {
  if (e.data !== "/" || filteredOptions.value.length === 0) return;
  entry.keyVisible = true;
  const input = document.querySelector(`.map-config .map-entry-${entry.id} .key-input textarea`);
  if (!input) return;
  const cursorPos = input.selectionStart;
  entry.keyCacheBefore = entry.key.substring(0, cursorPos - 1);
  entry.keyCacheAfter = entry.key.substring(cursorPos);
}

// 键选择选项时处理
function onKeySelect(value, entry) {
  entry.key = entry.keyCacheBefore + value + entry.keyCacheAfter;
  entry.keyCacheBefore = "";
  entry.keyCacheAfter = "";
  entry.keyVisible = false;
  syncToModelValue();
}

// 值输入变化时处理
function onValueInputChange(e, entry) {
  if (e.data !== "/" || filteredOptions.value.length === 0) return;
  entry.valueVisible = true;
  const input = document.querySelector(`.map-config .map-entry-${entry.id} .value-input textarea`);
  if (!input) return;
  const cursorPos = input.selectionStart;
  entry.valueCacheBefore = entry.value.substring(0, cursorPos - 1);
  entry.valueCacheAfter = entry.value.substring(cursorPos);
}

// 值选择选项时处理
function onValueSelect(value, entry) {
  entry.value = entry.valueCacheBefore + value + entry.valueCacheAfter;
  entry.valueCacheBefore = "";
  entry.valueCacheAfter = "";
  entry.valueVisible = false;
  syncToModelValue();
}

// 键直接输入变化
function onKeyChange(e, entry) {
  entry.key = e.target.value;
  syncToModelValue();
}

// 值直接输入变化
function onValueChange(e, entry) {
  entry.value = e.target.value;
  syncToModelValue();
}
</script>

<template>
  <div class="map-config">
    <Form layout="vertical">
      <div v-if="entries.length === 0" class="empty-tip">
        <Empty :description="'暂无数据，点击下方按钮添加'" :image="Empty.PRESENTED_IMAGE_SIMPLE" />
      </div>
      
      <div v-for="entry in entries" :key="entry.id" :class="['map-entry', 'map-entry-' + entry.id]">
        <Row :gutter="8" type="flex" align="middle">
          <Col :span="10">
            <FormItem label="键" :label-col="{ span: 24 }">
              <Tooltip title="输入 / 选择变量">
                <AutoComplete
                  v-model:value="entry.key"
                  class="key-input"
                  :options="filteredOptions"
                  :open="entry.keyVisible"
                  placeholder="请输入键名"
                  style="width: 100%"
                  @input="(e) => onKeyInputChange(e, entry)"
                  @select="(value) => onKeySelect(value, entry)"
                  @change="(e) => onKeyChange(e, entry)"
                >
                  <Textarea :rows="1" />
                </AutoComplete>
              </Tooltip>
            </FormItem>
          </Col>
          <Col :span="12">
            <FormItem label="值" :label-col="{ span: 24 }">
              <Tooltip title="输入 / 选择变量">
                <AutoComplete
                  v-model:value="entry.value"
                  class="value-input"
                  :options="filteredOptions"
                  :open="entry.valueVisible"
                  placeholder="请输入值"
                  style="width: 100%"
                  @input="(e) => onValueInputChange(e, entry)"
                  @select="(value) => onValueSelect(value, entry)"
                  @change="(e) => onValueChange(e, entry)"
                >
                  <Textarea :rows="1" />
                </AutoComplete>
              </Tooltip>
            </FormItem>
          </Col>
          <Col :span="2">
            <Button 
              type="text" 
              danger 
              @click="removeEntry(entry.id)"
              class="delete-btn"
            >
              <DeleteOutlined />
            </Button>
          </Col>
        </Row>
      </div>

      <FormItem>
        <Button type="dashed" block @click="addEntry">
          <PlusOutlined />
          添加
        </Button>
      </FormItem>
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
