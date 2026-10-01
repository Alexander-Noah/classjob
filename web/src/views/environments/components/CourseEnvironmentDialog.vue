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
