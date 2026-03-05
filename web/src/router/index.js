import { createRouter, createWebHistory } from 'vue-router';
import WorkflowEditor from '../views/WorkflowEditor.vue';
import WorkflowList from '../views/WorkflowList.vue';
import LLMProviders from '../views/LLMProviders.vue';

const routes = [
  {
    path: '/',
    redirect: '/workflows'
  },
  {
    path: '/workflows',
    name: 'WorkflowList',
    component: WorkflowList,
    meta: { title: '工作流列表' }
  },
  {
    path: '/workflow/new',
    name: 'NewWorkflow',
    component: WorkflowEditor,
    meta: { title: '新建工作流' }
  },
  {
    path: '/workflow/:id/edit',
    name: 'EditWorkflow',
    component: WorkflowEditor,
    meta: { title: '编辑工作流' }
  },
  {
    path: '/workflow/:id/run',
    name: 'RunWorkflow',
    component: WorkflowEditor,
    meta: { title: '运行工作流' }
  },
  {
    path: '/llm-providers',
    name: 'LLMProviders',
    component: LLMProviders,
    meta: { title: 'LLM供应商管理' }
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

// 路由守卫
router.beforeEach((to, from, next) => {
  if (to.meta.title) {
    document.title = `${to.meta.title} - Workflow Demo`;
  }
  next();
});

export default router;
