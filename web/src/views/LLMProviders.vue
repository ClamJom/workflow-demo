<template>
  <div class="llm-providers">
    <div class="header">
      <h2>LLM供应商管理</h2>
      <div class="actions">
        <el-button type="primary" @click="initDefaultProviders">初始化默认供应商</el-button>
        <el-button type="success" @click="showAddDialog">新增供应商</el-button>
        <el-button @click="goBack">返回</el-button>
      </div>
    </div>
    
    <el-card>
      <el-table :data="providers" style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="name" label="名称" width="150" />
        <el-table-column prop="type" label="类型" width="120" />
        <el-table-column prop="baseUrl" label="API地址" min-width="200" show-overflow-tooltip />
        <el-table-column prop="model" label="默认模型" width="150" />
        <el-table-column prop="description" label="描述" min-width="150" show-overflow-tooltip />
        <el-table-column label="默认" width="80">
          <template #default="scope">
            <el-tag v-if="scope.row.isDefault" type="success">默认</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="scope">
            <el-tag :type="scope.row.status === 'active' ? 'success' : 'info'">
              {{ scope.row.status === 'active' ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250">
          <template #default="scope">
            <el-button type="primary" size="small" @click="editProvider(scope.row)">编辑</el-button>
            <el-button v-if="!scope.row.isDefault" type="success" size="small" @click="setDefault(scope.row.id)">设为默认</el-button>
            <el-button type="danger" size="small" @click="deleteProviderHandler(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    
    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑供应商' : '新增供应商'" width="600px">
      <el-form :model="form" label-width="120px">
        <el-form-item label="供应商名称">
          <el-input v-model="form.name" placeholder="如: OpenAI" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.type" placeholder="选择类型">
            <el-option label="OpenAI" value="openai" />
            <el-option label="Azure OpenAI" value="azure" />
            <el-option label="Anthropic" value="anthropic" />
            <el-option label="Google" value="google" />
            <el-option label="本地模型" value="local" />
            <el-option label="自定义" value="custom" />
          </el-select>
        </el-form-item>
        <el-form-item label="API地址">
          <el-input v-model="form.baseUrl" placeholder="如: https://api.openai.com/v1" />
        </el-form-item>
        <el-form-item label="API密钥">
          <el-input v-model="form.apiKey" type="password" placeholder="请输入API密钥" show-password />
        </el-form-item>
        <el-form-item label="默认模型">
          <el-input v-model="form.model" placeholder="如: gpt-3.5-turbo" />
        </el-form-item>
        <el-form-item label="超时时间(ms)">
          <el-input-number v-model="form.timeout" :min="5000" :max="300000" :step="5000" />
        </el-form-item>
        <el-form-item label="最大Token">
          <el-input-number v-model="form.maxTokens" :min="100" :max="128000" :step="100" />
        </el-form-item>
        <el-form-item label="温度参数">
          <el-slider v-model="form.temperature" :min="0" :max="2" :step="0.1" show-stops />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio label="active">启用</el-radio>
            <el-radio label="inactive">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="设为默认">
          <el-switch v-model="form.isDefault" />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { 
  getLLMProviders, 
  createProvider, 
  updateProvider, 
  deleteProvider, 
  setDefaultProvider,
  initDefaultProviders as initProvidersAPI
} from '../api';

const router = useRouter();

// 状态
const loading = ref(false);
const submitting = ref(false);
const providers = ref([]);
const dialogVisible = ref(false);
const isEdit = ref(false);

// 表单数据
const defaultForm = {
  name: '',
  type: 'openai',
  baseUrl: 'https://api.openai.com/v1',
  apiKey: '',
  model: 'gpt-3.5-turbo',
  timeout: 60000,
  maxTokens: 4096,
  temperature: 0.7,
  description: '',
  status: 'active',
  isDefault: false
};

const form = ref({ ...defaultForm });
const editingId = ref(null);

// 生命周期
onMounted(() => {
  fetchProviders();
});

// 获取供应商列表
async function fetchProviders() {
  loading.value = true;
  try {
    providers.value = await getLLMProviders();
  } catch (e) {
    ElMessage.error('获取供应商列表失败: ' + e.message);
  } finally {
    loading.value = false;
  }
}

// 初始化默认供应商
async function initDefaultProviders() {
  try {
    await ElMessageBox.confirm('这将创建默认的LLM供应商配置，是否继续?', '提示', {
      type: 'info'
    });
    
    await initProvidersAPI();
    ElMessage.success('初始化成功');
    fetchProviders();
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('初始化失败: ' + e.message);
    }
  }
}

// 显示新增对话框
function showAddDialog() {
  isEdit.value = false;
  form.value = { ...defaultForm };
  editingId.value = null;
  dialogVisible.value = true;
}

// 编辑供应商
function editProvider(provider) {
  isEdit.value = true;
  form.value = {
    name: provider.name,
    type: provider.type,
    baseUrl: provider.baseUrl,
    apiKey: '', // 不返回密钥
    model: provider.model,
    timeout: provider.timeout,
    maxTokens: provider.maxTokens,
    temperature: provider.temperature,
    description: provider.description,
    status: provider.status,
    isDefault: provider.isDefault
  };
  editingId.value = provider.id;
  dialogVisible.value = true;
}

// 提交表单
async function submitForm() {
  if (!form.value.name) {
    ElMessage.warning('请输入供应商名称');
    return;
  }
  
  submitting.value = true;
  try {
    if (isEdit.value) {
      await updateProvider(editingId.value, form.value);
      ElMessage.success('更新成功');
    } else {
      await createProvider(form.value);
      ElMessage.success('创建成功');
    }
    
    dialogVisible.value = false;
    fetchProviders();
  } catch (e) {
    ElMessage.error((isEdit.value ? '更新' : '创建') + '失败: ' + e.message);
  } finally {
    submitting.value = false;
  }
}

// 设为默认
async function setDefault(id) {
  try {
    await setDefaultProvider(id);
    ElMessage.success('设置成功');
    fetchProviders();
  } catch (e) {
    ElMessage.error('设置失败: ' + e.message);
  }
}

// 删除供应商
async function deleteProviderHandler(id) {
  try {
    await ElMessageBox.confirm('确定要删除这个供应商吗?', '提示', {
      type: 'warning'
    });
    
    await deleteProvider(id);
    ElMessage.success('删除成功');
    fetchProviders();
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败: ' + e.message);
    }
  }
}

// 返回
function goBack() {
  router.push('/workflows');
}
</script>

<style scoped>
.llm-providers {
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
</style>
