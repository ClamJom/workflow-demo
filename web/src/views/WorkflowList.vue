<template>
  <div class="workflow-list">
    <div class="header">
      <h2>工作流管理</h2>
      <div class="actions">
        <el-button type="primary" @click="createWorkflow">新建工作流</el-button>
        <el-button @click="goToLLMProviders">LLM供应商管理</el-button>
      </div>
    </div>
    
    <el-card v-loading="loading">
      <el-table :data="workflows" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="createTime" label="创建时间" width="200">
          <template #default="scope">
            {{ formatDate(scope.row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="scope">
            <el-button type="primary" size="small" @click="editWorkflow(scope.row.uuid)">编辑</el-button>
            <el-button type="danger" size="small" @click="deleteWorkflow(scope.row.uuid, scope.row.name)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <div v-if="!loading && workflows.length === 0" class="empty-tip">
        暂无工作流，点击"新建工作流"创建
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { listWorkflows, deleteWorkflowFile } from '../api';

const router = useRouter();

// 状态
const workflows = ref([]);
const loading = ref(false);

// 生命周期
onMounted(() => {
  fetchWorkflows();
});

// 获取工作流列表
async function fetchWorkflows() {
  loading.value = true;
  try {
    const result = await listWorkflows();
    if (result.success) {
      workflows.value = result.data || [];
    } else {
      ElMessage.error(result.msg || '获取工作流列表失败');
    }
  } catch (e) {
    ElMessage.error('获取工作流列表失败: ' + e.message);
  } finally {
    loading.value = false;
  }
}

// 创建工作流
function createWorkflow() {
  router.push('/workflow/new');
}

// 编辑工作流
function editWorkflow(uuid) {
  router.push(`/workflow/${uuid}/edit`);
}

// 删除工作流
async function deleteWorkflow(uuid, name) {
  try {
    await ElMessageBox.confirm(`确定要删除工作流"${name}"吗?`, '提示', {
      type: 'warning'
    });
    
    const result = await deleteWorkflowFile(uuid);
    if (result.success) {
      ElMessage.success('删除成功');
      await fetchWorkflows();
    } else {
      ElMessage.error(result.msg || '删除失败');
    }
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败: ' + e.message);
    }
  }
}

// 跳转到LLM供应商管理
function goToLLMProviders() {
  router.push('/llm-providers');
}

// 格式化日期
function formatDate(date) {
  if (!date) return '-';
  try {
    return new Date(date).toLocaleString('zh-CN');
  } catch {
    return String(date);
  }
}
</script>

<style scoped>
.workflow-list {
  padding: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header h2 {
  margin: 0;
}

.actions {
  display: flex;
  gap: 10px;
}

.empty-tip {
  text-align: center;
  color: #999;
  padding: 40px 0;
  font-size: 14px;
}
</style>
