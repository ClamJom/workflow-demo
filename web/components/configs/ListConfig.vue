<script setup>
import { ref, onMounted } from 'vue';
import { Form, Input, Button, Row, Col, Empty, FormItem } from 'ant-design-vue';
import { PlusOutlined, DeleteOutlined, HolderOutlined } from '@ant-design/icons-vue';

const props = defineProps({
  config: {
    type: Object,
    required: true
  },
  modelValue: {
    type: String,
    default: '[]'
  }
});

const emit = defineEmits(['update:modelValue']);

// 内部维护的列表（包含空值的条目）
const items = ref([]);

// 初始化 items
function initItems() {
  try {
    const arr = JSON.parse(props.modelValue || '[]');
    items.value = arr.map((value, index) => ({
      value,
      id: index
    }));
  } catch {
    items.value = [];
  }
}

// 组件挂载时初始化
onMounted(() => {
  initItems();
});

// 将有效项转换为 JSON 字符串并 emit
function syncToModelValue() {
  const arr = [];
  items.value.forEach(item => {
    if (item.value && item.value.toString().trim()) {
      arr.push(item.value);
    }
  });
  emit('update:modelValue', JSON.stringify(arr));
}

// 添加新的列表项
function addItem() {
  items.value.push({ value: '', id: Date.now() });
}

// 删除列表项
function removeItem(id) {
  const index = items.value.findIndex(item => item.id === id);
  if (index > -1) {
    items.value.splice(index, 1);
    syncToModelValue();
  }
}

// 更新项时触发保存
function onItemChange(id, event) {
  const item = items.value.find(item => item.id === id);
  if (item) {
    item.value = event.target.value;
    syncToModelValue();
  }
}
</script>

<template>
  <div class="list-config">
    <Form layout="vertical">
      <div v-if="items.length === 0" class="empty-tip">
        <Empty :description="'暂无列表项，点击下方按钮添加'" :image="Empty.PRESENTED_IMAGE_SIMPLE" />
      </div>
      
      <div v-for="item in items" :key="item.id" class="list-item">
        <Row :gutter="8" type="flex" align="middle">
          <Col :span="20">
            <FormItem label="值" :label-col="{ span: 24 }">
              <Input 
                v-model:value="item.value" 
                placeholder="请输入值"
                @change="onItemChange(item.id, $event)"
              />
            </FormItem>
          </Col>
          <Col :span="4" class="delete-btn-container">
            <Button 
              type="text" 
              danger 
              @click="removeItem(item.id)"
              class="delete-btn"
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
.list-config {
  width: 100%;
}

.list-item {
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
