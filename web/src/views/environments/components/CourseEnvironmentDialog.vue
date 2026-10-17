<!--
  Web 管理平台的课程环境弹窗视图组件；模板、响应式状态与局部样式在本文件内协同维护。
  组件输入通过 defineProps 声明，父子交互以 props/emits 契约为准。
-->
<script setup>
import { computed } from 'vue'
import BusinessRecordDialog from '../../../components/business-management/BusinessRecordDialog.vue'

defineOptions({
  name: 'CourseEnvironmentDialog',
})

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false,
  },
  dialogTitle: {
    type: String,
    required: true,
  },
  editableColumns: {
    type: Array,
    default: () => [],
  },
  formModel: {
    type: Object,
    required: true,
  },
  fieldOptions: {
    type: Object,
    default: () => ({}),
  },
  formSubmitting: {
    type: Boolean,
    default: false,
  },
  statusOptions: {
    type: Array,
    default: () => [],
  },
  statusProp: {
    type: String,
    default: 'status',
  },
})

const emit = defineEmits(['submit', 'update:modelValue'])

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
})

const handleSubmit = () => {
  emit('submit')
}
</script>

<template>
  <BusinessRecordDialog
    v-model="visible"
    :dialog-title="dialogTitle"
    :editable-columns="editableColumns"
    :field-options="fieldOptions"
    :form-model="formModel"
    :form-submitting="formSubmitting"
    :status-options="statusOptions"
    :status-prop="statusProp"
    @submit="handleSubmit"
  />
</template>
