<script setup>
import { ref, computed, onMounted } from 'vue';
import { Form, Select, Textarea, AutoComplete, Tooltip, Row, Col, Button, Empty } from 'ant-design-vue';
import { PlusOutlined, DeleteOutlined } from '@ant-design/icons-vue';
import {generateUUID} from "../../utils/token.js";

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

// 运算符选项
const operatorOptions = [
  { value: '==', label: '等于 (==)' },
  { value: '!=', label: '不等于 (!=)' },
  { value: '<', label: '小于 (<)' },
  { value: '>', label: '大于 (>)' },
  { value: '<=', label: '小于等于 (<=)' },
  { value: '>=', label: '大于等于 (>=)' }
];

// 条件列表
const conditions = ref([]);

// 搜索过滤后的选项
const filteredOptions = computed(() => {
  if (!props.pool || props.pool.length === 0) return [];
  return props.pool.map(item => ({
    value: `{{${item.name}}}`,
    name: item.name,
    desc: item.des || item.type || ''
  }));
});

// 初始化值
function initValue() {
  try {
    const arr = JSON.parse(props.modelValue || '[]');
    conditions.value = arr.map((item, index) => ({
      id: index,
      a: item.a || '',
      operator: item.operator || '==',
      b: item.b || '',
      nextNodes: item.nextNodes || []
    }));
  } catch {
    conditions.value = [];
  }
}

// 组件挂载时初始化
onMounted(() => {
  initValue();
});

// 同步到 modelValue
function syncToModelValue() {
  const arr = conditions.value.map(item => ({
    a: item.a,
    operator: item.operator,
    b: item.b,
    nextNodes: item.nextNodes
  }));
  emit('update:modelValue', JSON.stringify(arr));
}

// 添加条件
function addCondition() {
  conditions.value.push({
    id: generateUUID(),
    a: '',
    operator: '==',
    b: '',
    nextNodes: [],
    cacheBefore: "",
    cacheAfter: "",
    leftVisible: false,
    rightVisible: false
  });
  syncToModelValue();
}

// 删除条件
function removeCondition(id) {
  const index = conditions.value.findIndex(item => item.id === id);
  if (index > -1) {
    conditions.value.splice(index, 1);
    syncToModelValue();
  }
}

// 左值输入变化时处理
function onLeftInputChange(e, condition) {
  if (e.data !== "/" || filteredOptions.value.length === 0) return;
  condition.leftVisible = true;
  const input = document.querySelector(`.condition-config .condition-${condition.id} .left-input textarea`);
  if (!input) return;
  const cursorPos = input.selectionStart;
  condition.cacheBefore = condition.a.substring(0, cursorPos - 1);
  condition.cacheAfter = condition.a.substring(cursorPos);
}

// 左值选择选项时处理
function onLeftSelect(value, condition) {
  condition.a = condition.cacheBefore + value + condition.cacheAfter;
  condition.cacheBefore = "";
  condition.cacheAfter = "";
  condition.leftVisible = false;
  syncToModelValue();
}

// 右值输入变化时处理
function onRightInputChange(e, condition) {
  if (e.data !== "/" || filteredOptions.value.length === 0) return;
  condition.rightVisible = true;
  const input = document.querySelector(`.condition-config .condition-${condition.id} .right-input textarea`);
  if (!input) return;
  const cursorPos = input.selectionStart;
  condition.cacheBeforeRight = condition.b.substring(0, cursorPos - 1);
  condition.cacheAfterRight = condition.b.substring(cursorPos);
}

// 右值选择选项时处理
function onRightSelect(value, condition) {
  condition.b = condition.cacheBeforeRight + value + condition.cacheAfterRight;
  condition.cacheBeforeRight = "";
  condition.cacheAfterRight = "";
  condition.rightVisible = false;
  syncToModelValue();
}

// 运算符变化时处理
function onOperatorChange(value, condition) {
  condition.operator = value;
  syncToModelValue();
}

// 左值直接输入变化
function onLeftValueChange(e, condition) {
  syncToModelValue();
}

// 右值直接输入变化
function onRightValueChange(e, condition) {
  syncToModelValue();
}
</script>

<template>
  <div class="condition-config">
    <div v-if="conditions.length === 0" class="empty-tip">
      <Empty :description="'暂无条件，点击下方按钮添加'" :image="Empty.PRESENTED_IMAGE_SIMPLE" />
    </div>
    
    <div v-for="condition in conditions" :key="condition.id" class="condition-item" :class="`condition-${condition.id}`">
      <Form layout="vertical">
        <Row :gutter="8" type="flex" align="middle">
          <Col :span="9">
            <Form.Item label="左值">
              <Tooltip title="输入 / 选择变量">
                <AutoComplete
                  v-model:value="condition.a"
                  class="left-input"
                  :options="filteredOptions"
                  :open="condition.leftVisible"
                  placeholder="输入 / 选择变量"
                  style="width: 100%"
                  @input="(e) => onLeftInputChange(e, condition)"
                  @select="(value) => onLeftSelect(value, condition)"
                  @change="(e) => onLeftValueChange(e, condition)"
                  @blur="()=>{condition.leftVisible = false}"
                >
                  <Textarea :rows="1" />
                </AutoComplete>
              </Tooltip>
            </Form.Item>
          </Col>
          <Col :span="4">
            <Form.Item label="运算符">
              <Select
                v-model:value="condition.operator"
                :options="operatorOptions"
                style="width: 100%"
                @change="(value) => onOperatorChange(value, condition)"
              />
            </Form.Item>
          </Col>
          <Col :span="9">
            <Form.Item label="右值">
              <Tooltip title="输入 / 选择变量">
                <AutoComplete
                  v-model:value="condition.b"
                  class="right-input"
                  :options="filteredOptions"
                  :open="condition.rightVisible"
                  placeholder="输入 / 选择变量"
                  style="width: 100%"
                  @input="(e) => onRightInputChange(e, condition)"
                  @select="(value) => onRightSelect(value, condition)"
                  @change="(e) => onRightValueChange(e, condition)"
                  @blur="()=>{condition.rightVisible = false}"
                >
                  <Textarea :rows="1" />
                </AutoComplete>
              </Tooltip>
            </Form.Item>
          </Col>
          <Col :span="2">
            <Button 
              type="text" 
              danger 
              @click="removeCondition(condition.id)"
              class="delete-btn"
            >
              <DeleteOutlined />
            </Button>
          </Col>
        </Row>
      </Form>
    </div>

    <Form layout="vertical">
      <Form.Item>
        <Button type="dashed" block @click="addCondition">
          <PlusOutlined />
          添加条件
        </Button>
      </Form.Item>
    </Form>
  </div>
</template>

<style scoped>
.condition-config {
  width: 100%;
}

.condition-item {
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
