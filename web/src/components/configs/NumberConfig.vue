<script setup>
const value = defineModel();
const props = defineProps({
  variablePool: {
    type: Object,
    required: false,
    default: null
  },
  min: {
    type: Number,
    required: false,
    default: 0
  },
  max: {
    type: Number,
    required: false,
    default: 100
  }
});
</script>

<template>
  <el-input v-model="value" :min="props.min" :max="props.max" :type=" props.variablePool ? 'text' : 'number'"/>
</template>