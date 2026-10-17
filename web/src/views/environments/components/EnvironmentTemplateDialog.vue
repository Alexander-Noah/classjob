<!--
  Web 管理平台的环境模板弹窗视图组件；模板、响应式状态与局部样式在本文件内协同维护。
  组件输入通过 defineProps 声明，父子交互以 props/emits 契约为准。
-->
<script setup>
import { computed } from 'vue'
import BusinessRecordDialog from '../../../components/business-management/BusinessRecordDialog.vue'

defineOptions({
  name: 'EnvironmentTemplateDialog',
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
    /**
     * 构造当前类型的默认状态。
     * @returns 生成的数组。
     */
    default: () => [],
  },
  formModel: {
    type: Object,
    required: true,
  },
  fieldOptions: {
    type: Object,
    /**
     * 构造当前类型的默认状态。
     * @returns 由当前表达式或分支生成的处理结果。
     */
    default: () => ({}),
  },
  formSubmitting: {
    type: Boolean,
    default: false,
  },
  statusOptions: {
    type: Array,
    /**
     * 构造当前类型的默认状态。
     * @returns 生成的数组。
     */
    default: () => [],
  },
  statusProp: {
    type: String,
    default: 'status',
  },
})

const emit = defineEmits(['submit', 'update:modelValue'])

const visible = computed({
  /**
   * 获取当前操作。
   * @returns props.modelValue 的当前值。
   */
  get: () => props.modelValue,
  /**
   * 设置当前操作。
   * 入参：value（值）。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  set: (value) => emit('update:modelValue', value),
})

/**
 * 处理Submit。
 */
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
