<script setup>
import {
    Layout,
    LayoutSider,
    LayoutHeader,
    LayoutContent,
    Button,
} from "ant-design-vue";
import {VueFlow} from "@vue-flow/core";
import {Controls} from "@vue-flow/controls";
import WorkflowTypes from "../types/workflow";

import '@vue-flow/core/dist/style.css'
import {onMounted, ref} from "vue";
import api from "../api";

const workflowApis = api.workflow;

const pageWidth = ref(100);
const pageHeight = ref(100);

const flowContainer = ref(null);
const flowBody = ref(null);

const workflows = ref([]);
const currentWorkflow = ref(null);

/**
 * 处理侧边栏的工作流名称点击事件
 * @param uuid  工作流的UUID
 */
function handleWorkflowClicked(uuid){
  console.log(uuid);
}

function handlerNewWorkflow(){
  const workflow = new WorkflowTypes.Workflow();
  workflowApis.saveWorkflow(workflow).then(res=>{
    getAllWorkflows();
  })
}

/**
 * 获取页面大小，备用
 */
function updatePageSize(){
  pageWidth.value = window.innerWidth;
  pageHeight.value = window.innerHeight;
}

/**
 * 获取所有工作流
 */
function getAllWorkflows(){
  workflowApis.getWorkflows().then(res=>{
    // 这里只获取工作流的信息，但不获取详情（包括节点信息等）
    workflows.value = res.data;
  })
}

/**
 * 如果是开发环境，将API挂载到window上，方便调试
 */
function hookApiToWindowIfDev(){
  if(import.meta.env.PROD) return;
  window.workflowApis = workflowApis;
}

/**
 * 调试
 */
function debug(){
  const workflow = new WorkflowTypes.Workflow();
  console.log(JSON.stringify(workflow))
}

onMounted(()=>{
  updatePageSize();
  hookApiToWindowIfDev();
  getAllWorkflows();
  debug();
});
</script>

<template>
  <Layout class="body">
    <LayoutSider>
      <!-- TODO: 工作流列表置于导航栏 -->
      <Button>新建工作流</Button>
      <ul>
        <li v-for="workflow in workflows" :key="workflow.id" @click="()=>{handleWorkflowClicked(workflow.uuid)}">
          {{ workflow.name }}
        </li>
      </ul>
    </LayoutSider>
    <Layout>
      <LayoutHeader>
        <!-- TODO: 放置工作流名称 -->
      </LayoutHeader>
      <LayoutContent class="flow-container" ref="flowContainer">
        <!-- TODO: 放置工作流主体 -->
        <div v-if="workflows.length === 0" class="no-workflow">
          <p>还没有工作流，您可以新建工作流</p>
        </div>
        <div v-else-if="currentWorkflow === null" class="no-selected-workflow">
          <p>您可以选择历史工作流或新建工作流</p>
        </div>
        <VueFlow ref="flowBody" v-else>
          <Controls />
        </VueFlow>
      </LayoutContent>
    </Layout>
  </Layout>
</template>

<style scoped>
.body{
  width: 100%;
  height: 100%;
}
</style>
